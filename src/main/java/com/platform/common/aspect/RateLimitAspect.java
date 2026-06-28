package com.platform.common.aspect;

import com.platform.common.annotation.RateLimit;
import com.platform.common.exception.BusinessException;
import com.platform.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * 限流AOP切面 - 基于Redis滑动窗口计数器
 * 对 @RateLimit 注解的方法进行IP级别的频率限制
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;

    public RateLimitAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = buildRateLimitKey(rateLimit.prefix());
        int window = rateLimit.window();
        int maxRequests = rateLimit.maxRequests();

        // Redis INCR + TTL 实现固定窗口计数器
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, window, TimeUnit.SECONDS);
        }

        if (count != null && count > maxRequests) {
            log.warn("Rate limit exceeded: key={}, count={}, max={}", key, count, maxRequests);
            throw BusinessException.tooManyRequests(rateLimit.message());
        }

        return joinPoint.proceed();
    }

    private String buildRateLimitKey(String prefix) {
        String clientIp = getClientIp();
        return prefix + ":" + clientIp;
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "unknown";
            HttpServletRequest request = attrs.getRequest();
            // 优先从代理头获取真实IP
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            // X-Forwarded-For 可能包含多个IP，取第一个
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        } catch (Exception e) {
            return "unknown";
        }
    }
}
