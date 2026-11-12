package org.example.queue.vo;

/**
 * 患者信息响应 VO，传给前端的信息
 */
public class PatientVO {
    private String id;
    private String name;
    private String arriveTime;
    private int treatTime;
    private int priority;
    private int totalWaitTime;   // 总体等待时间（到达 → 预计就诊）
    private int currentWaitTime; // 即时等待时间（当前还要等多久）

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

    public int getTotalWaitTime() { return totalWaitTime; }
    public void setTotalWaitTime(int totalWaitTime) { this.totalWaitTime = totalWaitTime; }

    public int getCurrentWaitTime() { return currentWaitTime; }
    public void setCurrentWaitTime(int currentWaitTime) { this.currentWaitTime = currentWaitTime; }
}
