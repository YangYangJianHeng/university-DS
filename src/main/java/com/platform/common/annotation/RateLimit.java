package com.platform.common.annotation;

import java.lang.annotation.*;

/**
 * 接口限流注解 - 基于IP+接口路径限制调用频率，防止暴力破解和DDoS
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    /** 限流key前缀 */
    String prefix() default "rate_limit";
    /** 时间窗口（秒） */
    int window() default 60;
    /** 窗口内最大请求次数 */
    int maxRequests() default 10;
    /** 超出限制后的提示消息 */
    String message() default "请求过于频繁，请稍后再试";
}
