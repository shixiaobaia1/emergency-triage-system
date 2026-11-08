package org.example.queue.service;

import org.example.queue.entity.Patient;

import java.util.List;

/**
 * 第三阶段的数据来源抽象。
 *
 * 第三阶段的两个结构（折半查找、哈希表）都以"第二阶段堆排序输出的有序序列"为输入，
 * 因此这里把序列来源抽象成接口：
 *   - 默认实现 TextFilePatientSequenceProvider：读 patients.txt 并本地仿真等待时间，
 *     保证第三阶段单独也能跑起来（不依赖前两阶段）；
 *   - 队友工程里用 HeapServicePatientSequenceProvider：直接调用第二阶段的
 *     heapService.heapSortByWaitTime()，拿到真实堆排序结果（推荐，见 integration 目录）。
 */
public interface PatientSequenceProvider {

    /**
     * 返回按等待时间升序排列的患者序列，即第二阶段大根堆排序的输出。
     *
     * @return 升序序列；数据尚未加载时返回空列表
     */
    List<Patient> sortedByWaitTime();
}
