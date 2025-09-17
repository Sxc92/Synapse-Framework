package com.indigo.security.model.auth;

import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户名密码认证信息
 * 只包含认证必需的凭证信息
 *
 * @author 史偕成
 * @date 2025/08/11 12:41:56
 */
@Data
@Builder
public class UsernamePasswordAuth {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 验证认证信息的完整性
     *
     * @return 是否有效
     */
    public boolean isValid() {
        return username != null && !username.trim().isEmpty() 
            && password != null && !password.trim().isEmpty();
    }

    /**
     * 获取用户名
     *
     * @return 用户名
     */
    public String getUsername() {
        return username;
    }
} 