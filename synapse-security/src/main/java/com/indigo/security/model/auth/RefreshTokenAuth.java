package com.indigo.security.model.auth;
import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 刷新Token认证信息
 *
 * @author 史偕成
 * @date 2025/08/11 12:41:56
 */
@Data
@Builder
public class RefreshTokenAuth {

    /**
     * 刷新令牌
     */
    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;

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
        return refreshToken != null && !refreshToken.trim().isEmpty();
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