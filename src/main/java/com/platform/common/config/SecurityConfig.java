package com.platform.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 安全配置
 * 
 * 架构说明:
 * - JWT认证通过 WebConfig 中的 JwtAuthenticationInterceptor 拦截器实现
 * - 角色授权通过 AdminInterceptor 拦截器实现  
 * - Spring Security 此处主要提供: 密码编码、CSRF禁用(无状态API)、安全头、CORS
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 无状态REST API，禁用CSRF
            .csrf(csrf -> csrf.disable())
            // 无状态会话，认证由JWT拦截器处理
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 请求授权 — 实际认证由WebConfig拦截器链处理
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/user/register", "/api/user/login", "/api/captcha").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/doc.html", "/webjars/**", "/v3/**", "/swagger-resources/**").permitAll()
                .anyRequest().permitAll()
            )
            // CORS 配置
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 安全响应头
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .xssProtection(xss -> xss.headerValue(org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                .contentTypeOptions(contentType -> {})
                // 禁止IE从响应内容推断MIME类型
                // X-Content-Type-Options: nosniff 已由 contentTypeOptions 默认添加
                // Strict-Transport-Security (HSTS) 在HTTPS环境下由反向代理控制
            );
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 开发环境允许localhost，生产环境应配置为具体域名
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
