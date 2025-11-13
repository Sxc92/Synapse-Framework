package com.indigo.security.core;

import com.indigo.cache.session.UserSessionService;
import com.indigo.core.context.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Token管理服务
 * 基于 TokenService 提供基础的令牌管理功能
 * 
 * <p><b>注意：</b>此类是为了保持向后兼容性而保留的。
 * 新代码应该直接使用 {@link TokenService}。
 *
 * @author 史偕成
 * @date 2025/01/08
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean({UserSessionService.class, TokenService.class})
public class TokenManager {

    private final TokenService tokenService;

    /**
     * 执行用户登录
     * 使用 TokenService 进行登录，同时存储用户上下文到Redis
     *
     * @param userId      用户ID
     * @param userContext 用户上下文信息
     * @return Token
     */
    public String login(Object userId, UserContext userContext) {
        if (userId == null) {
            log.error("用户登录失败: userId为空");
            throw new IllegalArgumentException("用户ID不能为空");
        }

        try {
            log.info("开始用户登录流程: userId={}", userId);

            // 使用 TokenService 生成 token
            String token = tokenService.generateToken(userId.toString(), userContext, 7200L);

            log.info("用户登录成功: userId={}, token={}, timeout=7200", userId, token);
            return token;

        } catch (Exception e) {
            log.error("用户登录失败: userId={}", userId, e);
            throw new RuntimeException("登录失败: " + e.getMessage(), e);
        }
    }

    /**
     * 撤销Token
     * 清除Redis中的会话信息
     *
     * @param token Token值
     */
    public void revokeToken(String token) {
        tokenService.revokeToken(token);
    }

    /**
     * 从Token中获取用户ID
     * 委托给 TokenService 处理
     *
     * @param token Token值
     * @return 用户ID
     */
    public String getUserIdFromToken(String token) {
        return tokenService.getUserIdFromToken(token);
    }

    /**
     * 获取当前Token
     * 注意：此方法需要从请求中获取 token，建议直接使用 UserContextInterceptor 设置的 token
     *
     * @return Token值
     */
    public String getCurrentToken() {
        // 无法直接获取，需要从请求中获取
        // 建议业务代码从请求头或 UserContextInterceptor 设置的请求属性中获取
        log.warn("getCurrentToken() 方法无法直接获取 token，请从请求中获取");
        return null;
    }

    /**
     * 验证Token是否有效
     * 委托给 TokenService 处理
     *
     * @param token Token值
     * @return 是否有效
     */
    public boolean isTokenValid(String token) {
        return tokenService.validateToken(token);
    }

    /**
     * 续期Token
     * 延长Token的过期时间
     *
     * @param token Token值
     * @param duration 续期时间（秒）
     * @return 是否续期成功
     */
    public boolean renewToken(String token, long duration) {
        return tokenService.renewToken(token, duration);
    }

    /**
     * 获取Token对应的用户上下文
     * 从Redis中获取用户会话信息
     *
     * @param token Token值
     * @return 用户上下文
     */
    public UserContext getUserContext(String token) {
        return tokenService.getUserContext(token);
    }
} 