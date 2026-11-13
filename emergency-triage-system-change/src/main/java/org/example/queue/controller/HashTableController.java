package org.example.queue.controller;

import org.example.queue.controller.dto.*;
import org.example.queue.entity.Patient;
import org.example.queue.service.PatientDataService;
import org.example.queue.structure.HashTable;
import org.example.queue.structure.SearchResult;
import org.example.queue.vo.Result;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.*;

/**
 * 第三阶段（二）：哈希表接口（除留余数法 + 线性探测）。
 * POST /hash/build    {"tableLength":101,"keyType":"WAIT_TIME"}
 * POST /hash/search   {"key":264,"keyType":"WAIT_TIME","tableLength":101}
 * GET  /hash/experiment?keyType=WAIT_TIME
 */
@RestController
@RequestMapping("/hash")
public class HashTableController {

    private final PatientDataService patientService;

    public HashTableController(PatientDataService patientService) {
        this.patientService = patientService;
    }

    /** 构建哈希表：按关键字升序插入，返回完整存储结构与统计 */
    @PostMapping("/build")
    public Result<HashTableResponse> build(@RequestBody HashTableBuildRequest req) {
        try {
            HashTable ht = buildTable(req.getTableLength(), req.getKeyType());
            return Result.success(buildResponse(ht, null));
        } catch (IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /** 哈希查找，同时返回完整存储结构（命中的槽位由前端高亮） */
    @PostMapping("/search")
    public Result<HashTableResponse> search(@RequestBody HashTableSearchRequest req) {
        try {
            HashTable ht = buildTable(req.getTableLength(), req.getKeyType());
            Integer searchKey = req.getKey();
            SearchResult<Patient> hit = ht.search(searchKey == null ? 0 : searchKey);
            return Result.success(buildResponse(ht, hit));
        } catch (IllegalStateException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /**
     * 装填因子实验：固定表长 163（质数），改变元素数得到 α=0.10~0.95 的实测点。
     * 实验要求关键字互异，因此该实验单独生成互异关键字，与真实患者数据无关。
     */
    @GetMapping("/experiment")
    public Result<LoadFactorExperimentResponse> experiment(
            @RequestParam(defaultValue = "WAIT_TIME") String keyType) {
        final int M = 163;
        HashTable.Key k = PatientDataService.toHashKey(keyType);
        List<Patient> pool = distinctPatients(155, M * 3);

        int[] ns = {16, 32, 48, 64, 80, 96, 112, 128, 144, 155};
        List<LoadFactorExperimentResponse.Row> rows = new ArrayList<>();
        for (int n : ns) {
            HashTable ht = new HashTable(M, k);
            for (int i = 0; i < n; i++) {
                ht.insert(pool.get(i));
            }
            double alpha = ht.loadFactor();
            double tSuccess = 0.5 * (1 + 1 / (1 - alpha));
            double tFailure = 0.5 * (1 + 1 / ((1 - alpha) * (1 - alpha)));
            rows.add(new LoadFactorExperimentResponse.Row(
                    n, alpha, ht.aslSuc(), tSuccess,
                    ht.aslFail(), tFailure,
                    ht.getCollisionCount(), ht.maxProbeLength()));
        }
        return Result.success(new LoadFactorExperimentResponse(M, M, rows));
    }


    private HashTable buildTable(Integer tableLength, String keyType) {
        int m = tableLength == null ? 101 : tableLength;
        HashTable.Key key = PatientDataService.toHashKey(keyType);
        HashTable ht = new HashTable(m, key);
        // 按关键字升序插入，与"基于第二阶段堆排序序列建表"一致
        for (Patient p : patientService.getSortedBy(keyType)) {
            ht.insert(p);
        }
        return ht;
    }

    private HashTableResponse buildResponse(HashTable ht, SearchResult<Patient> hit) {
        HashTableResponse.Summary summary = new HashTableResponse.Summary(
                ht.getM(), ht.getP(), ht.size(), ht.loadFactor(),
                ht.aslSuc(), ht.aslFail(),
                ht.getCollisionCount(), ht.maxProbeLength());

        List<HashTableResponse.SlotEntry> slots = new ArrayList<>(ht.getM());
        for (HashTable.SlotSnapshot s : ht.getSlotEntries()) {
            slots.add(new HashTableResponse.SlotEntry(
                    s.slot, s.key, s.probe, PatientDataService.toVO(s.patient)));
        }

        HashTableResponse.SearchResult sr = null;
        if (hit != null) {
            sr = new HashTableResponse.SearchResult(hit.isFound(), hit.getCount(),
                    hit.getIndex(), PatientDataService.toVO(hit.getData()));
        }
        return new HashTableResponse(summary, slots, sr);
    }

    /** 生成 count 个关键字互异的患者（固定种子，结果可复现） */
    private static List<Patient> distinctPatients(int count, int bound) {
        Random random = new Random(2026L);
        Set<Integer> waits = new LinkedHashSet<>();
        while (waits.size() < count) {
            waits.add(random.nextInt(bound));
        }
        List<Patient> list = new ArrayList<>(count);
        LocalTime start = LocalTime.of(8, 0);
        int i = 0;
        for (int w : waits) {
            int id = i + 1;
            Patient p = new Patient(id, "实验患者" + String.format("%03d", id),
                    start.plusMinutes(i * 2L), 10, 1);
            p.setTotalWaitTime(w);
            list.add(p);
            i++;
        }
        return list;
    }
}