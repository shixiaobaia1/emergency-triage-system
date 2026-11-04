package org.example.queue.vo;

import lombok.Data;

/**
 * 登录请求 DTO
 * <p>
 * 前端 POST /login 时的 JSON 请求体：
 * {"username":"admin","password":"123456"}
 */
@Data
public class LoginRequest {
    /** 用户名（预置账号：admin 或 doctor） */
    private String username;
    /** 密码（预置密码：123456） */
    private String password;
}
