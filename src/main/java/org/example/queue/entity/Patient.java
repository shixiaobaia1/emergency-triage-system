package org.example.queue.entity;

import java.time.LocalTime;

/**
 * 患者实体类
 * FIX: 去掉 Lombok，改回手写构造器 + getter/setter
 */
public class Patient {
    private int id;           // Entity 内部存 int（从 txt 的 "P001" 剥掉 P 前缀解析而来）
    private String name;
    private LocalTime arriveTime;
    private int treatTime;    // 原 proveTime 语义错误（prove=证明），改为 treatTime（治疗耗时）
    private int priority;
    private int waitTime;

    // 无参构造
    public Patient() {}

    // 全参构造
    public Patient(int id, String name, LocalTime arriveTime, int treatTime, int priority, int waitTime) {
        this.id = id;
        this.name = name;
        this.arriveTime = arriveTime;
        this.treatTime = treatTime;
        this.priority = priority;
        this.waitTime = waitTime;
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

    public int getWaitTime() { return waitTime; }
    public void setWaitTime(int waitTime) { this.waitTime = waitTime; }
}
