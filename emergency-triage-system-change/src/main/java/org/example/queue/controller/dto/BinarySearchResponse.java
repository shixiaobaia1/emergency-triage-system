package org.example.queue.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.queue.structure.BinarySearch;
import org.example.queue.vo.PatientVO;

import java.util.List;
import java.util.Map;

/**
 * 折半查找响应
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
    private BinarySearch.TreeNode tree;

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