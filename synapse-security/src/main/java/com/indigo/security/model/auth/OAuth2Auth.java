package com.indigo.security.model.auth;
import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * OAuth2.0认证信息
 * 只包含OAuth2.0认证必需的凭证信息
 *
 * @author 史偕成
 * @date 2025/08/11 12:41:56
 */
@Data
@Builder
public class OAuth2Auth {

    /**
     * 客户端ID
     */
    @NotBlank(message = "客户端ID不能为空")
    private String clientId;

    /**
     * 客户端密钥
     */
    @NotBlank(message = "客户端密钥不能为空")
    private String clientSecret;

    /**
     * 授权码
     */
    private String code;

    /**
     * 重定向URI
     */
    private String redirectUri;

    /**
     * 授权范围
     */
    private String scope;

    /**
     * OAuth2.0提供商
     */
    private String provider;

    /**
     * 验证认证信息的完整性
     * 根据不同的OAuth2.0流程验证必需字段
     *
     * @return 是否有效
     */
    public boolean isValid() {
        // 基础验证：客户端ID和密钥必须存在
        if (clientId == null || clientId.trim().isEmpty() 
            || clientSecret == null || clientSecret.trim().isEmpty()) {
            return false;
        }
        
        // 授权码模式：需要授权码和重定向URI
        if (code != null && !code.trim().isEmpty()) {
            return redirectUri != null && !redirectUri.trim().isEmpty();
        }
        
        // 客户端凭证模式：只需要客户端ID和密钥
        return true;
    }
} 