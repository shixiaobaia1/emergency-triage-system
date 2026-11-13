package org.example.queue.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


//  Web MVC 配置类——把 AuthInterceptor 注册到 Spring MVC 的拦截器链里
//
// addPathPatterns("/**") — 拦截所有请求
// excludePathPatterns     — 白名单放行

//在web这里有白名单，其他进拦截器，再看拦截器的白名单，都不在就看有没有token

@Component
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    // 注册鉴权拦截器——登录接口 /error /static 放行，其他都要带 X-Token
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/error", "/static/**");
    }
    //允许别的站点访问我的接口
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
