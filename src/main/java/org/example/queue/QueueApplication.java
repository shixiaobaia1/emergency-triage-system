package org.example.queue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 急诊排队系统 Spring Boot 启动类
 * 项目分为三个阶段：
 *   第一阶段：循环队列（CycleQueue）按到达时间排队、计算等待时间
 *   第二阶段：大根堆（MaxHeap）排序，按等待时间从长到短排
 *   扩展功能：8 个医生并行队列 + 全局优先等待池（PriorityQueue）动态调度
 *            登录鉴权（TokenUtil + AuthInterceptor），ADMIN / DOCTOR 两种角色
 */
@SpringBootApplication
public class QueueApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueueApplication.class, args);
    }

}
