package com.platform.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 文件访问签名工具 - 生成有时效性的文件访问签名URL，替代JWT token通过URL参数传递
 * 每次生成签名有效期5分钟，过期后需要重新获取
 * 
 * 使用独立密钥（platform.file.sign-secret），不与JWT密钥共享
 */
@Component
public class FileAccessSigner {

    private final String secret;

    /** 签名有效期（秒） */
    private static final long EXPIRE_SECONDS = 5 * 60;

    public FileAccessSigner(@Value("${platform.file.sign-secret}") String secret) {
        this.secret = secret;
    }

    /**
     * 生成文件访问签名
     * @param fileId 文件ID
     * @param type 访问类型 (download/preview)
     * @return 签名后的token字符串
     */
    public String sign(Long fileId, String type) {
        long expiresAt = System.currentTimeMillis() / 1000 + EXPIRE_SECONDS;
        String payload = fileId + ":" + type + ":" + expiresAt;
        String signature = hmacSha256(payload, secret);

        // Base64编码: fileId:type:expiresAt:signature
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((fileId + ":" + type + ":" + expiresAt + ":" + signature)
                        .getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 验证并解析文件访问签名
     * @param token 签名token
     * @return 解析结果，包含fileId和type；无效则返回null
     */
    public ValidationResult validate(String token) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":");
            if (parts.length != 4) return null;

            long fileId = Long.parseLong(parts[0]);
            String type = parts[1];
            long expiresAt = Long.parseLong(parts[2]);
            String providedSignature = parts[3];

            // 检查是否过期
            if (System.currentTimeMillis() / 1000 > expiresAt) {
                return null;
            }

            // 使用恒定时间比较防止时序攻击
            String payload = fileId + ":" + type + ":" + expiresAt;
            String expectedSignature = hmacSha256(payload, secret);
            if (!MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    providedSignature.getBytes(StandardCharsets.UTF_8))) {
                return null;
            }

            return new ValidationResult(fileId, type);
        } catch (Exception e) {
            return null;
        }
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC computation failed", e);
        }
    }

    /**
     * 签名验证结果
     */
    public static class ValidationResult {
        private final Long fileId;
        private final String type;

        public ValidationResult(Long fileId, String type) {
            this.fileId = fileId;
            this.type = type;
        }

        public Long getFileId() { return fileId; }
        public String getType() { return type; }
    }
}
