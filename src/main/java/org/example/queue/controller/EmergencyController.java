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
 * FIX: 8 个医生多队列调度 Controller
 * 原来的单队列 QueueService 已经改成 8 个 CycleQueue（每个医生一个）
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

    /** 加载 patients.txt 数据到内存（不直接入队） */
    @GetMapping("/load")
    public Result<List<QueueStatusVO>> load() throws IOException {
        queueService.loadFromFile();
        return Result.success(queueService.getStatus());
    }

    /** FIX: 查看所有 8 个医生队列的状态 */
    @GetMapping("/status")
    public Result<List<QueueStatusVO>> getStatus() {
        return Result.success(queueService.getStatus());
    }

    /** FIX: 查看指定医生的队列状态（index 0-7） */
    @GetMapping("/doctors/{index}")
    public Result<QueueStatusVO> getDoctorStatus(@PathVariable int index) {
        return Result.success(queueService.getDoctorStatus(index));
    }

    /** 从 txt 加载下一个患者，调度到当前最短的医生队列 */
    @PostMapping("/add")
    public Result<List<QueueStatusVO>> addOne() {
        return Result.success(queueService.addOneFromFile());
    }

    /** FIX: 队首患者就诊结束出队（指定医生编号，0-7） */
    @GetMapping("/treat")
    public Result<List<QueueStatusVO>> treatNext(@RequestParam(defaultValue = "0") int doctorIndex) {
        return Result.success(queueService.treatNext(doctorIndex));
    }

    /** FIX: 全局平均等待时间——所有医生队列中所有患者的 waitTime 总平均 */
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
