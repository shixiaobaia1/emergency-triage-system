package org.example.queue.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * 患者实体类
 * FIX: 补回 Lombok @Data/@AllArgsConstructor/@NoArgsConstructor——之前丢失导致手写了 72 行 getter/setter
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Patient {
    private int id;           // FIX: Entity 内部存 int（从 txt 的 "P001" 剥掉 P 前缀解析而来）
    private String name;
    private LocalTime arriveTime;
    private int treatTime;    // FIX: 原 proveTime 语义错误（prove=证明），改为 treatTime（治疗耗时）
    private int priority;
    private int waitTime;
}
