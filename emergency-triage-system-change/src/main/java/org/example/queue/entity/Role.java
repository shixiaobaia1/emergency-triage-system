package org.example.queue.entity;

/**
 * 用户角色枚举
 *   ADMIN  —— 管理员：所有功能都能用（包括 reset / remove）
 *   DOCTOR —— 医生：不能用 reset 和 remove，其他接口正常
 *
 * 账号从 static/account.txt 启动时加载（格式：username password ROLE）
 */
public enum Role {
    ADMIN,
    DOCTOR;
}
