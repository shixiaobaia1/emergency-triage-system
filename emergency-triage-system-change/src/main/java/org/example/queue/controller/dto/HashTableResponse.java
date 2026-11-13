package org.example.queue.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.queue.vo.PatientVO;

import java.util.List;

/**
 * 哈希表响应
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HashTableResponse {

    private Summary summary;
    private List<SlotEntry> slots;
    private SearchResult searchResult;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Summary {
        private int m;
        private int p;
        private int n;
        private double loadFactor;
        private double aslSuccess;
        private double aslFailure;
        private int collisionCount;
        private int maxProbeLength;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SlotEntry {
        private int slot;
        private Integer key;
        private Integer probeCount;
        private PatientVO patient;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SearchResult {
        private boolean found;
        private int compares;
        private int slot;
        private PatientVO target;
    }
}