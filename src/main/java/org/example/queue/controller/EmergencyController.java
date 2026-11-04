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

//循环队列，大根堆排序逻辑
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
    public Result<QueueStatusVO> load() throws IOException {
        queueService.loadFromFile();
        return Result.success(queueService.getStatus());
    }

    /** 获取队列当前状态 */
    @GetMapping("/status")
    public Result<QueueStatusVO> getStatus() {
        return Result.success(queueService.getStatus());
    }


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

    /** 重置队列（清空所有患者，重置文件读取指针）
     医生不能调用 reset，只有 ADMIN 可以*/
    @GetMapping("/reset")
    public Result<QueueStatusVO> reset() {
        if(!isAdmin()) {
            return Result.fail(403, "医生角色无权执行 reset 操作");
        }
        return Result.success(queueService.reset());
    }

    /** 患者放弃排队
     医生不能调用 removePatient，只有 ADMIN 可以*/
    @DeleteMapping("/remove")
    public Result<QueueStatusVO> removePatient(@RequestParam int id) {
        if(!isAdmin()) {
            return Result.fail(403, "医生角色无权执行 remove 操作");
        }
        return Result.success(queueService.giveUp(id));
    }

    // ========== 第二阶段：大根堆排序 ==========

    //做堆排序
    @GetMapping("/sort")
    public Result<PatientVO[]> heapSort() {
        return Result.success(heapService.heapSortByWaitTime());
    }

    // ===== 权限辅助方法 =====

    /**判断当前登录用户是否是 ADMIN 角色 */
    private boolean isAdmin() {
        User user = UserContext.get();
        return user != null && user.getRole() == Role.ADMIN;
    }
}
