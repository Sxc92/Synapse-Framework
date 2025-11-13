package com.indigo.security.utils;

import org.mindrot.jbcrypt.BCrypt;
import lombok.extern.slf4j.Slf4j;

/**
 * @author 史偕成
 * @date 2025/11/07 14:38
 **/
@Slf4j
public class PasswordUtils {

    /**
     * 加密密码
     *
     * @param rawPassword 明文密码
     * @return 加密后的密码
     */
    public static String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    /**
     * 验证密码
     *
     * @param rawPassword 明文密码
     * @param hashedPassword 加密后的密码
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, hashedPassword);
        } catch (Exception e) {
            log.error("密码验证失败", e);
            return false;
        }
    }

    /**
     * 检查密码是否需要重新加密
     * BCrypt会自动处理，但可以用于检查密码强度
     *
     * @param hashedPassword 加密后的密码
     * @return 是否需要重新加密
     */
    public static boolean needsRehash(String hashedPassword) {
        // BCrypt的hash格式：$2a$10$...
        // 可以检查加密轮数，如果轮数过低，建议重新加密
        if (hashedPassword == null || !hashedPassword.startsWith("$2a$")) {
            return true;
        }
        // 可以检查加密轮数（BCrypt.gensalt()默认是10轮）
        return false;
    }
}
