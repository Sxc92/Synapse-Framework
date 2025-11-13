package com.indigo.security.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Gateway 签名工具类
 * 用于在 Gateway 和微服务之间传递签名，防止请求被篡改
 * 
 * 签名算法：HMAC-SHA256
 * 签名内容：token + userId + timestamp
 * 
 * @author 史偕成
 * @date 2025/01/10
 */
@Slf4j
public class GatewaySignatureUtils {

    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * 生成签名
     * 
     * @param secret Gateway 密钥
     * @param token 用户 token
     * @param userId 用户 ID
     * @param timestamp 时间戳（毫秒）
     * @return Base64 编码的签名
     */
    public static String generateSignature(String secret, String token, String userId, long timestamp) {
        if (!StringUtils.hasText(secret) || !StringUtils.hasText(token) || !StringUtils.hasText(userId)) {
            log.warn("生成签名失败：参数不完整");
            return null;
        }

        try {
            // 构建签名内容：token + userId + timestamp
            String signContent = token + userId + timestamp;
            
            // 使用 HMAC-SHA256 生成签名
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            
            byte[] signatureBytes = mac.doFinal(signContent.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("生成签名失败", e);
            return null;
        }
    }

    /**
     * 验证签名
     * 
     * @param secret Gateway 密钥
     * @param token 用户 token
     * @param userId 用户 ID
     * @param timestamp 时间戳（毫秒）
     * @param signature 待验证的签名
     * @return 验证是否通过
     */
    public static boolean verifySignature(String secret, String token, String userId, long timestamp, String signature) {
        if (!StringUtils.hasText(secret) || !StringUtils.hasText(token) || 
            !StringUtils.hasText(userId) || !StringUtils.hasText(signature)) {
            log.warn("验证签名失败：参数不完整");
            return false;
        }

        try {
            // 重新生成签名
            String expectedSignature = generateSignature(secret, token, userId, timestamp);
            
            // 比较签名（使用安全比较，防止时序攻击）
            return constantTimeEquals(expectedSignature, signature);
        } catch (Exception e) {
            log.error("验证签名异常", e);
            return false;
        }
    }

    /**
     * 常量时间比较，防止时序攻击
     * 
     * @param a 字符串 a
     * @param b 字符串 b
     * @return 是否相等
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /**
     * 检查时间戳是否在有效期内（防止重放攻击）
     * 
     * @param timestamp 时间戳（毫秒）
     * @param validityWindow 有效期窗口（毫秒），默认 5 分钟
     * @return 是否在有效期内
     */
    public static boolean isTimestampValid(long timestamp, long validityWindow) {
        long currentTime = System.currentTimeMillis();
        long timeDiff = Math.abs(currentTime - timestamp);
        return timeDiff <= validityWindow;
    }

    /**
     * 检查时间戳是否在有效期内（默认 5 分钟）
     * 
     * @param timestamp 时间戳（毫秒）
     * @return 是否在有效期内
     */
    public static boolean isTimestampValid(long timestamp) {
        // 默认有效期 5 分钟
        return isTimestampValid(timestamp, 5 * 60 * 1000L);
    }
}

