package org.example.queue.config;

import org.example.queue.context.UserContext;
import org.example.queue.entity.User;
import org.example.queue.util.TokenUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

/**
 * 全局鉴权拦截器——每个 /queue/** 请求进 Controller 之前都会先过这里
 * <p>
 * 执行流程：
 *  1. preHandle：请求进来 → 检查 URI 要不要放行 → 要的直接 return true；不要的校验 X-Token header
 *     - token 有效 → UserContext.set(user) → return true（放行进 Controller）
 *     - token 无效/缺失 → 写 401 JSON → return false（拦截）
 *  2. afterCompletion：Controller 执行完（不管成功/异常）都回调这里 → UserContext.clear()（必须清，防 ThreadLocal 泄漏）
 * <p>
 * 放行范围在 WebMvcConfig 里配置，这里只管拦截逻辑
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenUtil tokenUtil;

    // 构造器注入（Spring 推荐）
    public AuthInterceptor(TokenUtil tokenUtil) {
        this.tokenUtil = tokenUtil;
    }

    /**
     * 请求预处理——鉴权主逻辑
     * @return true=放行（进 Controller），false=拦截（不进 Controller）
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // FIX: 只放行 POST /login（登录接口），不放行 /login/logout 和 /login/current
        //  因为后两个也需要 token 校验——logout 要知道谁登出，current 要拿当前用户
        //  精确匹配 "/login" 而不是 startsWith("/login")
        if(uri.equals("/login") || uri.equals("/error") || uri.startsWith("/static")) {
            return true;
        }

        // 从请求头取 token（前端每次调接口要带 X-Token: xxx）
        String token = request.getHeader("X-Token");
        // FIX: 加一行调试日志，确认 Apifox 到底有没有传 token 过来
        System.out.println("[AuthInterceptor] uri=" + uri + " | X-Token=" + token + " | sessions大小=" + sessionsCount());
        User user = tokenUtil.getUser(token);
        if(user == null) {
            // token 不存在或已过期 → 直接返回 401 JSON，不进 Controller
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"未登录或 token 已过期\",\"data\":null}");
            return false;
        }

        // token 有效 → 把 user 放进 ThreadLocal，Controller 里随时拿 UserContext.get()
        UserContext.set(user);
        return true;
    }

    /**
     * 请求完成回调——必须清 ThreadLocal
     * ThreadLocal 在 Tomcat 线程池里是复用的，不清会把 A 用户的上下文带到 B 用户的请求里（串号）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    // FIX: 临时调试用——看看 TokenUtil 里当前有多少 session
    private int sessionsCount() {
        try {
            // 反射拿 sessions map 的 size（TokenUtil 没暴露 size 方法，临时用）
            java.lang.reflect.Field f = TokenUtil.class.getDeclaredField("sessions");
            f.setAccessible(true);
            Map<?, ?> m = (Map<?, ?>) f.get(tokenUtil);
            return m.size();
        } catch (Exception e) {
            return -1;
        }
    }
}
