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
        if (userId == null || userId.trim().isEmpty()) {
            log.error("生成Token失败: userId为空");
            throw new IllegalArgumentException("用户ID不能为空");
        }

        if (userContext == null) {
            log.error("生成Token失败: userContext为空");
            throw new IllegalArgumentException("用户上下文不能为空");
        }

        if (expiration <= 0) {
            log.warn("Token过期时间异常，使用默认值7200秒: userId={}", userId);
            expiration = 7200;
        }

        try {
            // 生成 UUID token
            String token = UUID.randomUUID().toString().replace("-", "");
            log.debug("生成Token: userId={}, token={}, expiration={}", userId, token, expiration);

            // 存储用户会话到 Redis
            userSessionService.storeUserSession(token, userContext, expiration);
            
            // 存储 token 到 Redis（用于快速验证 token 是否存在）
            userSessionService.storeToken(token, userId, expiration);
            
            // 存储用户权限到 Redis
            if (userContext.getPermissions() != null && !userContext.getPermissions().isEmpty()) {
                userSessionService.storeUserPermissions(token, userContext.getPermissions(), expiration);
            }
            
            // 存储用户角色到 Redis
            if (userContext.getRoles() != null && !userContext.getRoles().isEmpty()) {
                userSessionService.storeUserRoles(token, userContext.getRoles(), expiration);
            }

            log.info("Token生成并存储成功: userId={}, token={}, expiration={}", userId, token, expiration);
            return token;

        } catch (Exception e) {
            log.error("生成Token失败: userId={}", userId, e);
            throw new RuntimeException("Token生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证 Token 是否有效
     * 
     * @param token Token值
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            // 检查 token 是否存在
            boolean exists = userSessionService.hasUserSession(token);
            if (exists) {
                log.debug("Token验证成功: token={}", token);
            } else {
                log.debug("Token验证失败: token不存在, token={}", token);
            }
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
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        try {
            UserContext userContext = userSessionService.getUserSession(token);
            if (userContext != null && userContext.getUserId() != null) {
                return userContext.getUserId();
            }
            return null;
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
        if (token == null || token.trim().isEmpty()) {
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
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        if (duration <= 0) {
            log.warn("续期时间异常: duration={}", duration);
            return false;
        }

        try {
            // 验证 token 是否有效
            if (!validateToken(token)) {
                log.warn("Token续期失败: token无效, token={}", token);
                return false;
            }

            // 延长 Redis 中的会话过期时间
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
        if (token == null || token.trim().isEmpty()) {
            log.info("撤销Token失败: token为空");
            return;
        }

        try {
            // 1. 清除 Redis 中的会话信息（包括会话、权限、角色、菜单、资源等）
            userSessionService.removeUserSession(token);
            
            // 2. 清除 Redis 中的 token（synapse:user:token:xxx）
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
        if (token == null || token.trim().isEmpty()) {
            return -1;
        }

        try {
            return userSessionService.getTokenRemainingTime(token);
        } catch (Exception e) {
            log.error("获取Token剩余时间失败: token={}", token, e);
            return -1;
        }
    }
}

