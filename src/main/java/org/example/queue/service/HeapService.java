package org.example.queue.service;

import org.example.queue.algorithm.CycleQueue;
import org.example.queue.algorithm.MaxHeap;
import org.example.queue.entity.Patient;
import org.example.queue.vo.PatientVO;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 第二阶段核心服务：大根堆构建 + 堆排序
 * FIX: 从 EmergencyService 拆分出来，职责单一化
 *  FIX: 依赖 QueueService 获取当前队列患者的快照（避免跨 Service 持锁）
 */
@Service
public class HeapService {

    private final QueueService queueService;

    // FIX: DateTimeFormatter 缓存，避免每次 toVO 都 new 一个
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    // FIX: 构造器注入（比 @Autowired 更推荐，Spring 官方也推荐）
    public HeapService(QueueService queueService) {
        this.queueService = queueService;
    }

    // FIX: 第二阶段主功能——从队列取患者快照，按等待时间做大根堆排序
    public synchronized PatientVO[] heapSortByWaitTime() {
        List<Patient> snapshot = queueService.getAllPatientsSnapshot();
        if(snapshot.isEmpty()) return new PatientVO[0];

        // FIX: snapshot 是 List，需要转数组给 MaxHeap
        Patient[] arr = snapshot.toArray(new Patient[0]);
        // FIX: MaxHeap 构造器要求 CycleQueue，所以先手动把 snapshot 排成 CycleQueue
        //  这里直接 new 一个临时的 CycleQueue 把 snapshot 塞进去
        CycleQueue tmpQueue = new CycleQueue(arr.length);
        for(Patient p : arr) tmpQueue.insertPriority(p);

        MaxHeap maxHeap = new MaxHeap(tmpQueue);
        maxHeap.heapSort();  // 升序排序
        return toVOArray(maxHeap.getMaxHeap());
    }

    // ===== 私有工具 =====

    // FIX: Patient[] → PatientVO[]（第二阶段堆排序结果需要返回 VO 给 Controller）
    private PatientVO[] toVOArray(Patient[] patients) {
        PatientVO[] result = new PatientVO[patients.length];
        for(int i = 0; i < patients.length; i++) {
            result[i] = toVO(patients[i]);
        }
        return result;
    }

    // FIX: Patient Entity → PatientVO（Entity int id → VO "P001" 格式 String，和 QueueService 统一）
    private PatientVO toVO(Patient p) {
        PatientVO v = new PatientVO();
        v.setId(QueueService.formatId(p.getId()));  // FIX: 1 → "P001"，复用 QueueService 的格式化逻辑
        v.setName(p.getName());
        v.setArriveTime(p.getArriveTime().format(HH_MM));  // FIX: DateTimeFormatter 缓存
        v.setTreatTime(p.getTreatTime());
        v.setPriority(p.getPriority());
        v.setWaitTime(p.getWaitTime());
        return v;
    }
}
