package org.example.queue.entity;

import java.time.LocalTime;


public class Patient {
    private int id;           // 从 txt 的 "P001" 剥掉 P 前缀解析而来
    private String name;//患者名字
    private LocalTime arriveTime;//到达时间
    private int treatTime;    // 治疗耗时（分钟）
    private int priority;     // 优先级，数字越小越紧急
    private int totalWaitTime;   // 患者到达到真正就诊的时间
    private int currentWaitTime; // 目前还要等多久

    // 无参构造
    public Patient() {}

    // 全参构造（两个等待时间默认 0，由 QueueService 计算后回填）
    public Patient(int id, String name, LocalTime arriveTime, int treatTime, int priority) {
        this.id = id;
        this.name = name;
        this.arriveTime = arriveTime;
        this.treatTime = treatTime;
        this.priority = priority;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalTime getArriveTime() { return arriveTime; }
    public void setArriveTime(LocalTime arriveTime) { this.arriveTime = arriveTime; }

    public int getTreatTime() { return treatTime; }
    public void setTreatTime(int treatTime) { this.treatTime = treatTime; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public int getTotalWaitTime() { return totalWaitTime; }
    public void setTotalWaitTime(int totalWaitTime) { this.totalWaitTime = totalWaitTime; }

    public int getCurrentWaitTime() { return currentWaitTime; }
    public void setCurrentWaitTime(int currentWaitTime) { this.currentWaitTime = currentWaitTime; }
}
