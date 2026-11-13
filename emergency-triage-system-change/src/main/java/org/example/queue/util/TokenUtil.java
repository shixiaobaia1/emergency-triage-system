package org.example.queue.util;

import org.example.queue.entity.User;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenUtil {

    // token映射是哪个账号
    // ConcurrentHashMap 保证并发读写线程安全（多用户同时登录/登出不会出问题）
    private final Map<String, User> sessions = new ConcurrentHashMap<>();

    /**
     登录成功后生成 token 并绑定用户
      @param user 登录成功的用户对象
      @return 32 位无横线 UUID 字符串，前端存下来后续请求带在 X-Token header 里
     */
    public String generateToken(User user) {
        // UUID.randomUUID() 随机生成UUID字符串
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, user);
        return token;
    }

    /**
     * 根据 token 取出绑定的用户
     * 没有就为null
     */
    public User getUser(String token) {
        if(token == null) return null;
        return sessions.get(token);
    }

    /**
     * 登出账号，让token失效
     */
    public void invalidate(String token) {
        sessions.remove(token);
    }
}
