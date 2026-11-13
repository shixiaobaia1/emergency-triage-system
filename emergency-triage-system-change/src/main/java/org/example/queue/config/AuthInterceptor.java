package org.example.queue.config;

import org.example.queue.context.UserContext;
import org.example.queue.entity.User;
import org.example.queue.util.TokenUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 全局鉴权拦截器——每个请求进 Controller 之前都会先过这里

 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenUtil tokenUtil;

    public AuthInterceptor(TokenUtil tokenUtil) {
        this.tokenUtil = tokenUtil;
    }

    /**
     @return true=放行（进 Controller），false=拦截（不进 Controller）
     */
@Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        // 放行登录接口、错误页、静态资源、首页、第三阶段接口
        if(uri.equals("/login") || uri.equals("/error")
                || uri.startsWith("/static")
                || uri.equals("/") || uri.equals("/index.html")
                || uri.startsWith("/binary")
                || uri.startsWith("/hash")
                || uri.startsWith("/schedule")
                // 直接按文件名访问的静态资源（如 /bg.jpg）也放行
                || uri.matches(".*\\.(jpg|jpeg|png|gif|svg|ico|css|js|woff2?|ttf|eot|webp)$")) {
            return true;
        }

        // 从请求头取 token
        String token = request.getHeader("X-Token");
        User user = tokenUtil.getUser(token);
        if(user == null) {
            // token 不存在或已过期 → 直接返回 401 JSON，不进 Controller
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"未登录或 token 已过期\",\"data\":null}");
            return false;
        }

        // token 有效 → 把 user 放进 ThreadLocal，Controller/Service 里随时能拿
        UserContext.set(user);
        return true;
    }

    /**
     * 请求结束后清理 ThreadLocal
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
