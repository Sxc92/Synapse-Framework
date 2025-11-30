package com.indigo.security.service;

import com.indigo.cache.session.UserSessionService;
import com.indigo.core.context.UserContext;
import com.indigo.core.entity.Result;
import com.indigo.core.exception.Ex;
import com.indigo.security.config.SecurityProperties;
import com.indigo.security.core.AuthenticationService;
import com.indigo.security.core.TokenService;
import com.indigo.security.model.AuthRequest;
import com.indigo.security.model.AuthResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

import static com.indigo.security.constants.SecurityError.*;

/**
 * 简化的认证服务实现
 * 使用 TokenService 处理所有类型的认证请求
 * 支持用户名密码、OAuth2.0、Token验证等多种认证方式
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Slf4j
public class DefaultAuthenticationService implements AuthenticationService {

    private final UserSessionService userSessionService;
    private final TokenService tokenService;
    private final SecurityProperties securityProperties;

    public DefaultAuthenticationService() {
        this.userSessionService = null;
        this.tokenService = null;
        this.securityProperties = null;
    }

    public DefaultAuthenticationService(UserSessionService userSessionService, 
                                      TokenService tokenService,
                                      SecurityProperties securityProperties) {
        this.userSessionService = userSessionService;
        this.tokenService = tokenService;
        this.securityProperties = securityProperties;
    }

    @Override
    public AuthResponse authenticate(AuthRequest request) {
        // 验证请求完整性
        AuthRequest.isValid(request);
        log.info("开始认证: authType={}, username={}", request.getAuthType(), request.getUsername());

        if (tokenService == null) {
            Ex.throwEx(AUTH_REQUEST_INVALID, "TokenService 未配置");
        }

        // 通过 TokenService 处理认证
        String token = processWithTokenService(request);
        // 存储用户会话信息
        storeUserSession(token, request);

        // 获取配置的 token 过期时间
        long expiration = getTokenTimeout();

        // 自动存储业务权限数据（如果提供）
        storePermissionDataIfProvided(token, request, expiration);

        return AuthResponse.of(token, null, expiration);
    }

    @Override
    public AuthResponse renewToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            Ex.throwEx(AUTH_TOKEN_NULL);
        }

        if (tokenService == null) {
            Ex.throwEx(AUTH_REQUEST_INVALID, "TokenService 未配置");
        }

        log.info("开始Token续期");
        // 通过 TokenService 续期
        boolean success = tokenService.renewToken(token, 7200L);
        if (!success) {
            Ex.throwEx(AUTH_TOKEN_INVALID, "Token续期失败");
        }
        log.info("Token续期成功");
        return AuthResponse.of(token, null, 7200L);
    }

    @Override
    public UserContext getCurrentUser() {
        // 从 UserContext 获取（由 UserContextInterceptor 设置）
        UserContext userContext = UserContext.getCurrentUser();
        if (userContext != null) {
            return userContext;
        }

        // 如果 UserContext 中没有，尝试从请求中获取 token（需要从请求头或请求属性中获取）
        // 这里暂时返回 null，因为 token 获取需要依赖 HttpServletRequest
        // 业务代码应该直接使用 UserContext.getCurrentUser()
        log.debug("无法获取当前用户信息：UserContext 未设置");
        return null;
    }

    @Override
    public Result<Void> logout() {
        try {
            // 从 UserContext 获取 token
            UserContext userContext = UserContext.getCurrentUser();
            if (userContext != null && tokenService != null) {
                // 需要从请求中获取 token，这里暂时无法直接获取
                // 业务代码应该直接调用 TokenService.revokeToken(token)
                log.warn("logout() 方法需要 token 参数，请使用 TokenService.revokeToken(token)");
            }
            log.info("用户登出成功");
            return Result.success(null);
        } catch (Exception e) {
            log.error("用户登出失败", e);
            return Result.error("登出失败: " + e.getMessage());
        }
    }

    /**
     * 通过 TokenService 处理不同类型的认证
     *
     * @param request 认证请求
     * @return 生成的Token
     */
    private String processWithTokenService(AuthRequest request) {
        return switch (request.getAuthType()) {
            case USERNAME_PASSWORD -> {
                // 用户名密码登录，生成 token
                // 不包含 permissions，避免 session 过大，permissions 单独存储
                UserContext userContext = UserContext.builder()
                        .userId(request.getUserId())
                        .account(request.getUsername())
                        .realName(request.getRealName())
                        .mobile(request.getMobile())
                        .email(request.getEmail())
                        .avatar(request.getAvatar())
                        .build();
                long expiration = getTokenTimeout();
                yield tokenService.generateToken(request.getUserId(), userContext, expiration);
            }
            case OAUTH2_AUTHORIZATION_CODE, OAUTH2_CLIENT_CREDENTIALS -> {
                // OAuth2.0登录，生成 token
                // 不包含 permissions，避免 session 过大，permissions 单独存储
                UserContext userContext = UserContext.builder()
                        .userId(request.getUserId())
                        .account(request.getUsername())
                        .build();
                long expiration = getTokenTimeout();
                yield tokenService.generateToken(request.getUserId(), userContext, expiration);
            }
            case TOKEN_VALIDATION -> {
                // Token验证
                String token = request.getTokenAuth().getToken();
                if (!tokenService.validateToken(token)) {
                    Ex.throwEx(AUTH_TOKEN_INVALID, "Token无效");
                }
                yield token;
            }
            case REFRESH_TOKEN -> {
                // 刷新Token（续期）
                String refreshToken = request.getRefreshTokenAuth().getRefreshToken();
                if (!tokenService.validateToken(refreshToken)) {
                    Ex.throwEx(AUTH_TOKEN_INVALID, "刷新Token无效");
                }
                // 续期 token
                tokenService.renewToken(refreshToken, 7200L);
                yield refreshToken;
            }
            default -> throw new IllegalArgumentException("不支持的认证类型: " + request.getAuthType());
        };
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

        // 构建 UserContext（不包含 permissions 和 systemMenuTree，避免 session 过大）
        // permissions 和 systemMenuTree 单独存储
        UserContext userContext = UserContext.builder()
                .userId(request.getUserId())
                .account(request.getUsername())
                .realName(request.getRealName())
                .email(request.getEmail())
                .mobile(request.getMobile())
                .avatar(request.getAvatar())
                .build();

        // 获取配置的 token 过期时间
        long tokenTimeout = getTokenTimeout();

        // 1. 存储用户会话（包含完整用户信息）
        userSessionService.storeUserSession(token, userContext, tokenTimeout);

        // 2. 单独存储权限（供 UserSessionService 查询使用）
        if (request.getPermissions() != null && !request.getPermissions().isEmpty()) {
            userSessionService.storeUserPermissions(token, request.getPermissions(), tokenTimeout);
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

    @Override
    public <T> void storeUserSystemMenuTree(String token, List<T> systemMenuTree, long expiration) {
        if (userSessionService == null) {
            log.warn("UserSessionService 未配置，跳过系统菜单树存储");
            return;
        }

        try {
            if (systemMenuTree != null && !systemMenuTree.isEmpty()) {
                userSessionService.storeUserSystemMenuTree(token, systemMenuTree, expiration);
                log.debug("Stored user system menu tree for token: {}, count: {}", token, systemMenuTree.size());
            }
            log.info("Stored user system menu tree for token: {}", token);
        } catch (Exception e) {
            log.error("存储用户系统菜单树失败: token={}", token, e);
            throw new RuntimeException("存储用户系统菜单树失败: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> Result<List<T>> getUserSystemMenuTree(String token, Class<T> clazz) {
        if (userSessionService == null) {
            return Result.error("UserSessionService 未配置");
        }

        try {
            List<T> systemMenuTree = userSessionService.getUserSystemMenuTree(token, clazz);
            if (systemMenuTree == null) {
                systemMenuTree = Collections.emptyList();
            }
            return Result.success(systemMenuTree);
        } catch (Exception e) {
            log.error("获取用户系统菜单树失败: token={}, clazz={}", token, clazz.getName(), e);
            return Result.error("获取用户系统菜单树失败: " + e.getMessage());
        }
    }

    @Override
    public <T> Result<T> getUserInfo(String token, AuthenticationService.UserInfoBuilder<T> builder) {
        if (userSessionService == null) {
            return Result.error("UserSessionService 未配置");
        }

        if (token == null || token.trim().isEmpty()) {
            return Result.error("Token 不能为空");
        }

        try {
            // 1. 从缓存获取用户上下文
            UserContext userContext = userSessionService.getUserSession(token);
            if (userContext == null) {
                return Result.error("用户未登录或 Token 已过期");
            }

            // 2. 从缓存获取权限列表
            List<String> permissions = userSessionService.getUserPermissions(token);
            if (permissions == null) {
                permissions = Collections.emptyList();
            }

            // 3. 从缓存获取系统菜单树
            List<?> systemMenuTree = userSessionService.getUserSystemMenuTree(token, Object.class);
            if (systemMenuTree == null) {
                systemMenuTree = Collections.emptyList();
            }

            // 4. 使用构建器组装用户信息
            T userInfo = builder.build(userContext, permissions, systemMenuTree);
            return Result.success(userInfo);
        } catch (Exception e) {
            log.error("获取用户信息失败: token={}", token, e);
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 自动存储业务权限数据（如果提供）
     * 在认证成功后，如果 AuthRequest 中包含 PermissionData，则自动存储到缓存
     *
     * @param token   Token值
     * @param request 认证请求
     * @param expiration 过期时间（秒）
     */
    @SuppressWarnings("unchecked")
    private void storePermissionDataIfProvided(String token, AuthRequest request, long expiration) {
        if (request.getPermissionData() == null) {
            return;
        }

        AuthRequest.PermissionData permissionData = request.getPermissionData();

        // 存储资源列表（只缓存 resources，不缓存 systems）
        // 注意：菜单信息已包含在 systemMenuTree 中，不需要单独存储
        if (permissionData.getResources() != null && !permissionData.getResources().isEmpty()) {
            storeUserPermissionData(token,
                    null, // 不再单独存储菜单，菜单信息在 systemMenuTree 中
                    (List<Object>) permissionData.getResources(),
                    null, // 不缓存 systems
                    expiration);
        }

        // 存储系统菜单树（单独存储，不放在 user:session 中，避免 session 过大）
        if (permissionData.getSystemMenuTree() != null && !permissionData.getSystemMenuTree().isEmpty()) {
            storeUserSystemMenuTree(token, (List<Object>) permissionData.getSystemMenuTree(), expiration);
        }
        
        // 注意：permissionTree 不进行缓存，如果需要可以从 systemMenuTree 和 resources 推导
    }

    /**
     * 获取配置的 token 过期时间
     * 
     * @return token 过期时间（秒），如果未配置则返回默认值 7200 秒（2 小时）
     */
    private long getTokenTimeout() {
        if (securityProperties != null 
                && securityProperties.getToken() != null 
                && securityProperties.getToken().getTimeout() > 0) {
            return securityProperties.getToken().getTimeout();
        }
        // 默认值：2 小时（7200 秒）
        return 2 * 60 * 60L;
    }
} 