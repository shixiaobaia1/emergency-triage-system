package org.example.queue.controller;

import org.example.queue.context.UserContext;
import org.example.queue.entity.Role;
import org.example.queue.entity.User;
import org.example.queue.service.HeapService;
import org.example.queue.service.QueueService;
import org.example.queue.vo.PatientVO;
import org.example.queue.vo.QueueStatusVO;
import org.example.queue.vo.Result;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 急诊分诊系统 Controller——RESTful 风格
 * FIX: 职责拆分后，分别注入 QueueService（第一阶段）和 HeapService（第二阶段）
 * FIX: 所有接口返回 Result<T> 统一响应包装，内部只返回 VO 不返回 Entity
 */
@RestController
@RequestMapping("/queue")
public class EmergencyController {

    private final QueueService queueService;
    private final HeapService heapService;

    // FIX: 构造器注入（Spring 推荐方式，比 @Resource 更清晰）
    public EmergencyController(QueueService queueService, HeapService heapService) {
        this.queueService = queueService;
        this.heapService = heapService;
    }

    // ========== 第一阶段：循环队列调度 ==========

    /** 加载 patients.txt 数据到内存（不直接入队） */
    @GetMapping("/load")
    public Result<QueueStatusVO> load() throws IOException {
        queueService.loadFromFile();
        return Result.success(queueService.getStatus());
    }

    /** 获取队列当前状态 */
    @GetMapping("/status")
    public Result<QueueStatusVO> getStatus() {
        return Result.success(queueService.getStatus());
    }

    // FIX: 清掉方法签名和 return 之间的多余空行，之前有 5 处（addOne/getAvgTime/treatNext/reset/heapSort）
    /** 从 txt 加载下一个患者入队（按优先级插入） */
    @PostMapping("/add")
    public Result<QueueStatusVO> addOne() {
        return Result.success(queueService.addOneFromFile());
    }

    /** 获取平均等待时间 */
    @GetMapping("/avg")
    public Result<Double> getAvgTime() {
        return Result.success(queueService.getAvgWaitTime());
    }

    /** 队首患者就诊结束，出队 */
    @GetMapping("/treat")
    public Result<QueueStatusVO> treatNext() {
        return Result.success(queueService.treatNext());
    }

    /** 重置队列（清空所有患者，重置文件读取指针） */
    // FIX: 医生不能调用 reset，只有 ADMIN 可以
    @GetMapping("/reset")
    public Result<QueueStatusVO> reset() {
        if(!isAdmin()) {
            return Result.fail(403, "医生角色无权执行 reset 操作");
        }
        return Result.success(queueService.reset());
    }

    /** 患者放弃排队 */
    // FIX: 医生不能调用 removePatient，只有 ADMIN 可以
    @DeleteMapping("/remove")
    public Result<QueueStatusVO> removePatient(@RequestParam int id) {
        if(!isAdmin()) {
            return Result.fail(403, "医生角色无权执行 remove 操作");
        }
        return Result.success(queueService.giveUp(id));
    }

    // ========== 第二阶段：大根堆排序 ==========

    /**
     * 第二阶段主功能：按等待时间做堆排序，返回有序患者数组
     * FIX: ID 转换链路——QueueService 里 Patient Entity 存 int id（从 "P001" 剥 P 解析），
     *      HeapService.toVO 里复用 QueueService.formatId() 转回 "P001" 格式 String 返回给前端
     */
    @GetMapping("/sort")
    public Result<PatientVO[]> heapSort() {
        return Result.success(heapService.heapSortByWaitTime());
    }

    // ===== 权限辅助方法 =====

    /** FIX: 判断当前登录用户是否是 ADMIN 角色 */
    private boolean isAdmin() {
        User user = UserContext.get();
        return user != null && user.getRole() == Role.ADMIN;
    }
}
