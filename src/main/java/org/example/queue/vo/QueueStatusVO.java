package org.example.queue.vo;

import java.util.List;

/**
 * 单个医生队列的状态响应 VO
 * FIX: 去掉 Lombok，改回手写构造器 + getter/setter
 * FIX: 新增 doctorIndex / doctorName 标识这是哪个医生的队列
 */
public class QueueStatusVO {
    private int doctorIndex;
    private String doctorName;
    private int front;
    private int rear;
    private int queueSize;
    private int capacity;
    private double avgWaitTime;
    private List<PatientVO> patientList;

    public QueueStatusVO() {}

    public QueueStatusVO(int doctorIndex, String doctorName, int front, int rear,
                         int queueSize, int capacity, double avgWaitTime, List<PatientVO> patientList) {
        this.doctorIndex = doctorIndex;
        this.doctorName = doctorName;
        this.front = front;
        this.rear = rear;
        this.queueSize = queueSize;
        this.capacity = capacity;
        this.avgWaitTime = avgWaitTime;
        this.patientList = patientList;
    }

    public int getDoctorIndex() { return doctorIndex; }
    public void setDoctorIndex(int doctorIndex) { this.doctorIndex = doctorIndex; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public int getFront() { return front; }
    public void setFront(int front) { this.front = front; }

    public int getRear() { return rear; }
    public void setRear(int rear) { this.rear = rear; }

    public int getQueueSize() { return queueSize; }
    public void setQueueSize(int queueSize) { this.queueSize = queueSize; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public double getAvgWaitTime() { return avgWaitTime; }
    public void setAvgWaitTime(double avgWaitTime) { this.avgWaitTime = avgWaitTime; }

    public List<PatientVO> getPatientList() { return patientList; }
    public void setPatientList(List<PatientVO> patientList) { this.patientList = patientList; }
