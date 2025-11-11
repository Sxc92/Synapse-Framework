package com.indigo.security.service;

import cn.dev33.satoken.stp.StpUtil;
import com.indigo.cache.session.UserSessionService;
import com.indigo.core.context.UserContext;
import com.indigo.core.entity.Result;
import com.indigo.core.exception.Ex;
import com.indigo.security.core.AuthenticationService;
import com.indigo.security.model.AuthRequest;
import com.indigo.security.model.AuthResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

import static com.indigo.security.constants.SecurityError.*;

/**
 * 简化的认证服务实现
 * 直接使用Sa-Token框架处理所有类型的认证请求
 * 支持用户名密码、OAuth2.0、Token验证等多种认证方式
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Slf4j
public class DefaultAuthenticationService implements AuthenticationService {

    private final UserSessionService userSessionService;

    public DefaultAuthenticationService() {
        this.userSessionService = null;
    }

    public DefaultAuthenticationService(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    @Override
    public AuthResponse authenticate(AuthRequest request) {
        // 验证请求完整性
        AuthRequest.isValid(request);
        log.info("开始认证: authType={}, username={}", request.getAuthType(), request.getUsername());
        // 通过Sa-Token框架处理认证
        String token = processWithSaToken(request);
        // 存储用户会话信息
        storeUserSession(token, request);

        return AuthResponse.of(token, null, 7200L);
    }

    @Override
    public AuthResponse renewToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            Ex.throwEx(AUTH_TOKEN_NULL);
        }
        log.info("开始Token续期");
        // 通过Sa-Token框架续期
        StpUtil.renewTimeout(7200L);
        log.info("Token续期成功");
        return AuthResponse.of(token, null, 7200L);

    }

    @Override
    public UserContext getCurrentUser() {
        try {
            String token = StpUtil.getTokenValue();
            if (token != null && userSessionService != null) {
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
        return switch (request.getAuthType()) {
            case USERNAME_PASSWORD -> {
                // Sa-Token用户名密码登录
                StpUtil.login(request.getUserId());
                yield StpUtil.getTokenValue();
            }
            case OAUTH2_AUTHORIZATION_CODE, OAUTH2_CLIENT_CREDENTIALS -> {
                // Sa-Token OAuth2.0登录
                StpUtil.login(request.getUserId());
                yield StpUtil.getTokenValue();
            }
            case TOKEN_VALIDATION ->
                // Sa-Token Token验证
                    validateTokenWithSaToken(request.getTokenAuth().getToken());
            case REFRESH_TOKEN ->
                // Sa-Token刷新Token
                    processRefreshToken(request.getRefreshTokenAuth().getRefreshToken());
            default -> throw new IllegalArgumentException("不支持的认证类型: " + request.getAuthType());
        };
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
     * @param token   Token值
     * @param request 认证请求
     */
    private void storeUserSession(String token, AuthRequest request) {
        if (userSessionService == null) {
            log.warn("UserSessionService 未配置，跳过会话存储");
            return;
        }

        UserContext userContext = UserContext.builder()
                .userId(request.getUserId())
                .account(request.getUsername())
                .loginTime(System.currentTimeMillis())
                .roles(request.getRoles())
                .permissions(request.getPermissions())
                .build();

        long tokenTimeout = 7200L;

        // 1. 存储用户会话（包含完整用户信息）
        userSessionService.storeUserSession(token, userContext, tokenTimeout);

        // 2. 单独存储权限（PermissionManager 需要）
        if (request.getPermissions() != null && !request.getPermissions().isEmpty()) {
            userSessionService.storeUserPermissions(token, request.getPermissions(), tokenTimeout);
        }

        // 3. 单独存储角色（PermissionManager 需要）
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            userSessionService.storeUserRoles(token, request.getRoles(), tokenTimeout);
        }
    }

    // ========== 菜单、资源、系统管理相关方法实现 ==========

    @Override
    public <TMenu, TResource, TSystem> void storeUserPermissionData(
            String token,
            List<TMenu> menus,
            List<TResource> resources,
            List<TSystem> systems,
            long expiration) {
        if (userSessionService == null) {
            log.warn("UserSessionService 未配置，跳过权限数据存储");
            return;
        }

        try {
            // 存储菜单列表
            if (menus != null && !menus.isEmpty()) {
                userSessionService.storeUserMenus(token, menus, expiration);
                log.debug("Stored user menus for token: {}, count: {}", token, menus.size());
            }

            // 存储资源列表
            if (resources != null && !resources.isEmpty()) {
                userSessionService.storeUserResources(token, resources, expiration);
                log.debug("Stored user resources for token: {}, count: {}", token, resources.size());
            }

            // 存储系统列表
            if (systems != null && !systems.isEmpty()) {
                userSessionService.storeUserSystems(token, systems, expiration);
                log.debug("Stored user systems for token: {}, count: {}", token, systems.size());
            }

            log.info("Stored user permission data (menus, resources, systems) for token: {}", token);
        } catch (Exception e) {
            log.error("存储用户权限数据失败: token={}", token, e);
            throw new RuntimeException("存储用户权限数据失败: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> Result<List<T>> getUserMenus(String token, Class<T> clazz) {
        if (userSessionService == null) {
            return Result.error("UserSessionService 未配置");
        }

        try {
            List<T> menus = userSessionService.getUserMenus(token, clazz);
            if (menus == null) {
                menus = Collections.emptyList();
            }
            return Result.success(menus);
        } catch (Exception e) {
            log.error("获取用户菜单列表失败: token={}, clazz={}", token, clazz.getName(), e);
            return Result.error("获取用户菜单列表失败: " + e.getMessage());
        }
    }

    @Override
    public <T> Result<List<T>> getUserResources(String token, Class<T> clazz) {
        if (userSessionService == null) {
            return Result.error("UserSessionService 未配置");
        }

        try {
            List<T> resources = userSessionService.getUserResources(token, clazz);
            if (resources == null) {
                resources = Collections.emptyList();
            }
            return Result.success(resources);
        } catch (Exception e) {
            log.error("获取用户资源列表失败: token={}, clazz={}", token, clazz.getName(), e);
            return Result.error("获取用户资源列表失败: " + e.getMessage());
        }
    }

    @Override
    public <T> Result<List<T>> getUserSystems(String token, Class<T> clazz) {
        if (userSessionService == null) {
            return Result.error("UserSessionService 未配置");
        }

        try {
            List<T> systems = userSessionService.getUserSystems(token, clazz);
            if (systems == null) {
                systems = Collections.emptyList();
            }
            return Result.success(systems);
        } catch (Exception e) {
            log.error("获取用户系统列表失败: token={}, clazz={}", token, clazz.getName(), e);
            return Result.error("获取用户系统列表失败: " + e.getMessage());
        }
    }
} 