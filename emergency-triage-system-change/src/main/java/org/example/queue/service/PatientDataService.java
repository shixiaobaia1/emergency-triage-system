package org.example.queue.service;

import org.example.queue.entity.Patient;
import org.example.queue.structure.BinarySearch;
import org.example.queue.structure.HashTable;
import org.example.queue.vo.PatientVO;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.ToIntFunction;

/**
 * 对上层 Controller 提供有序序列与 VO 转换。
 *每次实时取数，医生队列变化后，等待时间会自动联动，保证折半查找 / 哈希表拿到的是与前端一致的最新数据。
 */
@Service
public class PatientDataService {

    //哈希表转换；要id还是wiattime
    public static HashTable.Key toHashKey(String keyType) {
        return "ID".equalsIgnoreCase(keyType) ? HashTable.Key.ID : HashTable.Key.WaitTime;
    }

    //折半转换；要id还是wiattime
    public static ToIntFunction<Patient> toBinaryKey(String keyType) {
        return "ID".equalsIgnoreCase(keyType) ? BinarySearch.KeyID : BinarySearch.KeyWaitTime;
    }

    //依赖注入
    private final PatientSequenceProvider provider;

    public PatientDataService(PatientSequenceProvider provider) {
        this.provider = provider;
    }

    /** 按关键字升序获取患者列表 */
    public List<Patient> getSortedBy(String keyType) {
        ToIntFunction<Patient> key = toBinaryKey(keyType);
        List<Patient> sorted = new ArrayList<>(provider.sortedByWaitTime());
        sorted.sort(Comparator.comparingInt(key));
        BinarySearch.validateSorted(sorted.toArray(new Patient[0]), key, keyType);
        return sorted;
    }

    //根据 ID 查找患者
    public Optional<Patient> findById(int id) {
        return provider.sortedByWaitTime().stream()
                .filter(p -> p.getId() == id)
                .findFirst();
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
        vo.setTotalWaitTime(p.getTotalWaitTime());
        vo.setCurrentWaitTime(p.getCurrentWaitTime());
        return vo;
    }

    public static List<PatientVO> toVO(List<Patient> list) {
        List<PatientVO> out = new ArrayList<>(list.size());
        for (Patient p : list) out.add(toVO(p));
        return out;
    }
}