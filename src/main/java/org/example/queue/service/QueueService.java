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
import java.util.ArrayList;
import java.util.List;

@Service
public class QueueService {

    // FIX: 8 个医生，每个医生有自己的循环队列
    private static final int DOCTOR_COUNT = 8;
    private final CycleQueue[] queues = new CycleQueue[DOCTOR_COUNT];

    // FIX: 所有已入队患者的总列表（用于全局 avgWaitTime 和 HeapService 排序）
    private final List<Patient> allPatients = new ArrayList<>();

    @Value("classpath:static/patients.txt")
    private Resource resource;

    private List<String> lines;
    private int idx = 0;

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final String ID_PREFIX = "P";
    private static final int ID_DIGITS = 3;

    // FIX: 构造时就初始化 8 个医生队列（每个容量先给大一点，不够会自动扩容）
    public QueueService() {
        for (int i = 0; i < DOCTOR_COUNT; i++) {
            queues[i] = new CycleQueue(100);
        }
    }

    /** 从 txt 加载患者数据到内存 */
    public synchronized void loadFromFile() throws IOException {
        if (resource == null || !resource.exists()) {
            throw new IOException("找不到患者数据文件 static/patients.txt");
        }
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            lines = br.lines()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        idx = 0;
    }

    /**
     * FIX: 从 txt 取下一个患者，调度到"当前队列最短"的医生
     * 返回所有医生队列状态的汇总
     */
    public synchronized List<QueueStatusVO> addOneFromFile() {
        if (lines == null || lines.isEmpty() || idx >= lines.size()) {
            return getAllDoctorStatus();
        }
        Patient patient = parseLine(lines.get(idx));
        // FIX: 调度策略——找当前人数最少的医生队列，实现负载均衡
        int targetDoctor = findShortestQueue();
        CycleQueue target = queues[targetDoctor];

        // 队列满了就扩容
        if (!target.insertPriority(patient)) {
            List<Patient> all = target.getAll();
            CycleQueue newQ = new CycleQueue(target.getMaxLength() + all.size() + 10);
            for (Patient p : all) newQ.insertPriority(p);
            newQ.insertPriority(patient);
            queues[targetDoctor] = newQ;
        } else {
            // 直接塞进去了，target 引用还是指向原对象（insertPriority 是 in-place 修改）
        }

        allPatients.add(patient);
        idx++;

        // FIX: 每个医生队列的 waitTime 都要重新算（单个医生内部累加）
        for (CycleQueue q : queues) timeStep(q);

        return getAllDoctorStatus();
    }

    /** 队首患者就诊结束，出队（指定医生编号） */
    public synchronized List<QueueStatusVO> treatNext(int doctorIndex) {
        if (doctorIndex < 0 || doctorIndex >= DOCTOR_COUNT) return getAllDoctorStatus();
        CycleQueue q = queues[doctorIndex];
        if (q != null && !q.isEmpty()) {
            Patient treated = q.deQueue();
            // 从总表里也去掉（可选，也可以留着当历史记录）
            if (treated != null) allPatients.removeIf(p -> p.getId() == treated.getId());
        }
        for (CycleQueue cq : queues) timeStep(cq);
        return getAllDoctorStatus();
    }

    /** 医生自己出队（默认医生 0） */
    public synchronized List<QueueStatusVO> treatNextDefault() {
        return treatNext(0);
    }

    /** 患者放弃排队（从某个医生队列里删掉） */
    public synchronized List<QueueStatusVO> giveUp(int patientId) {
        // FIX: 找到这个患者在哪个医生队列里，然后从那个队列 remove
        for (CycleQueue q : queues) {
            List<Patient> snapshot = q.getAll();
            boolean found = snapshot.stream().anyMatch(p -> p.getId() == patientId);
            if (found) {
                q.remove(patientId);
                allPatients.removeIf(p -> p.getId() == patientId);
                break;
            }
        }
        for (CycleQueue cq : queues) timeStep(cq);
        return getAllDoctorStatus();
    }

    /** 重置所有队列 */
    public synchronized List<QueueStatusVO> reset() {
        for (CycleQueue q : queues) q.clear();
        allPatients.clear();
        idx = 0;
        return getAllDoctorStatus();
    }

    /** 获取所有医生队列的状态 */
    public synchronized List<QueueStatusVO> getStatus() {
        return getAllDoctorStatus();
    }

    /**
     * FIX: 全局平均等待时间——8 个医生队列里所有患者的 waitTime 求平均
     */
    public synchronized double getAvgWaitTime() {
        double sum = 0;
        int count = 0;
        for (CycleQueue q : queues) {
            timeStep(q);
            int i = q.getFront();
            while (i != q.getRear()) {
                Patient p = q.getPatients()[i];
                if (p != null) {
                    sum += p.getWaitTime();
                    count++;
                }
                i = (i + 1) % q.getMaxLength();
            }
        }
        return count > 0 ? sum / count : 0;
    }

