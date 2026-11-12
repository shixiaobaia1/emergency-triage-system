package org.example.queue.service;

import org.example.queue.algorithm.CycleQueue;
import org.example.queue.entity.Patient;
import org.example.queue.vo.PatientVO;
import org.example.queue.vo.QueueStatusVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

@Service
public class QueueService {

    // 医生人数，可以前端回传再调
    private static final int DEFAULT_DOCTOR_COUNT = 8;
    // FIX: 模拟时钟起点 08:00，每 5 秒推进 1 分钟
    private static final LocalTime SIM_START = LocalTime.of(8, 0);

    // 每个医生有自己的循环队列
    private CycleQueue[] queues;

    //每个医生"当前正在治疗的患者的结束时间"，null 表示该医生空闲
    private LocalTime[] doctorBusyUntil;

    //模拟当前时间
    private LocalTime simTime = SIM_START;

    // 计时器开关——load 后开始，reset 后停止
    private boolean running = false;

    //全局等待池排序规则——先按到达时间升序，同时到的按优先级升序（数字越小越紧急），用比较器做
    private final PriorityQueue<Patient> waitingPool = new PriorityQueue<>(
            Comparator.comparing(Patient::getArriveTime)
                    .thenComparing(Patient::getPriority)
    );

    // 所有仍在排队（等待池 + 医生队列）的患者总列表
    private final List<Patient> allPatients = new ArrayList<>();
    //找资源了
    @Value("classpath:static/patients.txt")
    private Resource resource;

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final String ID_PREFIX = "P";
    private static final int ID_DIGITS = 3;

    // 启动时先按默认 8 个医生初始化，避免队列为 null
    public QueueService() {
        initQueues(DEFAULT_DOCTOR_COUNT);
    }

    /** 按指定医生人数重建队列数组（前端传几个医生就有几个队列） */
    private void initQueues(int doctorCount) {
        //防止医生人数有问题
        int count = Math.max(1, doctorCount);
        queues = new CycleQueue[count];
        doctorBusyUntil = new LocalTime[count];
        for (int i = 0; i < count; i++) {
            queues[i] = new CycleQueue(100);
        }
    }

    /**
     * 加载患者数据，按传入的医生人数重建队列，并把模拟时钟复位到 08:00
     * 加载完成后计时器自动开始跑
     */
    public synchronized void loadFromFile(int doctorCount) throws IOException {
        if (resource == null || !resource.exists()) {
            throw new IOException("找不到患者数据文件 static/patients.txt");
        }
        List<String> lines;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            lines = br.lines()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        //初始化
        initQueues(doctorCount);
        waitingPool.clear();
        allPatients.clear();
        simTime = SIM_START;
        running = true;

        // 解析所有患者，全部先放进全局等待池（到点后才会分配进医生队列）
        for (String line : lines) {
            Patient p = parseLine(line);
            waitingPool.offer(p);
            allPatients.add(p);
        }

        // 加载时就按调度规则预计每位患者的总体等待时间
        recalcTotalWaitTime();
        // 起始时刻 08:00 就到点的患者，立即分配进医生队列并开始治疗
        dispatchArrivedPatients();
        processTreatments();
        updateCurrentWaitTimes();
    }

    /**
     * FIX: 计时器——每 5 秒推进 1 分钟模拟时间
     * 每跳一分钟做三件事：
     *   1) 把已到达时间 <= 当前时间的患者从等待池分配进医生队列
     *   2) 处理治疗：医生当前患者治疗时间到了就离开队列
     *   3) 重算所有患者的即时等待时间
     */
    @Scheduled(fixedRate = 2000)//每隔二秒执行一次下面的方法
    public synchronized void tick() {
        //重置没加载就不搞
        if (!running) return;
        simTime = simTime.plusMinutes(1);
        dispatchArrivedPatients();
        processTreatments();
        updateCurrentWaitTimes();
    }

    /** 手动推进一分钟（调试用，正常由计时器自动推进） */
    public synchronized List<QueueStatusVO> addOneFromFile() {
        if (!running) return getAllDoctorStatus();
        tickOnce();
        return getAllDoctorStatus();
    }

    /** 推进一步模拟（tick 和 addOneFromFile 共用，避免代码重复） */
    private void tickOnce() {
        simTime = simTime.plusMinutes(1);
        dispatchArrivedPatients();
        processTreatments();
        updateCurrentWaitTimes();
    }

