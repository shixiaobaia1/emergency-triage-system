package org.example.queue.controller.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

/** 装填因子实验响应 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoadFactorExperimentResponse {

    private int tableLength;
    private int primeDivisor;
    private List<Row> rows;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Row {
        private int n;                    // 插入元素数
        private double alpha;             // 装填因子
        private double aslSuccess;        // 实测
        private double theorySuccess;     // 线性探测理论值
        private double aslFailure;        // 实测
        private double theoryFailure;     // 线性探测理论值
        private int collisionCount;
        private int maxProbeLength;
    }
}