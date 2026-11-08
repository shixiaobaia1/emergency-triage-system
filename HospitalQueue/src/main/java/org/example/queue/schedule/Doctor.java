package org.example.queue.schedule;

import org.example.queue.entity.Patient;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 医生：一个医生 = 一个并发的服务窗口。
 * <p>
 * 每个医生独立记录自己"忙到几点"（freeAt），以及自己接诊过的患者列表。
 * 当医生空闲时，调度器会把下一个最着急的患者分配给它。
 */
public class Doctor {

    private final int id;                          // 医生编号
    private LocalTime freeAt;                       // 空闲时间点（忙到几点）
    private final List<Patient> handled = new ArrayList<>();  // 该医生接诊过的患者

    public Doctor(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public LocalTime getFreeAt() {
        return freeAt;
    }

    public void setFreeAt(LocalTime freeAt) {
        this.freeAt = freeAt;
    }

    /** 该医生接诊过的患者列表（按接诊顺序） */
    public List<Patient> getHandled() {
        return handled;
    }

    /**
     * 医生从 start 时刻开始接诊患者 p，一直忙到 start + 诊疗时长。
     *
     * @param p     被接诊的患者
     * @param start 开始接诊的时刻
     */
    public void assign(Patient p, LocalTime start) {
        handled.add(p);
        this.freeAt = start.plusMinutes(p.getTreatTime());
    }
}