    /**
     * 把"到达时间 <= 当前模拟时间"的患者从等待池分配进医生队列
     */
    private void dispatchArrivedPatients() {
        while (!waitingPool.isEmpty()) {
            Patient head = waitingPool.peek();
            //不到队首的时间意味着所有的患者都没到等待时间，可以不管
            if (head == null || head.getArriveTime().isAfter(simTime)) break;
            waitingPool.poll();
            // 分配给当前"总治疗时间最短"的医生队列
            insertToDoctor(findMinTreatTimeQueue(), head);
        }
    }

    /** 把患者塞进指定医生的循环队列，满了自动扩容 */
    private void insertToDoctor(int doctorIndex, Patient patient) {
        CycleQueue target = queues[doctorIndex];
        if (!target.insertPriority(patient)) {
            List<Patient> all = target.getAll();
            CycleQueue newQ = new CycleQueue(target.getMaxLength() + all.size() + 10);
            for (Patient p : all) newQ.insertPriority(p);
            newQ.insertPriority(patient);
            queues[doctorIndex] = newQ;
        }
    }

    /**
     * FIX: 处理治疗——每个医生串行治疗队首患者
     * 队首刚开始治疗时记录结束时间；当前时间到达结束时间后队首离开队列，下一位接着开始
     */
    private void processTreatments() {
        for (int i = 0; i < queues.length; i++) {
            CycleQueue q = queues[i];
            if (q.isEmpty()) {
                doctorBusyUntil[i] = null;
                continue;
            }
            if (doctorBusyUntil[i] == null) {
                doctorBusyUntil[i] = simTime.plusMinutes(headPatient(q).getTreatTime());
            }
            while (!q.isEmpty() && !simTime.isBefore(doctorBusyUntil[i])) {
                Patient done = q.deQueue();
                // 治疗完成 → 离开队列，也离开排队总表
                allPatients.removeIf(p -> p.getId() == done.getId());
                if (q.isEmpty()) {
                    doctorBusyUntil[i] = null;
                } else {
                    doctorBusyUntil[i] = doctorBusyUntil[i].plusMinutes(headPatient(q).getTreatTime());
                }
            }
        }
    }

    /** FIX: 手动让指定医生的队首患者立即完成治疗（调试用，正常由计时器自动完成） */
    public synchronized List<QueueStatusVO> completeHead(int doctorIndex) {
        if (doctorIndex < 0 || doctorIndex >= queues.length) return getAllDoctorStatus();
        CycleQueue q = queues[doctorIndex];
        if (!q.isEmpty()) {
            Patient done = q.deQueue();
            allPatients.removeIf(p -> p.getId() == done.getId());
            doctorBusyUntil[doctorIndex] = q.isEmpty()
                    ? null
                    : simTime.plusMinutes(headPatient(q).getTreatTime());
            recalcTotalWaitTime();
            updateCurrentWaitTimes();
        }
        return getAllDoctorStatus();
    }

    /** 患者放弃排队：从医生队列 / 等待池中抹除（txt 文件不动），并重算剩余患者的总体等待时间 */
    public synchronized List<QueueStatusVO> giveUp(int patientId) {
        boolean removed = false;
        for (int i = 0; i < queues.length; i++) {
            CycleQueue q = queues[i];
            if (q.getAll().stream().anyMatch(p -> p.getId() == patientId)) {
                q.remove(patientId);
                removed = true;
                break;
            }
        }
        if (!removed) {
            removed = waitingPool.removeIf(p -> p.getId() == patientId);
        }
        if (removed) {
            allPatients.removeIf(p -> p.getId() == patientId);
            // FIX: 有患者放弃 → 受影响患者的总体等待时间要重算
            recalcTotalWaitTime();
            updateCurrentWaitTimes();
        }
        return getAllDoctorStatus();
    }

    /** 重置所有队列，模拟时钟复位并停止计时 */
    public synchronized List<QueueStatusVO> reset() {
        for (CycleQueue q : queues) q.clear();
        for (int i = 0; i < doctorBusyUntil.length; i++) doctorBusyUntil[i] = null;
        waitingPool.clear();
        allPatients.clear();
        simTime = SIM_START;
        running = false;
        return getAllDoctorStatus();
    }

