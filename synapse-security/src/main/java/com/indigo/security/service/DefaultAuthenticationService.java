package com.indigo.security.service;

import com.indigo.core.entity.Result;
import com.indigo.security.core.AuthenticationService;
import com.indigo.security.config.SecurityProperties;
import com.indigo.security.factory.AuthenticationStrategyFactory;
import com.indigo.security.model.AuthRequest;
import com.indigo.security.model.AuthResponse;
import com.indigo.security.strategy.AuthenticationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 简化的认证服务实现
 * 使用策略模式处理不同类型的认证请求
 *
 * @author 史偕成
 * @date 2024/12/19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultAuthenticationService implements AuthenticationService {

    private final AuthenticationStrategyFactory strategyFactory;
    private final SecurityProperties securityProperties;

    @Override
    public Result<AuthResponse> authenticate(AuthRequest request) {
        try {
            if (request == null || !StringUtils.hasText(request.getUsername())) {
                return Result.error("用户名不能为空");
            }

            // 动态获取策略类型，支持从请求中指定或使用默认策略
            String strategyType = getStrategyType(request);
            AuthenticationStrategy strategy = strategyFactory.getStrategy(strategyType);
            
            return strategy.authenticate(request);
            
        } catch (Exception e) {
            log.error("认证失败", e);
            return Result.error("认证失败: " + e.getMessage());
        }
    }

    @Override
    public Result<AuthResponse> renewToken(String token) {
        try {
            if (!StringUtils.hasText(token)) {
                return Result.error("Token不能为空");
            }

            // 对于token续期，需要根据token本身推断策略类型
            // 这里可以通过token格式或者配置来确定策略类型
            String strategyType = inferStrategyTypeFromToken(token);
            AuthenticationStrategy strategy = strategyFactory.getStrategy(strategyType);
            
            return strategy.renewToken(token);
            
        } catch (Exception e) {
            log.error("Token续期失败", e);
            return Result.error("续期失败: " + e.getMessage());
        }
    }

    /**
     * 获取认证策略类型
     * 优先使用请求中指定的策略类型，否则使用默认策略
     *
     * @param request 认证请求
     * @return 策略类型
     */
    private String getStrategyType(AuthRequest request) {
        // 优先使用请求中指定的策略类型（如果启用）
        if (securityProperties.getAuthentication().isEnableRequestStrategyOverride() 
            && StringUtils.hasText(request.getStrategyType())) {
            String requestedStrategy = request.getStrategyType();
            if (strategyFactory.hasStrategy(requestedStrategy)) {
                log.debug("使用请求指定的认证策略: {}", requestedStrategy);
                return requestedStrategy;
            } else {
                log.warn("请求指定的认证策略不存在: {}, 使用默认策略", requestedStrategy);
            }
        }
        
        // 根据认证类型推断策略类型（如果启用）
        if (securityProperties.getAuthentication().isEnableStrategyInference() 
            && request.getAuthType() != null) {
            String inferredStrategy = inferStrategyTypeFromAuthType(request.getAuthType());
            if (strategyFactory.hasStrategy(inferredStrategy)) {
                log.debug("根据认证类型推断策略: {} -> {}", request.getAuthType(), inferredStrategy);
                return inferredStrategy;
            }
        }
        
        // 使用配置的默认策略
        String defaultStrategy = getDefaultStrategyType();
        log.debug("使用默认认证策略: {}", defaultStrategy);
        return defaultStrategy;
    }

    /**
     * 根据认证类型推断策略类型
     *
     * @param authType 认证类型
     * @return 策略类型
     */
    private String inferStrategyTypeFromAuthType(AuthRequest.AuthType authType) {
        return switch (authType) {
            case USERNAME_PASSWORD -> "satoken";
            case TOKEN_VALIDATION -> "satoken";
            case OAUTH2_AUTHORIZATION_CODE -> "oauth2";
            case OAUTH2_CLIENT_CREDENTIALS -> "oauth2";
            case REFRESH_TOKEN -> "satoken";
        };
    }

    /**
     * 根据token推断策略类型
     * 这里可以通过token格式、前缀等来推断策略类型
     *
     * @param token 访问令牌
     * @return 策略类型
     */
    private String inferStrategyTypeFromToken(String token) {
        // 这里可以根据token的格式来推断策略类型
        // 例如：JWT token通常以 "eyJ" 开头，Sa-Token可能有特定格式
        
        // 简单实现：检查token格式
        if (token.startsWith("eyJ")) {
            // JWT token格式
            return "jwt";
        } else if (token.contains(".") && token.length() > 32) {
            // 可能是Sa-Token格式
            return "satoken";
        } else {
            // 默认使用Sa-Token
            return "satoken";
        }
    }

    /**
     * 获取默认认证策略类型
     * 可以通过配置文件或环境变量来配置
     *
     * @return 默认策略类型
     */
    private String getDefaultStrategyType() {
        // 这里可以从配置文件、环境变量等获取默认策略类型
        // 暂时硬编码为 "satoken"，后续可以通过配置注入
        return "satoken";
    }
} 