package org.example.queue.controller.dto;

import lombok.Data;

/** 折半查找请求 */
@Data
public class BinarySearchRequest {

    /** 要查的关键字值 */
    private Integer key;

    /** "ID" 或 "WAIT_TIME"，默认 ID */
    private String keyType = "ID";
}