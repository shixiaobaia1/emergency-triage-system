package org.example.queue.service;

import org.example.queue.algorithm.CycleQueue;
import org.example.queue.entity.Patient;
import org.example.queue.vo.PatientVO;
import org.example.queue.vo.QueueStatusVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 第一阶段核心服务：循环队列调度 + 患者等待时间计算
 * FIX: 从 EmergencyService 拆分出来，职责单一化
 * FIX: 线程安全——所有 public 方法加 synchronized（简单 demo 场景够用）
 */
@Service
public class QueueService {
    private CycleQueue queue;

    // FIX: @Value 指向 static/patients.txt（你生成的 80 条格式数据文件）
    @Value("classpath:static/patients.txt")
    private Resource resource;

    private List<String> lines;   // txt 文件全部行
    private int idx = 0;          // 当前读到第几行

    // FIX: DateTimeFormatter 缓存，避免每次 toPatientVO 都 new 一个
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    // FIX: Entity 的 int id → VO 的 String 编号（如 1 → "P001"）
    private static final String ID_PREFIX = "P";
    private static final int ID_DIGITS = 3;

    // FIX: 从 txt 加载 80 个患者数据到内存，但不直接入队（按需入队）
    public synchronized void loadFromFile() throws IOException {
        if(resource == null || !resource.exists()) {
            throw new IOException("找不到患者数据文件 static/patients.txt");
        }
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            lines = br.lines()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        queue = new CycleQueue(lines.size());
        idx = 0;  // FIX: 加载时重置计数器
    }

    // FIX: 从 txt 取下一个患者入队（按优先级插入）
    public synchronized QueueStatusVO addOneFromFile() {
        if(lines == null || lines.isEmpty()) {
            return buildStatus();  // FIX: 未加载文件时返回空状态
        }
        if(idx >= lines.size()) {
            return buildStatus();  // FIX: 全部入队完防越界
        }
        Patient patient = parseLine(lines.get(idx));
        // FIX: 队列满时自动扩容
        if(!queue.insertPriority(patient)){
            List<Patient> all = queue.getAll();
            queue = new CycleQueue(queue.getMaxLength() + all.size());
            for(Patient p : all) queue.insertPriority(p);
            queue.insertPriority(patient);
        }
        idx++;
        timeStep();
        return buildStatus();
    }

    /**
     * FIX: 把一行 txt 解析成 Patient Entity
     * txt 格式：P001 到达 08:00 时长 15min 优先级 3
     *  列索引:  s[0]    s[1] s[2]  s[3] s[4]  s[5]  s[6]    s[7]
     * FIX: s[0] 是 "P001" 这种 String 编号，需要剥掉 "P" 前缀转成 int 存 Entity
     */
    private Patient parseLine(String line) {
        String[] s = line.split("\\s+");
        // FIX: "P001" → 剥掉 'P' → "001" → parseInt → 1
        String rawId = s[0];
        int numericId = Integer.parseInt(rawId.replace(ID_PREFIX, ""));

        // "08:00" → LocalTime
        java.time.LocalTime arrival = java.time.LocalTime.parse(s[2]);
        // "15min" → 剥掉 "min" → parseInt → 15
        int treatMin = Integer.parseInt(s[4].replace("min", ""));
        int priority = Integer.parseInt(s[6]);

        return new Patient(numericId, null, arrival, treatMin, priority, 0);
    }

    // FIX: 队首患者就诊，出队
    public synchronized QueueStatusVO treatNext() {
        if(queue == null || queue.isEmpty()) return buildStatus();
        queue.deQueue();
        timeStep();
        return buildStatus();
    }

    // FIX: 患者放弃排队
    public synchronized QueueStatusVO giveUp(int patientId) {
        if(queue == null) return buildStatus();
        queue.remove(patientId);
        timeStep();
        return buildStatus();
    }

    // FIX: 重置队列——清空循环队列 + 重置文件读取计数器
    public synchronized QueueStatusVO reset() {
        if(queue != null) queue.clear();
        idx = 0;  // FIX: 必须同时重置 idx，否则后续再次 addOneFromFile 会越界
        return buildStatus();
    }

    // FIX: 获取队列当前状态（原 Controller getStatus 调错了 reset）
    public synchronized QueueStatusVO getStatus() {
        return buildStatus();
    }

    // FIX: 获取平均等待时间（直接返回值，不通过 VO）
    public synchronized double getAvgWaitTime() {
        if(queue == null || queue.isEmpty()) return 0;
        timeStep();
        double sum = 0;
        int i = queue.getFront();
        while(i != queue.getRear()) {
            sum += queue.getPatients()[i].getWaitTime();
            i = (i + 1) % queue.getMaxLength();
        }
        return sum / queue.getLength();
    }

    // FIX: 暴露队列快照给 HeapService 构建大根堆用（返回 Entity 列表，VO 在 HeapService 转）
    public synchronized List<Patient> getAllPatientsSnapshot() {
        if(queue == null) return List.of();
        timeStep();
        return queue.getAll();
    }

    // ===== 内部私有方法 =====

    // FIX: 时间步进——重新计算每个患者的等待时间
    //  队首 waitTime=0，后一个等前一个看完（treatTime 累加）
    private void timeStep() {
        if(queue.isEmpty()) return;
        int waitTime = 0;
        int id = queue.getFront();
        while(id != queue.getRear()) {
            queue.getPatients()[id].setWaitTime(waitTime);
            waitTime += queue.getPatients()[id].getTreatTime();  // FIX: treatTime 不是 proveTime
            id = (id + 1) % queue.getMaxLength();
        }
    }

    // FIX: 构建 QueueStatusVO（Entity → VO 转换在这里做，Controller 不碰 Entity）
    private QueueStatusVO buildStatus() {
        QueueStatusVO vo = new QueueStatusVO();
        if(queue == null) {
            vo.setFront(0);
            vo.setRear(0);
            vo.setQueueSize(0);
            vo.setCapacity(0);
            vo.setAvgWaitTime(0);
            vo.setPatientList(List.of());
            return vo;
        }
        vo.setFront(queue.getFront());
        vo.setRear(queue.getRear());
        vo.setQueueSize(queue.getLength());
        vo.setCapacity(queue.getMaxLength() - 1);
        vo.setAvgWaitTime(getAvgWaitTime());
        vo.setPatientList(queue.getAll().stream().map(this::toPatientVO).toList());
        return vo;
    }

    // FIX: Patient Entity → PatientVO（Entity int id → VO "P001" 格式 String）
    private PatientVO toPatientVO(Patient p) {
        PatientVO v = new PatientVO();
        v.setId(formatId(p.getId()));  // FIX: 1 → "P001"
        v.setName(p.getName());
        v.setArriveTime(p.getArriveTime().format(HH_MM));
        v.setTreatTime(p.getTreatTime());
        v.setPriority(p.getPriority());
        v.setWaitTime(p.getWaitTime());
        return v;
    }

    // FIX: Entity 的 int id → txt 文件中的 String 编号格式
    //  如 1 → "P001"，42 → "P042"，80 → "P080"
    public static String formatId(int numericId) {
        return ID_PREFIX + String.format("%0" + ID_DIGITS + "d", numericId);
    }
}
