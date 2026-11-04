package org.example.queue.vo;

import lombok.Data;

/**
    登录成功响应 VO——返回给前端的登录结果
    前端拿到 token 后后续所有接口请求里一定要带 header: X-Token: oken
 */
@Data
public class LoginVO {
    /** 登录 token（32 位 UUID）*/
    private String token;
    private String username;
    private String role;
}
