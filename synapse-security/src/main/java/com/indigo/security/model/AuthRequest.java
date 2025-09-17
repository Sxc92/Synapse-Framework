package com.indigo.security.model;

import com.indigo.security.model.auth.OAuth2Auth;
import com.indigo.security.model.auth.RefreshTokenAuth;
import com.indigo.security.model.auth.TokenAuth;
import com.indigo.security.model.auth.UsernamePasswordAuth;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 认证请求模型
 * 支持多种认证方式的统一请求格式
 *
 * @author 史偕成
 * @date 2025/08/11 12:41:56
 **/
@Data
@Builder
public class AuthRequest {

    /**
     * 认证类型
     */
    @NotNull(message = "认证类型不能为空")
    private AuthType authType;

    /**
     * 用户名密码认证信息
     */
    private UsernamePasswordAuth usernamePasswordAuth;

    /**
     * Token认证信息
     */
    private TokenAuth tokenAuth;

    /**
     * OAuth2.0认证信息
     */
    private OAuth2Auth oauth2Auth;

    /**
     * 刷新Token认证信息
     */
    private RefreshTokenAuth refreshTokenAuth;

    /**
     * 用户ID（由业务模块传入）
     */
    private String userId;

    /**
     * 用户角色列表（由业务模块传入）
     */
    private List<String> roles;

    /**
     * 用户权限列表（由业务模块传入）
     */
    private List<String> permissions;

    /**
     * 用户部门职级信息（由业务模块传入）
     */
    private List<UserDeptPositionInfo> deptPositions;

    /**
     * 认证类型枚举
     */
    public enum AuthType {
        /**
         * 用户名密码认证
         */
        USERNAME_PASSWORD,

        /**
         * Token验证
         */
        TOKEN_VALIDATION,

        /**
         * OAuth2.0 授权码模式
         */
        OAUTH2_AUTHORIZATION_CODE,

        /**
         * OAuth2.0 客户端凭证模式
         */
        OAUTH2_CLIENT_CREDENTIALS,

        /**
         * 刷新Token
         */
        REFRESH_TOKEN
    }

    /**
     * 获取用户名
     * 根据不同的认证类型从对应的认证信息中提取用户名
     *
     * @return 用户名，如果无法获取则返回null
     */
    public String getUsername() {
        return switch (authType) {
            case USERNAME_PASSWORD -> usernamePasswordAuth != null ? usernamePasswordAuth.getUsername() : null;
            case TOKEN_VALIDATION -> tokenAuth != null ? tokenAuth.getUsername() : null;
            case OAUTH2_AUTHORIZATION_CODE, OAUTH2_CLIENT_CREDENTIALS -> oauth2Auth != null ? oauth2Auth.getClientId() : null;
            case REFRESH_TOKEN -> refreshTokenAuth != null ? refreshTokenAuth.getUsername() : null;
        };
    }

    /**
     * 获取用户名密码认证信息
     */
    public com.indigo.security.model.auth.UsernamePasswordAuth getUsernamePasswordAuth() {
        if (authType == AuthType.USERNAME_PASSWORD) {
            return usernamePasswordAuth;
        }
        return null;
    }

    /**
     * 获取Token认证信息
     */
    public com.indigo.security.model.auth.TokenAuth getTokenAuth() {
        if (authType == AuthType.TOKEN_VALIDATION) {
            return tokenAuth;
        }
        return null;
    }

    /**
     * 获取OAuth2认证信息
     */
    public com.indigo.security.model.auth.OAuth2Auth getOauth2Auth() {
        if (authType == AuthType.OAUTH2_AUTHORIZATION_CODE || 
            authType == AuthType.OAUTH2_CLIENT_CREDENTIALS) {
            return oauth2Auth;
        }
        return null;
    }

    /**
     * 获取刷新Token认证信息
     */
    public com.indigo.security.model.auth.RefreshTokenAuth getRefreshTokenAuth() {
        if (authType == AuthType.REFRESH_TOKEN) {
            return refreshTokenAuth;
        }
        return null;
    }

    /**
     * 验证认证请求的完整性
     * 检查认证类型与对应的认证信息是否匹配
     *
     * @return 是否有效
     */
    public boolean isValid() {
        if (authType == null) {
            return false;
        }
        
        // 验证认证信息完整性
        boolean authInfoValid = switch (authType) {
            case USERNAME_PASSWORD -> usernamePasswordAuth != null && usernamePasswordAuth.isValid();
            case TOKEN_VALIDATION -> tokenAuth != null && tokenAuth.isValid();
            case OAUTH2_AUTHORIZATION_CODE, OAUTH2_CLIENT_CREDENTIALS -> oauth2Auth != null && oauth2Auth.isValid();
            case REFRESH_TOKEN -> refreshTokenAuth != null && refreshTokenAuth.isValid();
        };
        
        if (!authInfoValid) {
            return false;
        }
        
        // 验证用户信息完整性（角色和权限由业务模块传入）
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        
        return true;
    }

    /**
     * 用户部门职级信息
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserDeptPositionInfo {
        /**
         * 部门ID
         */
        private String deptId;

        /**
         * 部门名称
         */
        private String deptName;

        /**
         * 部门路径
         */
        private String deptPath;

        /**
         * 职级ID
         */
        private String positionId;

        /**
         * 职级名称
         */
        private String positionName;

        /**
         * 职级等级
         */
        private Integer level;

        /**
         * 是否为主部门职级
         */
        private Boolean isPrimary;

        /**
         * 开始时间
         */
        private String startDate;

        /**
         * 结束时间
         */
        private String endDate;

        /**
         * 状态
         */
        private Integer status;
    }
} 