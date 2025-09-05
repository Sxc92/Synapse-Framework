package com.indigo.security.model;

import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * Token认证信息
 * 只包含认证必需的token信息
 *
 * @author 史偕成
 * @date 2025/08/11 12:41:56
 */
@Data
@Builder
public class TokenAuth {

    /**
     * 访问令牌
     */
    @NotBlank(message = "访问令牌不能为空")
    private String token;

    /**
     * 用户名（可选，用于某些场景下的用户标识）
     */
    private String username;

    /**
     * 验证认证信息的完整性
     *
     * @return 是否有效
     */
    public boolean isValid() {
        return token != null && !token.trim().isEmpty();
    }

    /**
     * 获取用户名
     *
     * @return 用户名，如果未设置则返回null
     */
    public String getUsername() {
        return username;
    }
} 