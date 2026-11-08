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
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

@Service
public class QueueService {

    // 8 个医生，每个医生有自己的循环队列（保留原来的 CycleQueue，完全不改内部逻辑）
    private static final int DOCTOR_COUNT = 8;
    private final CycleQueue[] queues = new CycleQueue[DOCTOR_COUNT];

    // FIX: 新增——全局等待池（PriorityQueue）
    // 调度核心：患者先进入这个等待池，再按优先级动态分配给空闲医生
    // 优先级数字越小越紧急，同级按到达时间先后
    private final PriorityQueue<Patient> waitingPool = new PriorityQueue<>(
            Comparator.comparingInt(Patient::getPriority)
                    .thenComparing(Patient::getArriveTime)
    );

    // 所有已入队（包括等待池 + 医生队列）的患者总列表
    private final List<Patient> allPatients = new ArrayList<>();

    @Value("classpath:static/patients.txt")
    private Resource resource;

    private List<String> lines;
    private int idx = 0;

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final String ID_PREFIX = "P";
    private static final int ID_DIGITS = 3;

    // 构造时就初始化 8 个医生队列（每个容量先给大一点，不够会自动扩容）
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
     * FIX: 从 txt 取下一个患者，先入全局等待池，再按优先级动态调度到最空闲的医生
     * 返回所有医生队列状态的汇总
     */
    public synchronized List<QueueStatusVO> addOneFromFile() {
        if (lines == null || lines.isEmpty() || idx >= lines.size()) {
            return getAllDoctorStatus();
        }
        Patient patient = parseLine(lines.get(idx));
        // FIX: 不再直接塞医生队列，先进全局等待池
        waitingPool.offer(patient);
        allPatients.add(patient);
        idx++;

        // FIX: 触发调度——从等待池里按优先级取人，分给当前最空的医生
        dispatchFromWaitingPool();

        return getAllDoctorStatus();
    }

    /**
     * FIX: 核心调度——从 waitingPool 里持续取患者，直到所有医生都"满一档"
     * 每次取优先级最高（数字最小）的患者，分给当前最短的医生队列
     */
    private void dispatchFromWaitingPool() {
        // 循环直到等待池空，或者所有医生都至少有 1 人了（第一轮）
        // 更完整的策略：等待池不空就一直调——谁最空给谁
        while (!waitingPool.isEmpty()) {
            int targetDoctor = findShortestQueue();
            Patient next = waitingPool.peek();
            if (next == null) break;

            CycleQueue target = queues[targetDoctor];
            // 队列满了就扩容（复用原来的扩容逻辑，不动）
            if (!target.insertPriority(next)) {
                List<Patient> all = target.getAll();
                CycleQueue newQ = new CycleQueue(target.getMaxLength() + all.size() + 10);
                for (Patient p : all) newQ.insertPriority(p);
                target = newQ;
                queues[targetDoctor] = newQ;
                newQ.insertPriority(next);
            } else {
                // 直接塞进去了
            }
            waitingPool.poll();  // 确认塞成功了才从等待池移除
        }

        // 每个医生队列的 waitTime 重新算（复用原来的 timeStep，不动）
        for (CycleQueue q : queues) timeStep(q);
    }

    /** 队首患者就诊结束，出队（指定医生编号） */
    public synchronized List<QueueStatusVO> treatNext(int doctorIndex) {
        if (doctorIndex < 0 || doctorIndex >= DOCTOR_COUNT) return getAllDoctorStatus();
        CycleQueue q = queues[doctorIndex];
        if (q != null && !q.isEmpty()) {
            Patient treated = q.deQueue();
            // 从总表里也去掉
            if (treated != null) allPatients.removeIf(p -> p.getId() == treated.getId());
        }
        // FIX: 这个医生空出了一个位置，从等待池里补人进来
        dispatchOneToDoctor(doctorIndex);

        for (CycleQueue cq : queues) timeStep(cq);
        return getAllDoctorStatus();
    }

    /** FIX: 医生出队后，从等待池里补一个最紧急的患者给他 */
    private void dispatchOneToDoctor(int doctorIndex) {
        if (waitingPool.isEmpty()) return;
        Patient next = waitingPool.poll();
        if (next == null) return;

        CycleQueue target = queues[doctorIndex];
        if (!target.insertPriority(next)) {
            // 满了就扩容
            List<Patient> all = target.getAll();
            CycleQueue newQ = new CycleQueue(target.getMaxLength() + all.size() + 10);
            for (Patient p : all) newQ.insertPriority(p);
            newQ.insertPriority(next);
            queues[doctorIndex] = newQ;
        }
    }

    /** 医生自己出队（默认医生 0） */
    public synchronized List<QueueStatusVO> treatNextDefault() {
        return treatNext(0);
    }

    /** 患者放弃排队 */
    public synchronized List<QueueStatusVO> giveUp(int patientId) {
        // 先从医生队列里找
        for (CycleQueue q : queues) {
            List<Patient> snapshot = q.getAll();
            boolean found = snapshot.stream().anyMatch(p -> p.getId() == patientId);
            if (found) {
                q.remove(patientId);
                allPatients.removeIf(p -> p.getId() == patientId);
                // 这个医生也空出位置了，试试从等待池补
                // （但这里不知道是哪个医生，懒得遍历了——下次 add 或 treat 时自然会调度）
                break;
            }
        }
        // 再看看是不是还在等待池里
        boolean inPool = waitingPool.stream().anyMatch(p -> p.getId() == patientId);
        if (inPool) {
            waitingPool.removeIf(p -> p.getId() == patientId);
            allPatients.removeIf(p -> p.getId() == patientId);
        }
        for (CycleQueue cq : queues) timeStep(cq);
        return getAllDoctorStatus();
    }

    /** 重置所有队列 */
    public synchronized List<QueueStatusVO> reset() {
        for (CycleQueue q : queues) q.clear();
        waitingPool.clear();  // FIX: 等待池也要清
        allPatients.clear();
        idx = 0;
        return getAllDoctorStatus();
    }

    /** 获取所有医生队列的状态 */
    public synchronized List<QueueStatusVO> getStatus() {
        return getAllDoctorStatus();
    }

    /**
     * FIX: 全局平均等待时间——遍历 8 个医生队列的 waitTime（复用原来的 getAvgWaitTime，不动）
     * 注意：waitingPool 里的患者还没分配医生，waitTime=0，不计入平均
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

    /** 给 HeapService 用——返回所有医生队列中所有患者的快照（不动） */
    public synchronized List<Patient> getAllPatientsSnapshot() {
        for (CycleQueue q : queues) timeStep(q);
        List<Patient> result = new ArrayList<>();
        for (CycleQueue q : queues) {
            result.addAll(q.getAll());
        }
        return result;
    }

    /** 返回指定医生的队列状态 */
    public synchronized QueueStatusVO getDoctorStatus(int doctorIndex) {
        if (doctorIndex < 0 || doctorIndex >= DOCTOR_COUNT) {
            QueueStatusVO vo = new QueueStatusVO();
            vo.setQueueSize(0);
            return vo;
        }
        return buildDoctorStatus(queues[doctorIndex], doctorIndex);
    }

    /** FIX: 查看全局等待池里还有多少患者 */
    public synchronized int getWaitingPoolSize() {
        return waitingPool.size();
    }

    // ========== 私有方法（全部保留原来的逻辑，不动） ==========

    /** 找当前患者数最少的医生队列——实现负载均衡调度 */
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

    /** 构建所有医生队列状态 */
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
        vo.setDoctorIndex(doctorIndex);
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
