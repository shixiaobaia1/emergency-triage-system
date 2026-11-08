package org.example.queue.controller.dto;

import lombok.Data;

/** 哈希表查找请求 */
@Data
public class HashTableSearchRequest {

    /** 要查的关键字值 */
    private Integer key;

    /** "ID" 或 "WAIT_TIME"，必须和构建时一致 */
    private String keyType = "WAIT_TIME";

    /** 表长 m，必须和构建时一致，不传默认 101 */
    private Integer tableLength = 101;
}