    /** FIX: 给 HeapService 用——返回所有医生队列中所有患者的快照 */
    public synchronized List<Patient> getAllPatientsSnapshot() {
        // 每次取快照前确保每个医生队列的 waitTime 是最新的
        for (CycleQueue q : queues) timeStep(q);
        List<Patient> result = new ArrayList<>();
        for (CycleQueue q : queues) {
            result.addAll(q.getAll());
        }
        return result;
    }

    /** FIX: 返回指定医生的队列状态 */
    public synchronized QueueStatusVO getDoctorStatus(int doctorIndex) {
        if (doctorIndex < 0 || doctorIndex >= DOCTOR_COUNT) {
            QueueStatusVO vo = new QueueStatusVO();
            vo.setQueueSize(0);
            return vo;
        }
        return buildDoctorStatus(queues[doctorIndex], doctorIndex);
    }

    // ========== 私有方法 ==========

    /** FIX: 找当前患者数最少的医生队列——实现负载均衡调度 */
    private int findShortestQueue() {
        int minSize = Integer.MAX_VALUE;
        int minIdx = 0;
        for (int i = 0; i < DOCTOR_COUNT; i++) {
            if (queues[i].getLength() < minSize) {
                minSize = queues[i].getLength();
                minIdx = i;
            }
        }
        return minIdx;
    }

    /** 解析 patients.txt 一行 → Patient */
    private Patient parseLine(String line) {
        String[] s = line.split("\\s+");
        String rawId = s[0];
        int numericId = Integer.parseInt(rawId.replace(ID_PREFIX, ""));
        java.time.LocalTime arrival = java.time.LocalTime.parse(s[2]);
        int treatMin = Integer.parseInt(s[4].replace("min", ""));
        int priority = Integer.parseInt(s[6]);
        return new Patient(numericId, null, arrival, treatMin, priority, 0);
    }

    /** 计算单个医生队列里每个患者的等待时间（队首=0，往后累加 treatTime） */
    private void timeStep(CycleQueue q) {
        if (q == null || q.isEmpty()) return;
        int waitTime = 0;
        int id = q.getFront();
        while (id != q.getRear()) {
            Patient p = q.getPatients()[id];
            if (p == null) {
                id = (id + 1) % q.getMaxLength();
                continue;
            }
            p.setWaitTime(waitTime);
            waitTime += p.getTreatTime();
            id = (id + 1) % q.getMaxLength();
        }
    }

    /** FIX: 构建所有医生队列状态 */
    private List<QueueStatusVO> getAllDoctorStatus() {
        List<QueueStatusVO> list = new ArrayList<>();
        for (int i = 0; i < DOCTOR_COUNT; i++) {
            list.add(buildDoctorStatus(queues[i], i));
        }
        return list;
    }

    /** 构建单个医生的 QueueStatusVO */
    private QueueStatusVO buildDoctorStatus(CycleQueue queue, int doctorIndex) {
        QueueStatusVO vo = new QueueStatusVO();
        vo.setDoctorIndex(doctorIndex);  // FIX: 标识这是哪个医生
        vo.setDoctorName("医生" + (doctorIndex + 1));
        if (queue == null) {
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
        // 单个医生自己的平均等待时间
        vo.setAvgWaitTime(calcDoctorAvg(queue));
        vo.setPatientList(queue.getAll().stream().map(this::toPatientVO).toList());
        return vo;
    }

    /** 计算单个医生队列的平均等待时间 */
    private double calcDoctorAvg(CycleQueue q) {
        double sum = 0;
        int count = 0;
        int i = q.getFront();
        while (i != q.getRear()) {
            Patient p = q.getPatients()[i];
            if (p != null) {
                sum += p.getWaitTime();
                count++;
            }
            i = (i + 1) % q.getMaxLength();
        }
        return count > 0 ? sum / count : 0;
    }

    private PatientVO toPatientVO(Patient p) {
        PatientVO v = new PatientVO();
        v.setId(formatId(p.getId()));
        v.setName(p.getName());
        v.setArriveTime(p.getArriveTime().format(HH_MM));
        v.setTreatTime(p.getTreatTime());
        v.setPriority(p.getPriority());
        v.setWaitTime(p.getWaitTime());
        return v;
    }

    public static String formatId(int numericId) {
        return ID_PREFIX + String.format("%0" + ID_DIGITS + "d", numericId);
    }

    public int getDoctorCount() {
        return DOCTOR_COUNT;
    }
}
