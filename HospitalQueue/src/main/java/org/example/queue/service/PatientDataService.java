package org.example.queue.service;

import jakarta.annotation.PostConstruct;
import org.example.queue.entity.Patient;
import org.example.queue.structure.BinarySearch;
import org.example.queue.structure.HashTable;
import org.example.queue.vo.PatientVO;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.ToIntFunction;

/**
 * 第三阶段的数据服务：对上层 Controller 提供"有序序列"与 VO 转换。
 *
 * 序列来源由 PatientSequenceProvider 决定：
 *   - 队友工程里用 HeapServicePatientSequenceProvider（@Primary），
 *     直接取第二阶段 heapService.heapSortByWaitTime() 的真实堆排序结果；
 *   - 没有接入时用 TextFilePatientSequenceProvider 兜底（读 patients.txt 本地仿真）。
 */
@Service
public class PatientDataService {

    /** 关键字 → HashTable 的 Key 枚举 */
    public static HashTable.Key toHashKey(String keyType) {
        return "ID".equalsIgnoreCase(keyType) ? HashTable.Key.ID : HashTable.Key.WaitTime;
    }

    /** 关键字 → 折半查找的比较函数 */
    public static ToIntFunction<Patient> toBinaryKey(String keyType) {
        return "ID".equalsIgnoreCase(keyType) ? BinarySearch.KeyID : BinarySearch.KeyWaitTime;
    }

    private final PatientSequenceProvider provider;
    private final List<Patient> patients = new ArrayList<>();
    private final Map<Integer, Patient> patientsById = new LinkedHashMap<>();

    public PatientDataService(PatientSequenceProvider provider) {
        this.provider = provider;
    }

    @PostConstruct
    public void init() {
        reload();
    }

    /** 重新加载数据（医生数量变化后调用，让折半查找/哈希表拿到新的等待时间） */
    public void reload() {
        List<Patient> source = provider.sortedByWaitTime();
        patients.clear();
        patientsById.clear();
        for (Patient p : source) {
            patients.add(p);
            patientsById.put(p.getId(), p);
        }
        System.out.println("[PatientDataService] 已加载 " + patients.size() + " 条有序患者数据");
    }

    /** 全部患者（按等待时间升序） */
    public List<Patient> getPatients() {
        return new ArrayList<>(patients);
    }

    /** 按关键字升序（第三阶段的"有序序列"） */
    public List<Patient> getSortedBy(String keyType) {
        ToIntFunction<Patient> key = toBinaryKey(keyType);
        List<Patient> sorted = new ArrayList<>(patients);
        sorted.sort(Comparator.comparingInt(key));
        BinarySearch.validateSorted(sorted.toArray(new Patient[0]), key, keyType);
        return sorted;
    }

    public Optional<Patient> findById(int id) {
        return Optional.ofNullable(patientsById.get(id));
    }

    /** 实体的编号是 int，VO 的编号是 "P001" 展示格式 */
    public static PatientVO toVO(Patient p) {
        if (p == null) return null;
        PatientVO vo = new PatientVO();
        vo.setId(String.format("P%03d", p.getId()));
        vo.setName(p.getName());
        vo.setArriveTime(p.getArriveTime() == null ? "" : p.getArriveTime().toString());
        vo.setTreatTime(p.getTreatTime());
        vo.setPriority(p.getPriority());
        vo.setWaitTime(p.getWaitTime());
        return vo;
    }

    public static List<PatientVO> toVO(List<Patient> list) {
        List<PatientVO> out = new ArrayList<>(list.size());
        for (Patient p : list) out.add(toVO(p));
        return out;
    }
}
