package org.example.queue.vo;

/**
 * 登录请求 DTO——前端 POST /login 时的 JSON 请求体
 * FIX: 去掉 Lombok，改回手写 getter/setter
 */
public class LoginRequest {
    private String username;
    private String password;

    public LoginRequest() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
