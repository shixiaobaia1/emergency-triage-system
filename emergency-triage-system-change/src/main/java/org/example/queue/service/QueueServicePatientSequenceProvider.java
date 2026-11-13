package org.example.queue.service;

import org.example.queue.entity.Patient;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 第三阶段数据源：直接复用第二阶段的 QueueService 队列。
 * waitTime 由 QueueService 的 timeStep 实时计算（队首=0 向后累加 treatTime），
 * 返回按等待时间升序的患者序列，等价于第二阶段堆排序的输出。
 */
@Component
public class QueueServicePatientSequenceProvider implements PatientSequenceProvider {

    private final QueueService queueService;

    public QueueServicePatientSequenceProvider(QueueService queueService) {
        this.queueService = queueService;
    }

    @Override
    public List<Patient> sortedByWaitTime() {
        // getAllPatientsSnapshot 返回的是新列表，排序不会影响队列内部状态
        List<Patient> snapshot = queueService.getAllPatientsSnapshot();
        snapshot.sort(Comparator.comparingInt(Patient::getTotalWaitTime));
        return snapshot;
    }
}