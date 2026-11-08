package org.example.queue.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.queue.vo.PatientVO;

import java.util.List;
import java.util.Map;

/**
 * 折半查找完整响应。
 * 说明：外层统一用队友的 Result 包装，这里只承载业务数据。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BinarySearchResponse {

    private SearchResult searchResult;
    private double aslSuccess;
    private double aslFailure;
    private int treeHeight;
    private Map<Integer, Integer> levelCounts;
    private List<String> trace;
    private List<PatientVO> sortedByKey;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SearchResult {
        private boolean found;
        private int index;
        private int compares;
        private PatientVO target;
    }
}
