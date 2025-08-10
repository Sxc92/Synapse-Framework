package com.indigo.security.strategy;

import com.indigo.cache.session.UserSessionService;
import com.indigo.core.context.UserContext;
import com.indigo.core.entity.Result;
import com.indigo.security.core.TokenManager;
import com.indigo.security.model.AuthRequest;
import com.indigo.security.model.AuthResponse;
import com.indigo.security.model.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OAuth2.0认证策略实现
 * 使用Sa-Token的OAuth2模块进行第三方认证，适用于第三方登录
 * 角色和权限信息由业务模块传入，通过UserSessionService进行缓存管理
 *
 * @author 史偕成
 * @date 2024/01/08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationStrategy implements AuthenticationStrategy {

    private final TokenManager tokenManager;
    private final UserSessionService userSessionService;

    @Override
    public String getStrategyType() {
        return "oauth2";
    }

    @Override
    public Result<AuthResponse> authenticate(AuthRequest request) {
        try {
            log.info("开始OAuth2.0认证: username={}, provider={}", request.getUsername(), request.getProvider());
            
            // OAuth2.0认证流程：验证第三方token，获取用户信息
            // 这里应该调用第三方OAuth2.0服务验证token并获取用户信息
            // 为了演示，我们简化处理，实际应该调用第三方API
            
            // 验证业务模块传入的用户信息
            if (request.getRoles() == null || request.getRoles().isEmpty()) {
                log.warn("用户角色信息缺失: username={}", request.getUsername());
                return Result.error("用户角色信息缺失");
            }
            
            if (request.getPermissions() == null || request.getPermissions().isEmpty()) {
                log.warn("用户权限信息缺失: username={}", request.getUsername());
                return Result.error("用户权限信息缺失");
            }
            
            // 创建用户上下文（基于OAuth2.0返回的用户信息和业务模块传入的角色权限）
            UserContext userContext = UserContext.builder()
                .userId(request.getUserId())
                .username(request.getUsername())
                .loginTime(System.currentTimeMillis())
                .roles(request.getRoles())
                .permissions(request.getPermissions())
                .build();
            
            // 通过TokenManager进行登录认证
            String token = tokenManager.login(request.getUsername(), userContext);
            
            // 获取token超时时间
            long tokenTimeout = 7200L; // 2小时
            
            // 通过UserSessionService存储用户会话、角色和权限信息
            userSessionService.storeUserSession(token, userContext, tokenTimeout);
            userSessionService.storeUserRoles(token, request.getRoles(), tokenTimeout);
            userSessionService.storeUserPermissions(token, request.getPermissions(), tokenTimeout);
            
            // 转换为UserPrincipal
            UserPrincipal userPrincipal = UserPrincipal.builder()
                .userId(userContext.getUserId())
                .username(userContext.getUsername())
                .roles(userContext.getRoles())
                .permissions(userContext.getPermissions())
                .build();
            
            // 构建认证响应
            AuthResponse response = AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(tokenTimeout)
                .userPrincipal(userPrincipal)
                .roles(userContext.getRoles())
                .permissions(userContext.getPermissions())
                .tokenCreatedAt(LocalDateTime.now())
                .build();
                
            log.info("OAuth2.0认证成功: username={}, provider={}, roles={}, permissions={}", 
                    request.getUsername(), request.getProvider(), request.getRoles(), request.getPermissions());
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("OAuth2.0认证失败: username={}, provider={}", request.getUsername(), request.getProvider(), e);
            return Result.error("OAuth2.0认证失败: " + e.getMessage());
        }
    }

    @Override
    public Result<AuthResponse> renewToken(String token) {
        try {
            log.info("开始OAuth2.0 Token续期");
            
            // 通过UserSessionService获取用户会话信息
            UserContext userContext = userSessionService.getUserSession(token);
            if (userContext == null) {
                return Result.error("用户会话已失效");
            }
            
            // 通过TokenManager验证token
            if (!tokenManager.isTokenValid(token)) {
                return Result.error("Token无效");
            }
            
            // 通过UserSessionService续期token
            boolean renewed = userSessionService.renewToken(token, 7200L);
            if (!renewed) {
                return Result.error("Token续期失败");
            }
            
            // 获取最新的角色和权限信息
            List<String> roles = userSessionService.getUserRoles(token);
            List<String> permissions = userSessionService.getUserPermissions(token);
            
            // 转换为UserPrincipal
            UserPrincipal userPrincipal = UserPrincipal.builder()
                .userId(userContext.getUserId())
                .username(userContext.getUsername())
                .roles(roles)
                .permissions(permissions)
                .build();
            
            // 构建认证响应
            AuthResponse response = AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(7200L)
                .userPrincipal(userPrincipal)
                .roles(roles)
                .permissions(permissions)
                .tokenCreatedAt(LocalDateTime.now())
                .build();
            
            log.info("OAuth2.0 Token续期成功: userId={}", userContext.getUserId());
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("OAuth2.0 Token续期失败", e);
            return Result.error("Token续期失败: " + e.getMessage());
        }
    }
} 