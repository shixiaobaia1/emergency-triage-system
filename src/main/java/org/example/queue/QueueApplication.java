package org.example.queue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 急诊排队系统 Spring Boot 启动类
 * 项目分为三个阶段：
 *   第一阶段：循环队列（CycleQueue）按到达时间排队、计算等待时间
 *   第二阶段：大根堆（MaxHeap）排序，按总体等待时间排序
 *   扩展功能：多医生并行队列 + 全局等待池动态调度 + 模拟时钟（5 秒 = 1 分钟）
 *            登录鉴权（TokenUtil + AuthInterceptor），ADMIN / DOCTOR 两种角色
 */
@SpringBootApplication
@EnableScheduling  // FIX: 开启定时任务，驱动模拟时钟
public class QueueApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueueApplication.class, args);
    }

}