    /** 获取所有医生队列的状态 */
    public synchronized List<QueueStatusVO> getStatus() {
        return getAllDoctorStatus();
    }

    /**
     * 全局平均等待时间——所有仍在排队患者（医生队列 + 等待池）的"即时等待时间"总平均
     */
    public synchronized double getAvgWaitTime() {
        updateCurrentWaitTimes();
        int sum = 0;
        int count = 0;
        for (Patient p : allPatients) {
            sum += p.getCurrentWaitTime();
            count++;
        }
        return count > 0 ? (double) sum / count : 0;
    }

    /** 给 HeapService 用——返回所有仍在排队的患者快照（医生队列 + 等待池） */
    public synchronized List<Patient> getAllPatientsSnapshot() {
        updateCurrentWaitTimes();
        return new ArrayList<>(allPatients);
    }

    /** 返回指定医生的队列状态 */
    public synchronized QueueStatusVO getDoctorStatus(int doctorIndex) {
        if (doctorIndex < 0 || doctorIndex >= queues.length) {
            QueueStatusVO vo = new QueueStatusVO();
            vo.setQueueSize(0);
            return vo;
        }
        return buildDoctorStatus(queues[doctorIndex], doctorIndex);
    }

    /** 查看全局等待池里还有多少患者 */
    public synchronized int getWaitingPoolSize() {
        return waitingPool.size();
    }

    /** FIX: 当前模拟时间（HH:mm），方便前端/调试看时钟走到哪了 */
    public synchronized String getSimTime() {
        return simTime.format(HH_MM);
    }

    // ========== 私有方法 ==========

    /**
     * FIX: 预计每位患者的"总体等待时间"（到达时间 → 预计真正就诊时间）
     *
     * 核心公式：
     *   预计就诊时刻 = max(患者到达时间, 医生空闲时刻)
     *   总体等待时间 = 预计就诊时刻 - 患者到达时间
     * 每安排一个人后：医生空闲时刻 = 该患者预计开始时刻 + 他的治疗时长
     *
     * 分两批算：
     *   ① 已在医生队列里的患者——排位已固定，按队列顺序依次累加，不能插队重排
     *   ② 还在等待池里的患者——还没分配医生，按"到达时间升序 + 同时到优先级小的先"排序后，
     *      逐个挑当前空闲最早的医生（贪心预测，只算时间，不真的入队）
     */
    private void recalcTotalWaitTime() {
        LocalTime[] doctorFree = new LocalTime[queues.length];
        for (int i = 0; i < queues.length; i++) {
            LocalTime free = simTime;
            if (doctorBusyUntil[i] != null && doctorBusyUntil[i].isAfter(free)) {
                free = doctorBusyUntil[i];
            }
            // 队列里已有的患者依次占用医生时间
            for (Patient p : queues[i].getAll()) {
                LocalTime start = p.getArriveTime().isAfter(free) ? p.getArriveTime() : free;
                p.setTotalWaitTime(minutesBetween(p.getArriveTime(), start));
                free = start.plusMinutes(p.getTreatTime());
            }
            doctorFree[i] = free;
        }
        // 等待池里的患者：按到达时间升序、同到达按优先级升序，逐个分给预计空闲最早的医生
        List<Patient> pool = new ArrayList<>(waitingPool);
        pool.sort(Comparator.comparing(Patient::getArriveTime).thenComparing(Patient::getPriority));
        for (Patient p : pool) {
            int idx = 0;
            for (int i = 1; i < doctorFree.length; i++) {
                if (doctorFree[i].isBefore(doctorFree[idx])) idx = i;
            }
            LocalTime start = p.getArriveTime().isAfter(doctorFree[idx]) ? p.getArriveTime() : doctorFree[idx];
            p.setTotalWaitTime(minutesBetween(p.getArriveTime(), start));
            doctorFree[idx] = start.plusMinutes(p.getTreatTime());
        }
    }

