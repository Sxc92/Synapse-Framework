package com.indigo.security.service;

import cn.dev33.satoken.stp.StpUtil;
import com.indigo.cache.session.UserSessionService;
import com.indigo.core.context.UserContext;
import com.indigo.core.entity.Result;
import com.indigo.security.core.AuthenticationService;
import com.indigo.security.model.AuthRequest;
import com.indigo.security.model.AuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 简化的认证服务实现
 * 直接使用Sa-Token框架处理所有类型的认证请求
 * 支持用户名密码、OAuth2.0、Token验证等多种认证方式
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultAuthenticationService implements AuthenticationService {

    private final UserSessionService userSessionService;

    @Override
    public Result<AuthResponse> authenticate(AuthRequest request) {
        try {
            // 验证请求完整性
            if (request == null) {
                return Result.error("认证请求不能为空");
            }
            
            if (!request.isValid()) {
                return Result.error("认证请求信息不完整");
            }

            log.info("开始认证: authType={}, username={}", request.getAuthType(), request.getUsername());
            
            // 通过Sa-Token框架处理认证
            String token = processWithSaToken(request);
            
            // 存储用户会话信息
            storeUserSession(token, request);
            
            log.info("认证成功: username={}, token={}", request.getUsername(), token);
            
            // TODO: 操作审计 - 记录登录成功事件
            // auditService.logUserAction(request.getUserId(), "LOGIN_SUCCESS", "authentication", "SUCCESS");
            
            return Result.success(AuthResponse.of(token, null, 7200L));
            
        } catch (Exception e) {
            log.error("认证失败: authType={}", request != null ? request.getAuthType() : "null", e);
            
            // TODO: 操作审计 - 记录登录失败事件
            // if (request != null) {
            //     auditService.logUserAction(request.getUserId(), "LOGIN_FAILED", "authentication", "FAILED: " + e.getMessage());
            // }
            
            return Result.error("认证失败: " + e.getMessage());
        }
    }

    @Override
    public Result<AuthResponse> renewToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return Result.error("Token不能为空");
            }

            log.info("开始Token续期");
            
            // 通过Sa-Token框架续期
            StpUtil.renewTimeout(7200L);
            
            log.info("Token续期成功");
            return Result.success(AuthResponse.of(token, null, 7200L));
            
        } catch (Exception e) {
            log.error("Token续期失败", e);
            return Result.error("续期失败: " + e.getMessage());
        }
    }

    @Override
    public UserContext getCurrentUser() {
        try {
            String token = StpUtil.getTokenValue();
            if (token != null) {
                return userSessionService.getUserSession(token);
            }
        } catch (Exception e) {
            log.warn("获取当前用户信息失败", e);
        }
        return null;
    }

    @Override
    public Result<Void> logout() {
        try {
            StpUtil.logout();
            log.info("用户登出成功");
            return Result.success(null);
        } catch (Exception e) {
            log.error("用户登出失败", e);
            return Result.error("登出失败: " + e.getMessage());
        }
    }

    /**
     * 通过Sa-Token框架处理不同类型的认证
     *
     * @param request 认证请求
     * @return 生成的Token
     */
    private String processWithSaToken(AuthRequest request) {
        switch (request.getAuthType()) {
            case USERNAME_PASSWORD:
                // Sa-Token用户名密码登录
                StpUtil.login(request.getUserId());
                return StpUtil.getTokenValue();
                
            case OAUTH2_AUTHORIZATION_CODE:
            case OAUTH2_CLIENT_CREDENTIALS:
                // Sa-Token OAuth2.0登录
                StpUtil.login(request.getUserId());
                return StpUtil.getTokenValue();
                
            case TOKEN_VALIDATION:
                // Sa-Token Token验证
                return validateTokenWithSaToken(request.getTokenAuth().getToken());
                
            case REFRESH_TOKEN:
                // Sa-Token刷新Token
                return processRefreshToken(request.getRefreshTokenAuth().getRefreshToken());
                
            default:
                throw new IllegalArgumentException("不支持的认证类型: " + request.getAuthType());
        }
    }

    /**
     * 验证Token（通过Sa-Token框架）
     *
     * @param token Token值
     * @return 验证后的Token
     */
    private String validateTokenWithSaToken(String token) {
        // 通过Sa-Token验证Token
        StpUtil.checkLogin();
        return token;
    }

    /**
     * 处理刷新Token（通过Sa-Token框架）
     *
     * @param refreshToken 刷新Token
     * @return 新的访问Token
     */
    private String processRefreshToken(String refreshToken) {
        // 通过Sa-Token处理刷新Token
        // 这里可以根据需要实现具体的刷新逻辑
        StpUtil.renewTimeout(7200L);
        return StpUtil.getTokenValue();
    }

    /**
     * 存储用户会话信息
     *
     * @param token Token值
     * @param request 认证请求
     */
    private void storeUserSession(String token, AuthRequest request) {
        UserContext userContext = UserContext.builder()
            .userId(request.getUserId())
            .username(request.getUsername())
            .loginTime(System.currentTimeMillis())
            .roles(request.getRoles())
            .permissions(request.getPermissions())
            .build();
            
        long tokenTimeout = 7200L;
        userSessionService.storeUserSession(token, userContext, tokenTimeout);
        userSessionService.storeUserRoles(token, request.getRoles(), tokenTimeout);
        userSessionService.storeUserPermissions(token, request.getPermissions(), tokenTimeout);
    }
} 