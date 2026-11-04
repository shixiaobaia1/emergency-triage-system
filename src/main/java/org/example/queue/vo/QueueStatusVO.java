package org.example.queue.vo;


import java.util.List;

/**
    队列状态响应 VO
 */

public class QueueStatusVO {
    private int front;          // 队头指针
    private int rear;           // 队尾指针
    private int queueSize;      // 队列当前人数
    private int capacity;       // 队列总容量
    private double avgWaitTime; // 平均等待时间（分钟）
    private List<PatientVO> patientList; // 按排队顺序的患者列表

    public int getFront() {
        return front;
    }

    public void setFront(int front) {
        this.front = front;
    }

    public int getRear() {
        return rear;
    }

    public void setRear(int rear) {
        this.rear = rear;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public double getAvgWaitTime() {
        return avgWaitTime;
    }

    public void setAvgWaitTime(double avgWaitTime) {
        this.avgWaitTime = avgWaitTime;
    }

    public List<PatientVO> getPatientList() {
        return patientList;
    }

    public void setPatientList(List<PatientVO> patientList) {
        this.patientList = patientList;
    }
}