    /**
     * FIX: 重算每位患者的"即时等待时间"（当前这一刻还要等多久才能就诊）
     *
     * 医生队列内：
     *   队首正在治疗 → 0
     *   第 k 位（k >= 1）= 队首剩余治疗时间 + 第 1..k-1 位的治疗时长之和
     *
     * 等待池内：
     *   还没到点 → 距离到达还有几分钟 + 总体等待时间
     *   已到点   → 总体等待时间 - 已经等了多久（不小于 0）
     */
    private void updateCurrentWaitTimes() {
        // 医生队列内：队首正在治疗 → 0；后面每位 = 前面所有人剩余治疗时间之和
        for (int i = 0; i < queues.length; i++) {
            List<Patient> list = queues[i].getAll();
            if (list.isEmpty()) continue;
            int offset = 0;
            if (doctorBusyUntil[i] != null && doctorBusyUntil[i].isAfter(simTime)) {
                offset = minutesBetween(simTime, doctorBusyUntil[i]);
            }
            for (int k = 0; k < list.size(); k++) {
                Patient p = list.get(k);
                if (k == 0) {
                    p.setCurrentWaitTime(0);
                } else {
                    p.setCurrentWaitTime(offset + sumTreatTime(list, 1, k));
                }
            }
        }
        // 等待池内：还没到点的 = 到点前的分钟数 + 总体等待时间；已到点的 = 预计总等待剩余
        for (Patient p : waitingPool) {
            if (p.getArriveTime().isAfter(simTime)) {
                p.setCurrentWaitTime(minutesBetween(simTime, p.getArriveTime()) + p.getTotalWaitTime());
            } else {
                int elapsed = minutesBetween(p.getArriveTime(), simTime);
                p.setCurrentWaitTime(Math.max(0, p.getTotalWaitTime() - elapsed));
            }
        }
    }

    /** 累加 list 中 [from, to) 区间患者的治疗时间 */
    private int sumTreatTime(List<Patient> list, int from, int to) {
        int sum = 0;
        for (int i = from; i < to && i < list.size(); i++) {
            sum += list.get(i).getTreatTime();
        }
        return sum;
    }

    /** 两个时刻之间的分钟数（负数按 0 处理，患者都在同一天内，不会跨天） */
    private int minutesBetween(LocalTime from, LocalTime to) {
        return (int) Math.max(0, Duration.between(from, to).toMinutes());
    }

    /** 取循环队列队首患者 */
    private Patient headPatient(CycleQueue q) {
        return q.getPatients()[q.getFront()];
    }

    /** 找当前"总治疗时间最短"的医生队列 */
    private int findMinTreatTimeQueue() {
        int minTime = Integer.MAX_VALUE;
        int minIdx = 0;
        for (int i = 0; i < queues.length; i++) {
            int total = 0;
            for (Patient p : queues[i].getAll()) {
                total += p.getTreatTime();
            }
            if (total < minTime) {
                minTime = total;
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
        LocalTime arrival = LocalTime.parse(s[2]);
        int treatMin = Integer.parseInt(s[4].replace("min", ""));
        int priority = Integer.parseInt(s[6]);
        return new Patient(numericId, null, arrival, treatMin, priority);
    }

    /** 构建所有医生队列状态 */
    private List<QueueStatusVO> getAllDoctorStatus() {
        List<QueueStatusVO> list = new ArrayList<>();
        for (int i = 0; i < queues.length; i++) {
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

    /** 计算单个医生队列的平均即时等待时间 */
    private double calcDoctorAvg(CycleQueue q) {
        List<Patient> list = q.getAll();
        if (list.isEmpty()) return 0;
        int sum = 0;
        for (Patient p : list) sum += p.getCurrentWaitTime();
        return (double) sum / list.size();
    }

    /** Entity → VO：id 由 int 还原成 "P001"，到达时间格式化成 HH:mm */
    private PatientVO toPatientVO(Patient p) {
        PatientVO v = new PatientVO();
        v.setId(formatId(p.getId()));
        v.setName(p.getName());
        v.setArriveTime(p.getArriveTime().format(HH_MM));
        v.setTreatTime(p.getTreatTime());
        v.setPriority(p.getPriority());
        v.setTotalWaitTime(p.getTotalWaitTime());
        v.setCurrentWaitTime(p.getCurrentWaitTime());
        return v;
    }

    /** int → "P001" 格式：Entity 内部存数字，对外展示统一用带 P 前缀的编号 */
    public static String formatId(int numericId) {
        return ID_PREFIX + String.format("%0" + ID_DIGITS + "d", numericId);
    }

    /** 返回当前实际医生人数 */
    public int getDoctorCount() {
        return queues.length;
    }
}
