package org.example.queue.vo;

/**
 * 患者信息响应 VO（替代直接返回 Patient Entity）
 * FIX: 去掉 Lombok，改回手写 getter/setter
 */
public class PatientVO {
    private String id;
    private String name;
    private String arriveTime;
    private int treatTime;
    private int priority;
    private int waitTime;

    public PatientVO() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getArriveTime() { return arriveTime; }
    public void setArriveTime(String arriveTime) { this.arriveTime = arriveTime; }

    public int getTreatTime() { return treatTime; }
    public void setTreatTime(int treatTime) { this.treatTime = treatTime; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public int getWaitTime() { return waitTime; }
    public void setWaitTime(int waitTime) { this.waitTime = waitTime; }
}
