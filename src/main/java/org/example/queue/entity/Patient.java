package org.example.queue.entity;

import java.time.LocalTime;

public class Patient {
    private int id;
    private String name;
    private LocalTime arriveTime;
    private int treatTime;  // FIX: proveTime语义错误，改为treatTime（治疗耗时）
    private int priority;
    private int waitTime;

    public Patient(int id, String name, LocalTime arriveTime, int treatTime, int priority, int waitTime) {
        this.id = id;
        this.name = name;
        this.arriveTime = arriveTime;
        this.treatTime = treatTime;
        this.priority = priority;
        this.waitTime = waitTime;
    }

    public Patient() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalTime getArriveTime() {
        return arriveTime;
    }

    public void setArriveTime(LocalTime arriveTime) {
        this.arriveTime = arriveTime;
    }

    public int getTreatTime() {
        return treatTime;
    }

    public void setTreatTime(int treatTime) {
        this.treatTime = treatTime;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }
}
