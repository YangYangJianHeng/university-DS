package com.platform.common.config;

import com.platform.common.interceptor.AdminInterceptor;
import com.platform.common.interceptor.JwtAuthenticationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtAuthenticationInterceptor jwtInterceptor;
    private final AdminInterceptor adminInterceptor;

    public WebConfig(JwtAuthenticationInterceptor jwtInterceptor,
                     AdminInterceptor adminInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
        this.adminInterceptor = adminInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // JWT认证拦截器：拦截所有 /api/**，排除公开端点
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(List.of(
                        "/api/user/register",
                        "/api/user/login",
                        "/api/captcha",
                        // 公开API（落地页统计等）
                        "/api/public/**",
                        // 公开的GET端点（列表、详情）放行
                        "/api/competition/list",
                        "/api/competition/*",  // GET /api/competition/{id}
                        "/api/teacher/list",
                        "/api/teacher/*",       // GET /api/teacher/{id}
                        "/api/recruit/list",
                        "/api/recruit/*",       // GET /api/recruit/{id}
                        "/api/achievement/list",
                        "/api/achievement/share/list",
                        // 文档
                        "/h2-console/**",
                        "/doc.html",
                        "/webjars/**",
                        "/v3/**",
                        "/swagger-resources/**"
                ));

        // Admin角色拦截器：拦截所有 /api/admin/**
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/admin/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map /uploads/** to file system uploads/ directory for avatar access
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
