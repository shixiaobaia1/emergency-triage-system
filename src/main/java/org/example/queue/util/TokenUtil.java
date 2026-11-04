package org.example.queue.util;

import org.example.queue.entity.User;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简易 Token 工具——内存 session，不引新依赖
 * <p>
 * 工作原理：登录成功后生成一个 UUID token，token → User 对象存进 ConcurrentHashMap
 * 后续请求带 X-Token header，拦截器用 token 反查 User
 * <p>
 * ⚠️ 仅 demo 够用（重启后所有 session 丢失），生产环境建议换 Redis + JWT
 */
@Component
public class TokenUtil {

    // token → User 的内存映射
    // ConcurrentHashMap 保证并发读写线程安全（多用户同时登录/登出不会出问题）
    private final Map<String, User> sessions = new ConcurrentHashMap<>();

    /**
     * 登录成功后生成 token 并绑定用户
     * @param user 登录成功的用户对象
     * @return 32 位无横线 UUID 字符串，前端存下来后续请求带在 X-Token header 里
     */
    public String generateToken(User user) {
        // UUID.randomUUID() 是 36 位（含 4 个 "-"），replace 后变 32 位，更短
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, user);
        return token;
    }

    /**
     * 根据 token 取出绑定的用户
     * @param token 前端传的 X-Token 值
     * @return token 存在返回对应 User，不存在/过期返回 null
     */
    public User getUser(String token) {
        if(token == null) return null;
        return sessions.get(token);
    }

    /**
     * 登出 / 强制下线
     * 从 session 池里删掉这个 token，后续再带这个 token 就查不到了
     */
    public void invalidate(String token) {
        sessions.remove(token);
    }
}
