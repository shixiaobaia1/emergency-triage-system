package org.example.queue.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


//  Web MVC 配置类——把 AuthInterceptor 注册到 Spring MVC 的拦截器链里
//
// addPathPatterns("/**") — 拦截所有请求
// excludePathPatterns     — 白名单放行（这些路径不走鉴权）

@Component
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")// 拦截所有 URI
                .excludePathPatterns(
                        "/login",
                        "/error",
                        "/static/**",
                        "/favicon.ico"//这些需要放行
                );
    }
}
