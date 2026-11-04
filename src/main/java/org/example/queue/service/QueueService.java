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

@Service
public class QueueService {
    private CycleQueue queue;

    //用 @Value注释 指向patients.txt（你生成的 80 条格式数据文件）
    @Value("classpath:static/patients.txt")
    private Resource resource;

    private List<String> lines;   // 存txt 文件全部行
    private int idx = 0;          // 记录当前读到第几行

    //  DateTimeFormatter 缓存，避免每次 toPatientVO 都 new 一个
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final String ID_PREFIX = "P";
    private static final int ID_DIGITS = 3;

    //从 txt 加载 80 个患者数据到内存，但先不直接入队
    public synchronized void loadFromFile() throws IOException {
        if(resource == null || !resource.exists()) {
            //出错处理
            throw new IOException("找不到患者数据文件 static/patients.txt");
        }
        //从资源中拿数据放到内存里
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            lines = br.lines()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        queue = new CycleQueue(lines.size());
        idx = 0;  //加载时重置计数器，回到第0行
    }

    //从 txt 取下一个患者入队（按优先级插入）
    public synchronized QueueStatusVO addOneFromFile() {
        if(lines == null || lines.isEmpty()) {
            return buildStatus();  // 未加载文件时返回空状态
        }
        if(idx >= lines.size()) {
            return buildStatus();  //防越界
        }
        Patient patient = parseLine(lines.get(idx));
        // 队列满时扩容
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
     把字符串变成patient对象
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

    //队首患者去就诊，出队
    public synchronized QueueStatusVO treatNext() {
        if(queue == null || queue.isEmpty()) return buildStatus();
        queue.deQueue();
        timeStep();
        return buildStatus();
    }

    // 患者放弃排队
    public synchronized QueueStatusVO giveUp(int patientId) {
        if(queue == null) return buildStatus();
        queue.remove(patientId);
        timeStep();
        return buildStatus();
    }

    // 重置队列
    public synchronized QueueStatusVO reset() {
        if(queue != null) queue.clear();
        idx = 0;
        return buildStatus();//清理参数
    }

    //获取队列当前状态
    public synchronized QueueStatusVO getStatus() {
        return buildStatus();
    }

    //获取平均等待时间
    public synchronized double getAvgWaitTime() {
        if(queue == null || queue.isEmpty()) return 0;
        timeStep();
        double sum = 0;
        int validCount = 0;//记录实际有几个，防止出问题
        int i = queue.getFront();
        while(i != queue.getRear()) {
            Patient p = queue.getPatients()[i];
            if(p != null) {
                sum += p.getWaitTime();
                validCount++;
            }
            i = (i + 1) % queue.getMaxLength();
        }
        return validCount > 0 ? sum / validCount : 0;
    }

    //给外面的api调用，看当前队列
    public synchronized List<Patient> getAllPatientsSnapshot() {
        if(queue == null) return List.of();
        timeStep();
        return queue.getAll();
    }
    //计算每一个患者的等待时间，用前面的处理时间之和
    private void timeStep() {
        if(queue.isEmpty()) return;
        int waitTime = 0;
        int id = queue.getFront();
        while(id != queue.getRear()) {
            Patient p = queue.getPatients()[id];
            if(p == null) {
                id = (id + 1) % queue.getMaxLength();
                continue;
            }
            p.setWaitTime(waitTime);
            waitTime += p.getTreatTime();
            id = (id + 1) % queue.getMaxLength();
        }
    }

    //构建 QueueStatusVO
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

    // 让int id → 变回txt里的P+数字格式
    public static String formatId(int numericId) {
        return ID_PREFIX + String.format("%0" + ID_DIGITS + "d", numericId);
    }
}
