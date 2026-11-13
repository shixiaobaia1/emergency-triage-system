package org.example.queue.vo;

/**
 * 登录请求 DTO——前端 POST /login 时的 JSON 请求体
 */
public class LoginRequest {
    private String username;//账号
    private String password;//密码

    public LoginRequest() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
