package org.example.queue.schedule;

import org.example.queue.entity.Patient;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * 多医生并发调度器。
 * <p>
 * 核心思想：N 个医生就像 N 条"服务窗口"，同时接诊。
 * 时间一点点往前走：
 *   1. 把"已经到达"的患者放进等待队列；
 *   2. 只要有医生空闲、队列里有患者，就让空闲医生接诊"最着急"的患者；
 *   3. 患者的等待时间 = 开始接诊时刻 - 到达时刻。
 * <p>
 * 医生越多，同一时间被接诊的患者越多，总等待时间越短。
 */
public class Scheduler {

    private final int doctorCount;
    private final Doctor[] doctors;

    public Scheduler(int doctorCount) {
        if (doctorCount < 1) {
            throw new IllegalArgumentException("医生数量至少为 1");
        }
        this.doctorCount = doctorCount;
        this.doctors = new Doctor[doctorCount];
        for (int i = 0; i < doctorCount; i++) {
            doctors[i] = new Doctor(i + 1);
        }
    }

    public int getDoctorCount() {
        return doctorCount;
    }

    public Doctor[] getDoctors() {
        return doctors;
    }

    /**
     * 执行调度，给每个患者设置好等待时间（p.setWaitTime），并分配医生。
     * 规则：优先级数字越小越急（1 最急），先看；同级按到达先后。
     */
    public void dispatch(List<Patient> patients) {
        if (patients == null || patients.isEmpty()) {
            return;
        }

        // 按到达时间排序（未到的人不能提前看）
        List<Patient> arrivals = new ArrayList<>(patients);
        arrivals.sort(Comparator.comparing(Patient::getArriveTime));

        // 等待队列：优先级小的先出；优先级相同按到达时间
        PriorityQueue<Patient> waiting = new PriorityQueue<>(
                Comparator.comparingInt(Patient::getPriority)
                        .thenComparing(Patient::getArriveTime));

        // 所有医生在"第一个患者到达"的时刻就位
        LocalTime clock = arrivals.get(0).getArriveTime();
        for (Doctor d : doctors) {
            d.setFreeAt(clock);
        }

        int idx = 0;
        while (idx < arrivals.size() || !waiting.isEmpty()) {
            // 1. 把 clock 之前（含）到达的患者全部入队
            while (idx < arrivals.size() && !arrivals.get(idx).getArriveTime().isAfter(clock)) {
                waiting.add(arrivals.get(idx++));
            }

            // 2. 找一个当前空闲的医生
            Doctor doc = availableDoctor(clock);
            if (doc == null) {
                // 所有医生都在忙：时间跳到"最早空闲的医生"时刻
                clock = earliestFreeTime();
                continue;
            }

            // 3. 医生有空但没患者：跳到下一个患者到达的时刻
            if (waiting.isEmpty()) {
                if (idx >= arrivals.size()) {
                    break;
                }
                clock = arrivals.get(idx).getArriveTime();
                continue;
            }

            // 4. 空闲医生接诊最着急的患者
            Patient p = waiting.poll();
            p.setWaitTime((int) Duration.between(p.getArriveTime(), clock).toMinutes());
            doc.assign(p, clock);   // 医生忙到 clock + 诊疗时长
        }
    }

    /** 找一个当前（freeAt <= clock）空闲的医生，优先返回最早就闲下来的 */
    private Doctor availableDoctor(LocalTime clock) {
        Doctor best = null;
        for (Doctor d : doctors) {
            if (!d.getFreeAt().isAfter(clock)) {
                if (best == null || d.getFreeAt().isBefore(best.getFreeAt())) {
                    best = d;
                }
            }
        }
        return best;
    }

    /** 所有医生中最早的空闲时间 */
    private LocalTime earliestFreeTime() {
        LocalTime t = doctors[0].getFreeAt();
        for (Doctor d : doctors) {
            if (d.getFreeAt().isBefore(t)) {
                t = d.getFreeAt();
            }
        }
        return t;
    }

    /** 计算所有患者的平均等待时间（分钟） */
    public double averageWaitTime(List<Patient> patients) {
        long total = 0;
        for (Patient p : patients) {
            total += p.getWaitTime();
        }
        return patients.isEmpty() ? 0 : (double) total / patients.size();
    }
}