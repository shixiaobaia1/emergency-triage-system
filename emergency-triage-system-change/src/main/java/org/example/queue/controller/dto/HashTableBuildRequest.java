package org.example.queue.controller.dto;

import lombok.Data;

/** 哈希表构建请求 */
@Data
public class HashTableBuildRequest {

    /** 表长 m，不传则默认 101（质数，p=m，α≈0.79） */
    private Integer tableLength = 101;

    /** "ID" 或 "WAIT_TIME"，默认 WAIT_TIME（能产生真实冲突） */
    private String keyType = "WAIT_TIME";
}