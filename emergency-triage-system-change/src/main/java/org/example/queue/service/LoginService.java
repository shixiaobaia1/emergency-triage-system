package org.example.queue.service;

import org.example.queue.entity.Role;
import org.example.queue.entity.User;
import org.example.queue.util.TokenUtil;
import org.example.queue.vo.LoginVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class LoginService {

    //用value读 account.txt 文件
    @Value("classpath:static/account.txt")
    private org.springframework.core.io.Resource accountResource;

    // 启动时加载的用户列表
    private final List<User> users = new ArrayList<>();

    private final TokenUtil tokenUtil;

    public LoginService(TokenUtil tokenUtil) {
        this.tokenUtil = tokenUtil;
    }

    @PostConstruct
    public void loadAccounts() {
        try (InputStream is = accountResource.getInputStream();
             //拿资源
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                //加载
                line = line.trim();
                if (line.isEmpty()) continue; // 跳过空行
                String[] parts = line.split("\\s+");
                if (parts.length < 3) continue; // 格式不对跳过
                // parts[0]=username, parts[1]=password, parts[2]=role
                String username = parts[0];
                String password = parts[1];
                //最后一个单词是角色
                String roleStr = parts[parts.length - 1];
                Role role;
                try {
                    role = Role.valueOf(roleStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    continue; // 角色名不认识这行就是有问题的要跳过
                }
                users.add(new User(username, password, role));
            }
            System.out.println("[LoginService] 从 account.txt 加载了 " + users.size() + " 个账号");
        } catch (Exception e) {//出问题的提示
            System.err.println("[LoginService] 加载 account.txt 失败: " + e.getMessage());
        }
    }

    /**
     * 登录验证
     * @return LoginVO（含 token 和角色），密码错或账号不存在返回 null
     */
    public LoginVO login(String username, String password) {
        if(username == null || password == null) return null;
        for(User user : users) {
            if(user.getUsername().equals(username) && password.equals(user.getPassword())) {
                // 登录成功 → 生成 token 并绑到这个 user 对象
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
