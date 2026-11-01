package org.example.queue.controller;

import jakarta.annotation.Resource;
import org.example.queue.entity.Patient;
import org.example.queue.entity.QueueMessage;
import org.example.queue.service.EmergencyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/queue")
public class EmergencyController {
    @Resource
    private EmergencyService emergencyService;

    //加载txt
    @GetMapping("/load")
    public QueueMessage load() throws IOException {
        emergencyService.loadFromFile("patients.txt");
        return emergencyService.getQueueStatus();
    }
    //拿信息
    @GetMapping("/status")
    public QueueMessage getStatus() throws IOException {
        return emergencyService.getQueueStatus();
    }
    //加人进队列
    @PostMapping("/add")
    public QueueMessage addPatients(){
        emergencyService.addQueue();
        return emergencyService.getQueueStatus();
    }
    //拿平均值
    @GetMapping("/avg")
    public double getAvgTime() throws IOException {
        return emergencyService.getAvgWaitTime();
    }

    //治疗结束
    @GetMapping("/treat")
    public QueueMessage treatPatients(){
        emergencyService.treatNext();
        return emergencyService.getQueueStatus();
    }
    //大根堆排序
    @GetMapping("/sort")
    public Patient[] sort(){
        return emergencyService.heapSort();
    }
    //重置
    @GetMapping("/reset")
    public QueueMessage reset(){
        emergencyService.reset();
        return emergencyService.getQueueStatus();
    }

}
