package org.example.queue.algorithm;

import org.example.queue.entity.QueueMessage;
import org.springframework.core.io.Resource;
import org.example.queue.entity.Patient;
import org.springframework.beans.factory.annotation.Value;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class EmergencyScheduler {
    private CycleQueue queue;
    //获取resource文件下的patient.txt文件
    @Value("classpath:patient.txt")
    private Resource resource;
    //txt加载进来，把所有患者信息加入队列
    public void loadFromFile(String filePath) throws IOException {
        //加载txt
        List<String> lines;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            lines = br.lines()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        //进队列
        queue = new CycleQueue(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            String[] strings = lines.get(i).split("\\s+");
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");

            Patient patient = new Patient(Integer.parseInt(strings[0]),null,
                    LocalTime.parse(strings[2]),Integer.parseInt(strings[4]),
                            Integer.parseInt(strings[6]),0);
            //错误处理
            if(!queue.insertPriority(patient)){
                CycleQueue tmp = queue;
                queue = new CycleQueue(tmp.getMaxLength()+10);
                queue.setFront(tmp.getFront());
                queue.setRear(tmp.getRear());
                queue.setLength(tmp.getLength());
                queue.setPatients(tmp.getPatients());
                queue.insertPriority(patient);
            }
        }

    }
    public void timeStep() {
        int waitTime = 0;
        int id = queue.getFront();
        while (id!=queue.getRear()) {
            queue.getPatients()[id].setWaitTime(waitTime);
            waitTime +=queue.getPatients()[id].getProveTime();
            id = (id + 1) % queue.getMaxLength();
        }
    }

    public Patient treatNext(){
        Patient patient = queue.deQueue();
        timeStep();
        return patient;
    }

    //拿平均等待时间
    public double getAvgWaitTime(){
        double avgWaitTime = 0;
        for (int i = 0; i < queue.getLength(); i++) {
            avgWaitTime+=queue.getPatients()[i].getWaitTime();
        }
        return avgWaitTime/queue.getLength();
    }
    public QueueMessage getQueueStatus(){
        QueueMessage queueMessage = new QueueMessage();
        queueMessage.setFront(queue.getFront());
        queueMessage.setRear(queue.getRear());
        queueMessage.setAvgTime(getAvgWaitTime());
        return queueMessage;
    }
    public void reset(){
        queue.clear();
    }
}
