package com.indigo.security.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indigo.core.context.UserContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;

/**
 * UserContext 编码/解码工具类
 * 用于在 Gateway 和微服务之间传递用户上下文信息
 * 
 * @author 史偕成
 * @date 2025/01/10
 */
@Slf4j
public class UserContextCodec {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 编码 UserContext 为 Base64 字符串
     * 
     * @param userContext 用户上下文
     * @return Base64 编码的 JSON 字符串
     */
    public static String encode(UserContext userContext) {
        if (userContext == null) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(userContext);
            return Base64.getEncoder().encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("编码 UserContext 失败", e);
            return null;
        }
    }

    /**
     * 解码 Base64 字符串为 UserContext
     * 
     * @param encodedValue Base64 编码的 JSON 字符串
     * @return 用户上下文，如果解码失败返回 null
     */
    public static UserContext decode(String encodedValue) {
        if (encodedValue == null || encodedValue.trim().isEmpty()) {
            return null;
        }
        try {
            byte[] jsonBytes = Base64.getDecoder().decode(encodedValue);
            String json = new String(jsonBytes, java.nio.charset.StandardCharsets.UTF_8);
            return objectMapper.readValue(json, UserContext.class);
        } catch (Exception e) {
            log.warn("解码 UserContext 失败: {}", encodedValue, e);
            return null;
        }
    }
}

