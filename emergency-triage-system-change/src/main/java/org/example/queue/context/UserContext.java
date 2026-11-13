package org.example.queue.context;

import org.example.queue.entity.User;


  //ThreadLocal 在当前线程内传递用户信息（此登录用户中）
public class UserContext {

    // ThreadLocal 静态变量，全局唯一，每个线程有独立副本
    private static final ThreadLocal<User> CURRENT = new ThreadLocal<>();

    //鉴权拦截器校验 token 通过后调用，把当前线程绑定到 user
    public static void set(User user) {
        CURRENT.set(user);
    }

    /** 让Controller/Service 随时取当前登录用户，返回 null 表示没登录 */
    public static User get() {
        return CURRENT.get();
    }

    /** 请求结束后调用，清除 ThreadLocal，防止线程池复用时用户信息还留存 */
    public static void clear() {
        CURRENT.remove();
    }
}
