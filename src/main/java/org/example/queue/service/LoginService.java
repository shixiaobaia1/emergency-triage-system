package org.example.queue.service;

import org.example.queue.entity.Role;
import org.example.queue.entity.User;
import org.example.queue.util.TokenUtil;
import org.example.queue.vo.LoginVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 登录服务
 * FIX: 加 @Service 让 Spring 管理，原实现没注解无法注入
 * FIX: 加 TokenUtil 生成登录 token
 */
@Service
public class LoginService {

    // 内存里预置两个账号（demo 用，后续接数据库可以替换）
    private final List<User> users = List.of(
            new User("admin", "123456", Role.ADMIN),
            new User("doctor", "123456", Role.DOCTOR)
    );

    private final TokenUtil tokenUtil;

    public LoginService(TokenUtil tokenUtil) {
        this.tokenUtil = tokenUtil;
    }

    /**
     * 登录验证
     * @return LoginVO（含 token 和角色），密码错返回 null
     */
    public LoginVO login(String username, String password) {
        if(username == null || password == null) return null;
        for(User user : users) {
            // FIX: 把输入的 password（外部不可信）放前面调 equals，user.getPassword()（内存里的常量）放后面
            //  这样即使 user.getPassword() 被别处误置 null 也不会 NPE（虽然有上面的 null check）
            if(user.getUsername().equals(username) && (password != null && password.equals(user.getPassword()))) {
                // FIX: 登录成功 → 生成 token 并绑到 user
                String token = tokenUtil.generateToken(user);
                LoginVO vo = new LoginVO();
                vo.setToken(token);
                vo.setUsername(user.getUsername());
                vo.setRole(user.getRole().name());
                return vo;
            }
        }
        return null;
    }

    /** 登出——让 token 失效 */
    public void logout(String token) {
        tokenUtil.invalidate(token);
    }
}
