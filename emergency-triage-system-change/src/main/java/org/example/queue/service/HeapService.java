package org.example.queue.service;

import org.example.queue.algorithm.CycleQueue;
import org.example.queue.algorithm.MaxHeap;
import org.example.queue.entity.Patient;
import org.example.queue.vo.PatientVO;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class HeapService {

    private final QueueService queueService;

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    public HeapService(QueueService queueService) {
        this.queueService = queueService;
    }

    //拿当前队列，按等待时间做大根堆排序
    public synchronized PatientVO[] heapSortByWaitTime() {
        //拿所有患者的List
        List<Patient> snapshot = queueService.getAllPatientsSnapshot();
        if(snapshot.isEmpty()) return new PatientVO[0];
        Patient[] arr = snapshot.toArray(new Patient[0]);
        //转成循环队列
        CycleQueue tmpQueue = new CycleQueue(arr.length);
        for(Patient p : arr) tmpQueue.insertPriority(p);
        //弄个循环队列数组进参数里
        MaxHeap maxHeap = new MaxHeap(tmpQueue);
        maxHeap.heapSort();  // 升序排序
        return toVOArray(maxHeap.getMaxHeap());
    }
    //把数组变成VO
    private PatientVO[] toVOArray(Patient[] patients) {
        PatientVO[] result = new PatientVO[patients.length];
        for(int i = 0; i < patients.length; i++) {
            result[i] = toVO(patients[i]);
        }
        return result;
    }

    private PatientVO toVO(Patient p) {
        PatientVO v = new PatientVO();
        v.setId(QueueService.formatId(p.getId()));  // 让id从int转回P什么什么的
        v.setName(p.getName());
        v.setArriveTime(p.getArriveTime().format(HH_MM));  //按格式返回VO里
        v.setTreatTime(p.getTreatTime());
        v.setPriority(p.getPriority());
        v.setTotalWaitTime(p.getTotalWaitTime());
        v.setCurrentWaitTime(p.getCurrentWaitTime());
        return v;
    }
}
