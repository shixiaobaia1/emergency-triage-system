package org.example.queue.service;

import org.example.queue.entity.Patient;

import java.util.List;

//接口：患者队列数据从哪里来
public interface PatientSequenceProvider {

    /**
     * 返回按等待时间升序排列的患者序列，即第二阶段大根堆排序的输出。
     *
     * @return 升序序列；数据尚未加载时返回空列表
     */
    List<Patient> sortedByWaitTime();
}