package org.example.queue.vo;

import java.util.List;

/**
 * 单个医生队列的状态响应 VO
 */
public class QueueStatusVO {
    private int doctorIndex;      // 医生编号 0-7
    private String doctorName;    // 医生名称，如 "医生1"
    private int front;            // 循环队列队头指针
    private int rear;             // 循环队列队尾指针
    private int queueSize;        // 队列当前人数
    private int capacity;         // 队列总容量（= maxLength - 1，牺牲一个槽）
    private double avgWaitTime;   // 该医生队列自己的平均等待时间（分钟）
    private List<PatientVO> patientList; // 按排队顺序的患者列表

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
}
