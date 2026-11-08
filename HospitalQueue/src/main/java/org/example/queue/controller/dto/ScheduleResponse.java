package org.example.queue.controller.dto;

import lombok.Data;
import org.example.queue.vo.PatientVO;

import java.util.List;

/** 多医生调度结果 */
@Data
public class ScheduleResponse {

    private int doctorCount;          // 医生数量
    private int patientCount;         // 患者总数
    private double avgWaitTime;       // 平均等待时间（分钟）
    private int maxWaitTime;          // 最长等待时间（分钟）
    private List<DoctorSchedule> doctors;   // 每个医生的接诊名单

    /** 单个医生的接诊情况 */
    @Data
    public static class DoctorSchedule {
        private int doctorId;                // 医生编号（1~N）
        private int handledCount;            // 该医生接诊人数
        private List<PatientVO> patients;    // 接诊的患者（已含等待时间，按接诊顺序）
    }
}