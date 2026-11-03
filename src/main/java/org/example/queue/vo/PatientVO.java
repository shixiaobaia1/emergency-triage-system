package org.example.queue.vo;

import lombok.Data;

/**
 * 患者信息响应 VO（替代直接返回 Patient Entity）
 * FIX: 新增 VO 分层，Controller 不再直接返回 Entity，避免内部字段泄露
 */
@Data
public class PatientVO {
    private String id;            // FIX: String 类型编号，如 P001、P002——和 txt 文件格式一致
    private String name;          // 姓名（暂未用，后续可扩展）
    private String arriveTime;    // 到达时间，格式化 HH:mm（FIX: Entity 是 LocalTime，VO 转 String 方便前端展示）
    private int treatTime;        // 治疗耗时（分钟）
    private int priority;         // 优先级（1最紧急）
    private int waitTime;         // 当前等待时间（分钟）
}
