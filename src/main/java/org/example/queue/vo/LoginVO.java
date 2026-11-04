package org.example.queue.vo;

import lombok.Data;

/**
 * 登录成功响应 VO——返回给前端的登录结果
 * <p>
 * 前端拿到 token 后必须在后续所有接口请求里带 header: X-Token: 这个token值
 */
@Data
public class LoginVO {
    /** 登录 token（32 位 UUID），后续请求放 X-Token header 里 */
    private String token;
    /** 登录用户名（如 admin、doctor） */
    private String username;
    /** 角色：ADMIN=管理员（全权限）、DOCTOR=医生（不能 reset/remove） */
    private String role;
}
