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
 * Sa-Token内部认证策略实现
 * 使用Sa-Token进行内部用户认证，适用于用户名密码登录
 * 角色和权限信息由业务模块传入，通过UserSessionService进行缓存管理
 *
 * @author 史偕成
 * @date 2025/01/08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SaTokenAuthenticationStrategy implements AuthenticationStrategy {

    private final TokenManager tokenManager;
    private final UserSessionService userSessionService;

    @Override
    public String getStrategyType() {
        return "satoken";
    }

    @Override
    public Result<AuthResponse> authenticate(AuthRequest request) {
        try {
            log.info("开始Sa-Token内部认证: username={}", request.getUsername());
            
            // 验证业务模块传入的用户信息
            if (request.getRoles() == null || request.getRoles().isEmpty()) {
                log.warn("用户角色信息缺失: username={}", request.getUsername());
                return Result.error("用户角色信息缺失");
            }
            
            if (request.getPermissions() == null || request.getPermissions().isEmpty()) {
                log.warn("用户权限信息缺失: username={}", request.getUsername());
                return Result.error("用户权限信息缺失");
            }
            
            // 创建用户上下文（使用业务模块传入的角色和权限）
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
                
            log.info("Sa-Token内部认证成功: username={}, roles={}, permissions={}", 
                    request.getUsername(), request.getRoles(), request.getPermissions());
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("Sa-Token内部认证失败: username={}", request.getUsername(), e);
            return Result.error("内部认证失败: " + e.getMessage());
        }
    }

    @Override
    public Result<AuthResponse> renewToken(String token) {
        try {
            log.info("开始Sa-Token Token续期");
            
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
            
            log.info("Sa-Token Token续期成功: userId={}", userContext.getUserId());
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("Sa-Token Token续期失败", e);
            return Result.error("Token续期失败: " + e.getMessage());
        }
    }
} 