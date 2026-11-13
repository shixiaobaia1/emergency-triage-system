package org.example.queue.controller;

import org.example.queue.controller.dto.*;
import org.example.queue.entity.Patient;
import org.example.queue.service.PatientDataService;
import org.example.queue.structure.BinarySearch;
import org.example.queue.structure.SearchResult;
import org.example.queue.vo.PatientVO;
import org.example.queue.vo.Result;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * 折半查找接口。
 * GET  /binary/patients?keyType=ID
 * GET  /binary/stats?keyType=ID
 * POST /binary/search   {"key":40,"keyType":"ID"}
 */
@RestController
@RequestMapping("/binary")
public class BinarySearchController {

    private final PatientDataService patientService;

    public BinarySearchController(PatientDataService patientService) {
        this.patientService = patientService;
    }

    /** 按指定关键字升序的患者列表 */
    @GetMapping("/patients")
    public Result<List<PatientVO>> list(@RequestParam(defaultValue = "ID") String keyType) {
        return Result.success(PatientDataService.toVO(patientService.getSortedBy(keyType)));
    }

    /** 判定树统计：树高 + 各层结点数 + 成功/失败 ASL */
    @GetMapping("/stats")
    public Result<BinarySearchResponse> stats(
            @RequestParam(defaultValue = "ID") String keyType) {
        List<Patient> sorted = patientService.getSortedBy(keyType);
        BinarySearch.DecisionTreeStats dt = BinarySearch.decisionTreeStats(sorted.size());

        BinarySearchResponse resp = new BinarySearchResponse();
        resp.setAslSuccess(dt.aslSuc);
        resp.setAslFailure(dt.aslFail);
        resp.setTreeHeight(dt.treeHeight);
        resp.setLevelCounts(dt.levelCounts);
        resp.setSortedByKey(PatientDataService.toVO(sorted));
        resp.setTree(BinarySearch.decisionTree(sorted.toArray(new Patient[0]),
                PatientDataService.toBinaryKey(keyType)));
        return Result.success(resp);
    }

    /** 单次折半查找：返回命中位置、比较次数、比较轨迹 + 判定树统计 */
    @PostMapping("/search")
    public Result<BinarySearchResponse> search(@RequestBody BinarySearchRequest req) {
        int key = req.getKey() == null ? 0 : req.getKey();
        ToIntFunction<Patient> keyFunc = PatientDataService.toBinaryKey(req.getKeyType());

        List<Patient> sorted = patientService.getSortedBy(req.getKeyType());
        Patient[] arr = sorted.toArray(new Patient[0]);
        BinarySearch.DecisionTreeStats dt = BinarySearch.decisionTreeStats(arr.length);

        List<String> trace = new ArrayList<>();
        SearchResult<Patient> hit = searchWithTrace(arr, key, keyFunc, trace);

        BinarySearchResponse.SearchResult sr = new BinarySearchResponse.SearchResult(
                hit.isFound(), hit.getIndex(), hit.getCount(),
                hit.getData() == null ? null : PatientDataService.toVO(hit.getData()));

        BinarySearchResponse resp = new BinarySearchResponse();
        resp.setSearchResult(sr);
        resp.setAslSuccess(dt.aslSuc);
        resp.setAslFailure(dt.aslFail);
        resp.setTreeHeight(dt.treeHeight);
        resp.setLevelCounts(dt.levelCounts);
        resp.setTrace(trace);
        resp.setSortedByKey(PatientDataService.toVO(sorted));
        resp.setTree(BinarySearch.decisionTree(arr, keyFunc));
        return Result.success(resp);
    }

    // 带轨迹的折半查找

    private SearchResult<Patient> searchWithTrace(Patient[] arr, int key,
                                                  ToIntFunction<Patient> keyFunc,
                                                  List<String> trace) {
        int low = 0, high = arr.length - 1, compares = 0;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midKey = keyFunc.applyAsInt(arr[mid]);
            compares++;
            trace.add(String.format("第%d次比较: [low=%d,high=%d] mid=%d, key=%d",
                    compares, low, high, mid, midKey));
            if (midKey == key) {
                return new SearchResult<>(mid, compares, arr[mid]);
            } else if (midKey < key) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return SearchResult.miss(compares);
    }
}