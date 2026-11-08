package org.example.queue.vo;

/**
 * 登录成功响应 VO——返回给前端的登录结果
 * FIX: 去掉 Lombok，改回手写 getter/setter
 */
public class LoginVO {
    private String token;
    private String username;
    private String role;

    public LoginVO() {}

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
