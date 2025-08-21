package com.indigo.security.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 认证响应模型
 * 专注于认证成功后的Token返回
 * 用户信息通过UserSession存储到Redis
 *
 * @author 史偕成
 * @date 2025/08/11 12:41:56
 */
@Data
@Builder
public class AuthResponse {

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 刷新令牌
     */
    private String refreshToken;

    /**
     * 令牌过期时间（秒）
     */
    private Long expiresIn;

    /**
     * 令牌创建时间
     */
    private LocalDateTime tokenCreatedAt;

    /**
     * 令牌过期时间
     */
    private LocalDateTime tokenExpiresAt;

    /**
     * 创建认证响应
     */
    public static AuthResponse of(String accessToken, String refreshToken, Long expiresIn) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .tokenCreatedAt(LocalDateTime.now())
                .tokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn))
                .build();
    }
} 