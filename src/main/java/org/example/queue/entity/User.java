package org.example.queue.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户实体——支持两个角色：ADMIN（管理员）和 DOCTOR（医生）
 * <p>
 * FIX: 补回 Lombok @Data/@AllArgsConstructor/@NoArgsConstructor，原手写 getter/setter 砍掉
 * FIX: 登录鉴权改造——新增 role 字段区分权限
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    /** 登录用户名（预置：admin / doctor） */
    private String username;
    /** 登录密码（demo 里明文存内存，生产要加密+存数据库） */
    private String password;
    /** 角色枚举：ADMIN=全权限，DOCTOR=受限（不能 reset/removePatient） */
    private Role role;
}
