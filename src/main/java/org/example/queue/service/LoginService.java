package org.example.queue.service;

import org.example.queue.entity.Role;
import org.example.queue.entity.User;
import org.example.queue.util.TokenUtil;
import org.example.queue.vo.LoginVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoginService {

    // 内存里预置两个账号
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
            //看能否命中
            if(user.getUsername().equals(username) && (password != null && password.equals(user.getPassword()))) {
                // 登录成功 → 生成 token 并绑到这个user对象这里
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
