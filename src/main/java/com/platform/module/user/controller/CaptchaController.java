package com.platform.module.user.controller;

import com.platform.common.result.Result;
import com.platform.common.util.CaptchaUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
public class CaptchaController {

    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final long CAPTCHA_TTL_MINUTES = 5;

    private final StringRedisTemplate redisTemplate;

    public CaptchaController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Generate a new image captcha.
     * Returns captchaKey (used to verify) and base64 captcha image.
     */
    @GetMapping("/api/captcha")
    public Result<Map<String, String>> generate() {
        CaptchaUtil.CaptchaResult captcha = CaptchaUtil.generate();

        String key = UUID.randomUUID().toString().replace("-", "");

        // Store in Redis: captcha:{uuid} -> code
        redisTemplate.opsForValue().set(
                CAPTCHA_PREFIX + key,
                captcha.code(),
                CAPTCHA_TTL_MINUTES,
                TimeUnit.MINUTES
        );

        Map<String, String> result = new HashMap<>();
        result.put("captchaKey", key);
        result.put("captchaImage", captcha.base64Image());

        return Result.success(result);
    }

    /**
     * Validate captcha code. Returns true if valid, deletes the key after validation.
     */
    public static boolean validateCaptcha(StringRedisTemplate redisTemplate, String captchaKey, String captchaCode) {
        if (captchaKey == null || captchaCode == null) {
            return false;
        }
        String storedCode = redisTemplate.opsForValue().get(CAPTCHA_PREFIX + captchaKey);
        if (storedCode == null) {
            return false;
        }
        boolean valid = storedCode.equalsIgnoreCase(captchaCode);
        if (valid) {
            // Delete after successful validation to prevent reuse
            redisTemplate.delete(CAPTCHA_PREFIX + captchaKey);
        }
        return valid;
    }
}
