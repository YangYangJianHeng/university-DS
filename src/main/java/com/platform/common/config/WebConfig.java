package com.platform.common.config;

import com.platform.common.interceptor.JwtAuthenticationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtAuthenticationInterceptor jwtInterceptor;

    public WebConfig(JwtAuthenticationInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(List.of(
                        "/api/user/register",
                        "/api/user/login",
                        "/api/competition/list",
                        "/api/competition/**",
                        "/api/teacher/list",
                        "/api/teacher/**",
                        "/h2-console/**",
                        "/doc.html",
                        "/webjars/**",
                        "/v3/**",
                        "/swagger-resources/**"
                ));
    }
}
