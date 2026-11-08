package org.example.queue.entity;

/**
 * 用户实体——支持两个角色：ADMIN（管理员）和 DOCTOR（医生）
 * FIX: 去掉 Lombok，改回手写构造器 + getter/setter
 */
public class User {
    private String username;
    private String password;
    private Role role;

    public User() {}

    public User(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
