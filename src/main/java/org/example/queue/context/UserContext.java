package org.example.queue.context;

import org.example.queue.entity.User;

/**
 * 当前登录用户上下文——用 ThreadLocal 在线程内传递用户信息
 * <p>
 * 调用链路：AuthInterceptor.preHandle() → UserContext.set(user) → Controller/Service 直接拿 → afterCompletion 里 clear()
 * 用 ThreadLocal 是因为 Spring MVC 默认请求-线程模型下，一个请求对应一个线程，天然隔离
 * <p>
 * ⚠️ 必须在请求结束后 clear()，否则线程池复用会导致用户串号 + 内存泄漏
 */
public class UserContext {

    // ThreadLocal 静态变量，全局唯一，每个线程有独立副本
    private static final ThreadLocal<User> CURRENT = new ThreadLocal<>();

    /** 鉴权拦截器校验 token 通过后调用，把当前线程绑定到 user */
    public static void set(User user) {
        CURRENT.set(user);
    }

    /** Controller/Service 里随时取当前登录用户，返回 null 表示没登录 */
    public static User get() {
        return CURRENT.get();
    }

    /** 请求结束后必须调用，清除 ThreadLocal，防止线程池复用时串用户 */
    public static void clear() {
        CURRENT.remove();
    }
}
