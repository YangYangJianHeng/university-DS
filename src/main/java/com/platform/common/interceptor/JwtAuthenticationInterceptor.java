package com.platform.common.interceptor;

import com.platform.common.exception.BusinessException;
import com.platform.common.util.JwtUtil;
import com.platform.common.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 仅从 Authorization Header 读取 JWT token
        // 文件访问通过 FileAccessController 使用签名URL（无需JWT），不再通过URL参数传递token
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || token.isEmpty()) {
            throw BusinessException.unauthorized("未登录");
        }

        if (!jwtUtil.validateToken(token)) {
            throw BusinessException.unauthorized("Token已过期或无效");
        }

        Long userId = jwtUtil.getUserId(token);
        String role = jwtUtil.getRole(token);
        UserContext.setCurrentUserId(userId);
        UserContext.setCurrentUserRole(role);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
