package org.example.queue.vo;

/**
 * 患者信息响应 VO（替代直接返回 Patient Entity）
 * FIX: 新增 VO 分层，Controller 不再直接返回 Entity，避免内部字段泄露
 */

public class PatientVO {
    private int id;            // 患者编号
    private String name;       // 姓名（暂未用，后续可扩展）
    private String arriveTime; // 到达时间，格式化 HH:mm（FIX: Entity 是 LocalTime，VO 转 String 方便前端展示）
    private int treatTime;     // 治疗耗时（分钟）
    private int priority;      // 优先级（1最紧急）
    private int waitTime;      // 当前等待时间（分钟）

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

    public String getArriveTime() {
        return arriveTime;
    }

    public void setArriveTime(String arriveTime) {
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
