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
    // 模拟时钟起点 08:00，每 2 秒推进 1 分钟
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

    // FIX: 全部患者总列表（含已治疗完成的患者，堆排序/平均等待时间统计仍包含他们）
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
        //加载数据
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

        // 解析所有患者，全部先放进全局等待池和患者队列（到点后才会分配进医生队列）
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
     * 计时器——每 2 秒推进 1 分钟模拟时间
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
            //扩容
            List<Patient> all = target.getAll();
            CycleQueue newQ = new CycleQueue(target.getMaxLength() + all.size() + 10);
            for (Patient p : all) newQ.insertPriority(p);
            newQ.insertPriority(patient);
            queues[doctorIndex] = newQ;
        }
    }

    /**
     * 处理治疗——每个医生串行治疗队首患者
     * 队首刚开始治疗时记录结束时间；当前时间到达结束时间后队首离开队列，下一位接着开始
     * 离开队列的患者不会从 allPatients 删除，并把"到达 → 开始治疗"的实际等待时间定格记录下来
     */
    private void processTreatments() {
        for (int i = 0; i < queues.length; i++) {
            CycleQueue q = queues[i];
            //空了就记录医生空闲
            if (q.isEmpty()) {
                doctorBusyUntil[i] = null;
                continue;
            }
            //拿第一个人的治疗时间+现在的时间是医生现在的空闲时间
            if (doctorBusyUntil[i] == null) {
                doctorBusyUntil[i] = simTime.plusMinutes(headPatient(q).getTreatTime());
            }
            while (!q.isEmpty() && !simTime.isBefore(doctorBusyUntil[i])) {
                // 结束时间 - 治疗耗时 = 该患者实际开始治疗的时间（找开始的时间）
                LocalTime startAt = doctorBusyUntil[i].minusMinutes(headPatient(q).getTreatTime());
                Patient done = q.deQueue();
                // 记录已诊断患者的 id 与最终等待时间（等待时间定格，不再随时钟变化），开始治疗时间减到达时间
                done.setTotalWaitTime(minutesBetween(done.getArriveTime(), startAt));
                done.setCurrentWaitTime(0);
                if (q.isEmpty()) {
                    doctorBusyUntil[i] = null;
                } else {
                    doctorBusyUntil[i] = doctorBusyUntil[i].plusMinutes(headPatient(q).getTreatTime());
                }
            }
        }
    }

    /** 手动让指定医生的队首患者立即完成治疗 */
    public synchronized List<QueueStatusVO> completeHead(int doctorIndex) {
        if (doctorIndex < 0 || doctorIndex >= queues.length) return getAllDoctorStatus();//这是有问题的情况
        CycleQueue q = queues[doctorIndex];
        if (!q.isEmpty()) {
            // 手动治疗完成：同样记录最终等待时间，并保留在全部患者总表中
            Patient done = headPatient(q);
            // 实际开始治疗时刻——如果已经在治的就用"原定结束时间 - 原治疗时长"反推；还没开始的就算此刻是治疗时间也是结束时间
            LocalTime startAt = doctorBusyUntil[doctorIndex] == null//这是存本来这个患者什么时候结束
                    ? simTime
                    : doctorBusyUntil[doctorIndex].minusMinutes(done.getTreatTime());
            q.deQueue();
            // 治疗时长为"开始治疗 → 点击完成"实际经过的分钟数
            done.setTreatTime(minutesBetween(startAt, simTime));
            done.setTotalWaitTime(minutesBetween(done.getArriveTime(), startAt));
            done.setCurrentWaitTime(0);
            //新的患者上来
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
            //放弃排队也留档——等待时间定格为"放弃时间 - 到达时间"，并保留在全部患者总表中，
            Patient quit = allPatients.stream().filter(p -> p.getId() == patientId).findFirst().orElse(null);
            if (quit != null) {
                quit.setTotalWaitTime(minutesBetween(quit.getArriveTime(), simTime));
                quit.setCurrentWaitTime(0);
            }
            //有患者放弃 → 受影响患者的总体等待时间要重算
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
     * 全局平均等待时间——全部患者（含已治疗完成）的"总体等待时间"总平均
     */
    public synchronized double getAvgWaitTime() {
        updateCurrentWaitTimes();
        int sum = 0;
        int count = 0;
        for (Patient p : allPatients) {
            sum += p.getTotalWaitTime();
            count++;
        }
        //确定当前到底有多少个患者
        return count > 0 ? (double) sum / count : 0;
    }

    /** 给 HeapService 用——返回全部患者快照 */
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

    /** 当前模拟时间（HH:mm），方便前端/调试看时钟走到哪了 */
    public synchronized String getSimTime() {
        return simTime.format(HH_MM);
    }

    /**
     * 预计每位患者的"总体等待时间"（到达时间 → 预计真正就诊时间）
     *
     * 【核心公式】
     *   预计就诊时刻 = max(患者到达时间, 医生空闲时刻)
     *   总体等待时间 = 预计就诊时刻 - 患者到达时间
     *   安排完一个人后：医生空闲时刻 = 该患者预计开始时刻 + 他的治疗时长
     *
     *   医生队列里的患者：排位已经固定，只能按队列顺序一个接一个往后排，不能重新调换顺序
     *   等待池里的患者：还没分配医生，需要预测"将来会被分给哪位医生"，所以要重新模拟一次调度
     *
     * 【第一批：已在医生队列里的患者】
     *   对每一位医生：
     *     1) 先确定这位医生的"空闲起点" free
     *        - 一般情况 free = 当前模拟时间
     *        - 如果医生手上有患者正在治疗，free = 那位患者的预计结束时间
     *     2) 按队列顺序（front → rear）一位一位往后推：
     *        - 普通患者：开始时刻 = max(他自己的到达时间, free)
     *          （人还没到就等到达；已经到了就等医生空出来）
     *        - 队首且正在治疗：开始时刻 = 预计结束时间 - 治疗时长
     *          （他已经在治了，如果把"结束时间"当成"开始时间"，等待时间会多算一个治疗时长）
     *        - 算完一个人，free 更新为"他开始时刻 + 他的治疗时长"，再算下一位
     *     3) 该医生队列排完，free 的值就是这位医生预计什么时候彻底空闲，存入 doctorFree[]
     *
     * 【第二批：还在等待池里的患者】
     *   1) 先把等待池复制一份并排序：到达时间早的在前；到达时间相同的，优先级数字小的在前
     *   2) 依次处理每位患者：
     *      - 在 doctorFree[] 里挑"预计空闲最早"的医生（贪心：谁最早空就给谁）
     *      - 该患者开始时刻 = max(他的到达时间, 这位医生的空闲时刻)
     *      - 算完后把这位医生的空闲时刻往后推：开始时刻 + 他的治疗时长
     *   3) 注意：这里只是"预测"，不会真的把患者塞进医生队列；
     *      真正入队发生在 dispatchArrivedPatients()，即患者到达时间到了才执行
     */
    private void recalcTotalWaitTime() {
        // doctorFree[i] 记录第 i 位医生"预计什么时候彻底空闲"，留给第二批（等待池）预测用
        LocalTime[] doctorFree = new LocalTime[queues.length];
        for (int i = 0; i < queues.length; i++) {
            // free = 这位医生的"空闲起点"，默认从现在开始
            LocalTime free = simTime;
            // 如果医生手上有人正在治疗，就从那位患者的预计结束时刻开始算
            if (doctorBusyUntil[i] != null && doctorBusyUntil[i].isAfter(free)) {
                free = doctorBusyUntil[i];
            }
            // 队列里已有的患者依次占用医生时间
            boolean first = true;      // 标记当前遍历到的是不是队首
            for (Patient p : queues[i].getAll()) {
                LocalTime start;
                if (first && doctorBusyUntil[i] != null) {
                    // 队首已经在治疗，开始时刻 = 预计治完时刻 - 治疗时长，不能再按"治完时刻"往后推
                    start = doctorBusyUntil[i].minusMinutes(p.getTreatTime());
                } else {
                    // 普通患者：自己到了才可能开始（取到达时间）；到了就等医生空出来（取 free）
                    start = p.getArriveTime().isAfter(free) ? p.getArriveTime() : free;
                }
                // 总体等待时间 = 开始治疗时刻 - 到达时间
                p.setTotalWaitTime(minutesBetween(p.getArriveTime(), start));
                // 这个人治完后医生才有空：空闲时刻 = 开始时刻 + 他的治疗时长
                free = start.plusMinutes(p.getTreatTime());
                first = false;
            }
            // 该医生队列全部排完，free 就是这位医生预计彻底空闲的时刻
            doctorFree[i] = free;
        }
        // 等待池里的患者：按到达时间升序、同到达按优先级升序，逐个分给预计空闲最早的医生
        List<Patient> pool = new ArrayList<>(waitingPool);   // 复制一份，不影响真正的等待池
        pool.sort(Comparator.comparing(Patient::getArriveTime).thenComparing(Patient::getPriority));
        for (Patient p : pool) {
            int idx = 0;      // 预计空闲最早的医生下标
            for (int i = 1; i < doctorFree.length; i++) {
                if (doctorFree[i].isBefore(doctorFree[idx])) idx = i;   // 发现更早空闲的就换人
            }
            // 他开始就诊的时刻 = max(他的到达时间, 这位医生的空闲时刻)
            LocalTime start = p.getArriveTime().isAfter(doctorFree[idx]) ? p.getArriveTime() : doctorFree[idx];
            p.setTotalWaitTime(minutesBetween(p.getArriveTime(), start));
            // 把这位医生的空闲时刻往后推，供下一位等待池患者继续预测
            doctorFree[idx] = start.plusMinutes(p.getTreatTime());
        }
    }

    /**
     * 重算每位患者的"即时等待时间"（当前这一刻还要等多久才真正就诊）
     *
     * 【和上面"总体等待时间"的区别】
     *   总体等待时间：从"到达时间"算到"预计真正就诊"，是个基准值，只在加载/有人放弃时重算
     *   即时等待时间：从"现在"算到"预计真正就诊"，每跳一分钟都会重算一次，数值不断变小
     *
     * 【医生队列内】offset 表示队首还要治多久（= 队首结束时间 - 当前时间；医生空闲时为 0）
     *   队首（k == 0）：正在治疗 → 0（已经轮到他了，不用再等）
     *   第 k 位（k >= 1）：offset + 第 1..k-1 位的治疗时长之和
     *     含义：先等队首治完，再等排在自己前面的那些人依次治完
     *     举例：队首还剩 5 分钟，第 2 位自己前面没人
     *           → 第 2 位还要等 5 分钟
     *           若还有第 3 位、第 2 位要治 10 分钟
     *           → 第 3 位还要等 5 + 10 = 15 分钟
     *
     * 【等待池内】
     *   ① 还没到点（到达时间 > 当前时间）：
     *        还要等 "距离到达的分钟数" + "总体等待时间"
     *        例：09:00 到、现在 08:50、总体等待 20 分钟 → 10 + 20 = 30 分钟后才看上
     *   ② 已经到点（到达时间 <= 当前时间）：
     *        "总体等待时间" - "已经等了多久"，结果不小于 0
     *        例：总体等待 20 分钟、从到达起已经等了 8 分钟 → 还要再等 12 分钟
     */
    private void updateCurrentWaitTimes() {
        // 医生队列内：队首正在治疗 → 0；后面每位 = 前面所有人剩余治疗时间之和
        for (int i = 0; i < queues.length; i++) {
            List<Patient> list = queues[i].getAll();
            if (list.isEmpty()) continue;      // 这位医生队列是空的，不用算
            int offset = 0;                    // 队首还要治疗多久；医生空闲时为 0
            if (doctorBusyUntil[i] != null && doctorBusyUntil[i].isAfter(simTime)) {
                offset = minutesBetween(simTime, doctorBusyUntil[i]);
            }
            for (int k = 0; k < list.size(); k++) {
                Patient p = list.get(k);
                if (k == 0) {
                    p.setCurrentWaitTime(0);   // 队首正在治疗，已经轮到他，不用再等
                } else {
                    // 第 k 位 = 先等队首治完 + 再等排在他前面的 1..k-1 位依次治完
                    p.setCurrentWaitTime(offset + sumTreatTime(list, 1, k));
                }
            }
        }
        // 等待池内：还没到点的 = 到点前的分钟数 + 总体等待时间；已到点的 = 预计总等待剩余
        for (Patient p : waitingPool) {
            if (p.getArriveTime().isAfter(simTime)) {
                // 还没到的：先等"距离到达的时间"，到点后还要按总体等待时间继续等
                p.setCurrentWaitTime(minutesBetween(simTime, p.getArriveTime()) + p.getTotalWaitTime());
            } else {
                // 已经到的：总体等待时间减去已经等掉的时长，结果不小于 0
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
        int minTime = Integer.MAX_VALUE;//记最小时长
        int minIdx = 0;//记最小时长对应的医生
        for (int i = 0; i < queues.length; i++) {
            int total = 0;
            //看医生队列里目前的总治疗时长
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
