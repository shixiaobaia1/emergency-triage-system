package org.example.queue.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类——把 AuthInterceptor 注册到 Spring MVC 的拦截器链里
 * <p>
 * addPathPatterns("/**") — 拦截所有请求
 * excludePathPatterns     — 白名单放行（这些路径不走鉴权）
 *   /login/**    登录相关接口（登录前不可能有 token，必须放行）
 *   /error       Spring Boot 错误页（没配置也应该放行）
 *   /static/**   静态资源（txt 文件、html 等）
 *   /favicon.ico 网站图标
 */
@Component
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    /** 注册全局拦截器并设置拦截/放行范围 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")           // 拦截所有 URI
                .excludePathPatterns(
                        "/login/**",           // 登录系列接口（放行）
                        "/error",              // Spring Boot 错误页
                        "/static/**",          // 静态资源（patients.txt 在这下面）
                        "/favicon.ico"
                );
    }
}
