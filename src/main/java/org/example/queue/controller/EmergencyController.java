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
import java.util.List;

/**
 * FIX: 多医生多队列调度 Controller
 * 医生人数不固定，由 /queue/load?doctorCount=N 传入，每个医生一个独立的 CycleQueue
 */
@RestController
@RequestMapping("/queue")
public class EmergencyController {

    private final QueueService queueService;
    private final HeapService heapService;

    public EmergencyController(QueueService queueService, HeapService heapService) {
        this.queueService = queueService;
        this.heapService = heapService;
    }

    /**
     * FIX: 加载 patients.txt，医生人数由前端传入（默认 8）
     * 加载后模拟时钟复位到 08:00 并自动开始（5 秒 = 1 分钟）
     */
    @GetMapping("/load")
    public Result<List<QueueStatusVO>> load(@RequestParam(defaultValue = "8") int doctorCount) throws IOException {
        queueService.loadFromFile(doctorCount);
        return Result.success(queueService.getStatus());
    }

    /** FIX: 查看所有医生队列的状态（数量由最近一次 load 的 doctorCount 决定） */
    @GetMapping("/status")
    public Result<List<QueueStatusVO>> getStatus() {
        return Result.success(queueService.getStatus());
    }

    /** FIX: 查看指定医生的队列状态（index 从 0 到 医生人数-1） */
    @GetMapping("/doctors/{index}")
    public Result<QueueStatusVO> getDoctorStatus(@PathVariable int index) {
        return Result.success(queueService.getDoctorStatus(index));
    }

    /** FIX: 手动推进一分钟（调试用，正常由 5 秒一跳的计时器自动推进） */
    @PostMapping("/add")
    public Result<List<QueueStatusVO>> addOne() {
        return Result.success(queueService.addOneFromFile());
    }

    /** FIX: 手动让指定医生队首患者立即完成治疗（调试用，正常由计时器自动完成） */
    @GetMapping("/treat")
    public Result<List<QueueStatusVO>> treatNext(@RequestParam(defaultValue = "0") int doctorIndex) {
        return Result.success(queueService.completeHead(doctorIndex));
    }

    /** FIX: 查看当前模拟时间（从 08:00 开始，每 5 秒跳 1 分钟） */
    @GetMapping("/now")
    public Result<String> now() {
        return Result.success(queueService.getSimTime());
    }

    /** FIX: 全局平均等待时间——所有仍在排队患者的"即时等待时间"总平均 */
    @GetMapping("/avg")
    public Result<Double> getAvgTime() {
        return Result.success(queueService.getAvgWaitTime());
    }

    /** 重置所有医生队列（只有 ADMIN 可以） */
    @GetMapping("/reset")
    public Result<List<QueueStatusVO>> reset() {
        if (!isAdmin()) {
            return Result.fail(403, "医生角色无权执行 reset 操作");
        }
        return Result.success(queueService.reset());
    }

    /** 患者放弃排队（只有 ADMIN 可以） */
    @DeleteMapping("/remove")
    public Result<List<QueueStatusVO>> removePatient(@RequestParam int id) {
        if (!isAdmin()) {
            return Result.fail(403, "医生角色无权执行 remove 操作");
        }
        return Result.success(queueService.giveUp(id));
    }

    // ========== 第二阶段：大根堆排序 ==========

    /** FIX: 堆排序基于所有医生队列合起来的全部患者 */
    @GetMapping("/sort")
    public Result<PatientVO[]> heapSort() {
        return Result.success(heapService.heapSortByWaitTime());
    }

    // ===== 权限辅助方法 =====

    private boolean isAdmin() {
        User user = UserContext.get();
        return user != null && user.getRole() == Role.ADMIN;
    }
}
