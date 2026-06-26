package com.platform.common.interceptor;

import com.platform.common.exception.BusinessException;
import com.platform.common.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Admin role interceptor - ensures only ADMIN users can access /api/admin/** endpoints.
 * This runs AFTER JwtAuthenticationInterceptor, so UserContext is already populated.
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String role = UserContext.getCurrentUserRole();
        if (role == null || !"ADMIN".equals(role)) {
            throw BusinessException.forbidden("需要管理员权限");
        }

        return true;
    }
}
