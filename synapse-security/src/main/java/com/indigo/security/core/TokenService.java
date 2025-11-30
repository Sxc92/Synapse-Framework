package com.indigo.security.core;

import com.indigo.cache.session.UserSessionService;
import com.indigo.core.context.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Token 服务
 * 负责 Token 的生成、验证、存储和撤销
 * 使用 UUID 生成 token，通过 UserSessionService 存储到 Redis
 * 
 * <p><b>设计说明：</b>
 * <ul>
 *   <li>Token 生成：使用 UUID.randomUUID() 生成唯一标识</li>
 *   <li>Token 存储：通过 UserSessionService 存储到 Redis</li>
 *   <li>Token 验证：通过 UserSessionService 检查 token 是否存在</li>
 *   <li>用户信息：从 Redis 中获取 UserContext</li>
 * </ul>
 * 
 * <p><b>与 TokenManager 的关系：</b>
 * TokenManager 后续将使用 TokenService 替代 Sa-Token 的功能
 * 
 * @author 史偕成
 * @date 2025/01/XX
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(UserSessionService.class)
public class TokenService {

    /**
     * 默认 Token 过期时间（秒）：2 小时
     */
    private static final long DEFAULT_TOKEN_EXPIRATION = 7200L;

    private final UserSessionService userSessionService;

    /**
     * 生成 Token 并存储用户会话
     * 
     * @param userId      用户ID
     * @param userContext 用户上下文
     * @param expiration  过期时间（秒）
     * @return 生成的 token
     */
    public String generateToken(String userId, UserContext userContext, long expiration) {
        validateGenerateTokenParams(userId, userContext);
        expiration = normalizeExpiration(expiration, userId);

        try {
            String token = createToken();
            log.debug("生成Token: userId={}, token={}, expiration={}", userId, token, expiration);

            storeTokenData(token, userId, userContext, expiration);

            log.info("Token生成并存储成功: userId={}, token={}, expiration={}", userId, token, expiration);
            return token;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成Token失败: userId={}", userId, e);
            throw new RuntimeException("Token生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证生成 Token 的参数
     */
    private void validateGenerateTokenParams(String userId, UserContext userContext) {
        if (userId == null || userId.trim().isEmpty()) {
            log.error("生成Token失败: userId为空");
            throw new IllegalArgumentException("用户ID不能为空");
        }

        if (userContext == null) {
            log.error("生成Token失败: userContext为空");
            throw new IllegalArgumentException("用户上下文不能为空");
        }
        }

    /**
     * 规范化过期时间
     */
    private long normalizeExpiration(long expiration, String userId) {
        if (expiration <= 0) {
            log.warn("Token过期时间异常，使用默认值{}秒: userId={}", DEFAULT_TOKEN_EXPIRATION, userId);
            return DEFAULT_TOKEN_EXPIRATION;
        }
        return expiration;
        }

    /**
     * 创建 Token（UUID）
     */
    private String createToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 存储 Token 相关数据到 Redis
     * 
     * <p><b>注意：</b>permissions 不再存储在 UserContext 中，而是单独存储。
     * 权限数据应该通过 {@link com.indigo.security.service.DefaultAuthenticationService#storePermissionDataIfProvided}
     * 方法单独存储。
     */
    private void storeTokenData(String token, String userId, UserContext userContext, long expiration) {
            // 存储用户会话到 Redis（不包含 permissions 和 systemMenuTree，避免 session 过大）
            userSessionService.storeUserSession(token, userContext, expiration);
            
            // 存储 token 到 Redis（用于快速验证 token 是否存在）
            userSessionService.storeToken(token, userId, expiration);
            
            // 注意：permissions 不再从 UserContext 中获取，而是通过 DefaultAuthenticationService.storePermissionDataIfProvided 单独存储
    }

    /**
     * 验证 Token 是否有效
     * 
     * @param token Token值
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        if (isTokenEmpty(token)) {
            return false;
        }

        try {
            boolean exists = userSessionService.hasUserSession(token);
            log.debug("Token验证{}: token={}", exists ? "成功" : "失败", token);
            return exists;
        } catch (Exception e) {
            log.error("验证Token异常: token={}", token, e);
            return false;
        }
    }

    /**
     * 从 Token 中获取用户ID
     * 
     * @param token Token值
     * @return 用户ID，如果 token 无效则返回 null
     */
    public String getUserIdFromToken(String token) {
        if (isTokenEmpty(token)) {
            return null;
        }

        try {
            UserContext userContext = userSessionService.getUserSession(token);
            return userContext != null ? userContext.getUserId() : null;
        } catch (Exception e) {
            log.debug("从Token获取用户ID异常: token={}", token, e);
            return null;
        }
    }

    /**
     * 获取 Token 对应的用户上下文
     * 
     * @param token Token值
     * @return 用户上下文，如果 token 无效则返回 null
     */
    public UserContext getUserContext(String token) {
        if (isTokenEmpty(token)) {
            return null;
        }

        try {
            return userSessionService.getUserSession(token);
        } catch (Exception e) {
            log.error("获取用户上下文失败: token={}", token, e);
            return null;
        }
    }

    /**
     * 续期 Token
     * 延长 Token 的过期时间
     * 
     * @param token     Token值
     * @param duration 续期时间（秒）
     * @return 是否续期成功
     */
    public boolean renewToken(String token, long duration) {
        if (isTokenEmpty(token)) {
            return false;
        }

        if (duration <= 0) {
            log.warn("续期时间异常: duration={}", duration);
            return false;
        }

        try {
            if (!validateToken(token)) {
                log.warn("Token续期失败: token无效, token={}", token);
                return false;
            }

            userSessionService.extendUserSession(token, duration);
            log.info("Token续期成功: token={}, duration={}", token, duration);
            return true;
        } catch (Exception e) {
            log.error("Token续期失败: token={}, duration={}", token, duration, e);
            return false;
        }
    }

    /**
     * 撤销 Token
     * 清除 Redis 中的会话信息和 token
     * 
     * @param token Token值
     */
    public void revokeToken(String token) {
        if (isTokenEmpty(token)) {
            log.info("撤销Token失败: token为空");
            return;
        }

        try {
            // 清除 Redis 中的会话信息（包括会话、权限、菜单、资源等）
            userSessionService.removeUserSession(token);
            // 清除 Redis 中的 token
            userSessionService.removeToken(token);
            
            log.info("Token撤销成功: token={}", token);
        } catch (Exception e) {
            log.error("撤销Token异常: token={}", token, e);
        }
    }

    /**
     * 获取 Token 剩余时间
     * 
     * @param token Token值
     * @return 剩余时间（秒），如果 token 不存在返回 -1
     */
    public long getTokenRemainingTime(String token) {
        if (isTokenEmpty(token)) {
            return -1;
        }

        try {
            return userSessionService.getTokenRemainingTime(token);
        } catch (Exception e) {
            log.error("获取Token剩余时间失败: token={}", token, e);
            return -1;
        }
    }

    /**
     * 检查 Token 是否为空
     *
     * @param token Token值
     * @return 是否为空
     */
    private boolean isTokenEmpty(String token) {
        return token == null || token.trim().isEmpty();
    }
}

