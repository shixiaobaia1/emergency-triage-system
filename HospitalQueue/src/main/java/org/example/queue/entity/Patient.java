package org.example.queue.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
public class Patient {
    private int id;                 //患者编号
    private String name;            //患者姓名
    private LocalTime arriveTime;   //患者到达时间
    private int treatTime;          //预估诊疗时长
    private int priority;           //优先级
    private int waitTime;           //等待时长

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
