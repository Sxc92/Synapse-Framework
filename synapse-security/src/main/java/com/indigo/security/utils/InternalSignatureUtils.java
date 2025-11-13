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
 * 内部服务签名工具类
 * 用于在微服务之间传递签名，确保服务间调用的安全性
 * 
 * 签名算法：HMAC-SHA256
 * 签名内容：serviceName + timestamp + secret
 * 
 * @author 史偕成
 * @date 2025/01/10
 */
@Slf4j
public class InternalSignatureUtils {

    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * 内部服务调用请求头
     */
    public static final String X_INTERNAL_SERVICE_HEADER = "X-Internal-Service";
    public static final String X_INTERNAL_TIMESTAMP_HEADER = "X-Internal-Timestamp";
    public static final String X_INTERNAL_SIGNATURE_HEADER = "X-Internal-Signature";

    /**
     * 生成内部服务签名
     * 
     * @param secret 服务密钥
     * @param serviceName 服务名称
     * @param timestamp 时间戳（毫秒）
     * @return Base64 编码的签名
     */
    public static String generateSignature(String secret, String serviceName, long timestamp) {
        if (!StringUtils.hasText(secret) || !StringUtils.hasText(serviceName)) {
            log.warn("生成内部服务签名失败：参数不完整");
            return null;
        }

        try {
            // 构建签名内容：serviceName + timestamp + secret
            String signContent = serviceName + timestamp + secret;
            
            // 使用 HMAC-SHA256 生成签名
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            
            byte[] signatureBytes = mac.doFinal(signContent.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("生成内部服务签名失败", e);
            return null;
        }
    }

    /**
     * 验证内部服务签名
     * 
     * @param secret 服务密钥
     * @param serviceName 服务名称
     * @param timestamp 时间戳（毫秒）
     * @param signature 待验证的签名
     * @return 验证是否通过
     */
    public static boolean verifySignature(String secret, String serviceName, long timestamp, String signature) {
        if (!StringUtils.hasText(secret) || !StringUtils.hasText(serviceName) || 
            !StringUtils.hasText(signature)) {
            log.warn("验证内部服务签名失败：参数不完整");
            return false;
        }

        try {
            // 重新生成签名
            String expectedSignature = generateSignature(secret, serviceName, timestamp);
            
            // 比较签名（使用安全比较，防止时序攻击）
            return constantTimeEquals(expectedSignature, signature);
        } catch (Exception e) {
            log.error("验证内部服务签名异常", e);
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

