package com.indigo.security.core;

import cn.dev33.satoken.stp.StpUtil;
import com.indigo.cache.session.UserSessionService;
import com.indigo.core.context.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 简化的Token管理服务
 * 基于Sa-Token提供基础的令牌管理功能，避免重复造轮子
 *
 * @author 史偕成
 * @date 2025/01/08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenManager {

    private final UserSessionService userSessionService;

    /**
     * 执行用户登录
     * 使用Sa-Token进行登录，同时存储用户上下文到Redis
     *
     * @param userId      用户ID
     * @param userContext 用户上下文信息
     * @return Sa-Token
     */
    public String login(Object userId, UserContext userContext) {
        if (userId == null) {
            log.error("用户登录失败: userId为空");
            throw new IllegalArgumentException("用户ID不能为空");
        }

        try {
            log.info("开始用户登录流程: userId={}", userId);

            // 执行Sa-Token登录
            StpUtil.login(userId);
            String token = StpUtil.getTokenValue();

            if (token == null || token.trim().isEmpty()) {
                log.error("Sa-Token登录失败: 未能生成有效token");
                throw new RuntimeException("Token生成失败");
            }

            // 获取token超时时间
            long tokenTimeout = StpUtil.getTokenTimeout();
            if (tokenTimeout <= 0) {
                log.warn("Token超时时间异常，使用默认值7200秒: userId={}", userId);
                tokenTimeout = 7200;
            }

            // 存储用户信息到Redis
            if (userContext != null) {
                try {
                    userSessionService.storeUserSession(token, userContext, tokenTimeout);
                    userSessionService.storeUserPermissions(token, userContext.getPermissions(), tokenTimeout);
                    
                    // TODO: 操作审计 - 记录用户登录成功事件
                    // auditService.logUserAction(userId.toString(), "LOGIN_SUCCESS", "token_manager", "SUCCESS");
                    
                    log.info("用户登录成功: userId={}, token={}, timeout={}", userId, token, tokenTimeout);
                } catch (Exception e) {
                    log.error("存储用户会话失败，执行登出操作: userId={}, token={}", userId, token, e);
                    
                    // TODO: 操作审计 - 记录会话存储失败事件
                    // auditService.logUserAction(userId.toString(), "SESSION_STORE_FAILED", "token_manager", "FAILED: " + e.getMessage());
                    
                    try {
                        StpUtil.logout(userId);
                    } catch (Exception logoutEx) {
                        log.error("登出操作失败", logoutEx);
                    }
                    throw new RuntimeException("存储用户会话失败", e);
                }
            } else {
                log.warn("用户登录成功但无上下文信息: userId={}, token={}", userId, token);
                
                // TODO: 操作审计 - 记录登录成功但无上下文事件
                // auditService.logUserAction(userId.toString(), "LOGIN_SUCCESS_NO_CONTEXT", "token_manager", "WARNING");
            }

            return token;

        } catch (Exception e) {
            log.error("用户登录失败: userId={}", userId, e);
            throw new RuntimeException("登录失败: " + e.getMessage(), e);
        }
    }

    /**
     * 撤销Token
     * 清除Redis中的会话信息并执行Sa-Token登出
     *
     * @param token Token值
     */
    public void revokeToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.info("撤销Token失败: token为空");
            return;
        }

        try {
            String userId = getUserIdFromToken(token);
            if (userId != null) {
                // 先清除Redis中的会话信息
                userSessionService.removeUserSession(token);
                // 然后执行Sa-Token登出
                StpUtil.logout(userId);
                
                // TODO: 操作审计 - 记录Token撤销事件
                // auditService.logUserAction(userId, "TOKEN_REVOKED", "token_manager", "SUCCESS");
                
                log.info("Token撤销成功: userId={}, token={}", userId, token);
            } else {
                log.warn("Token撤销失败: 无法获取用户信息, token={}", token);
                
                // TODO: 操作审计 - 记录Token撤销失败事件
                // auditService.logUserAction("UNKNOWN", "TOKEN_REVOKE_FAILED", "token_manager", "FAILED: 无法获取用户信息");
            }
        } catch (Exception e) {
            log.error("撤销Token异常: token={}", token, e);
            
            // TODO: 操作审计 - 记录Token撤销异常事件
            // auditService.logUserAction("UNKNOWN", "TOKEN_REVOKE_EXCEPTION", "token_manager", "EXCEPTION: " + e.getMessage());
        }
    }

    /**
     * 从Token中获取用户ID
     * 委托给Sa-Token处理
     *
     * @param token Token值
     * @return 用户ID
     */
    public String getUserIdFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        try {
            Object loginId = StpUtil.stpLogic.getLoginIdByToken(token);
            return loginId != null ? loginId.toString() : null;
        } catch (Exception e) {
            log.info("获取用户ID异常: token={}", token, e);
            return null;
        }
    }

    /**
     * 获取当前Token
     * 委托给Sa-Token处理
     *
     * @return Token值
     */
    public String getCurrentToken() {
        try {
            return StpUtil.getTokenValue();
        } catch (Exception e) {
            log.info("获取当前Token异常", e);
            return null;
        }
    }

    /**
     * 验证Token是否有效
     * 委托给Sa-Token处理
     *
     * @param token Token值
     * @return 是否有效
     */
    public boolean isTokenValid(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            Object loginId = StpUtil.stpLogic.getLoginIdByToken(token);
            return loginId != null;
        } catch (Exception e) {
            log.info("验证Token异常: token={}", token, e);
            return false;
        }
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
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            // 验证token是否有效
            if (!isTokenValid(token)) {
                return false;
            }

            // 延长Redis中的会话过期时间
            userSessionService.extendUserSession(token, duration);
            log.info("Token续期成功: token={}, duration={}", token, duration);
            return true;
        } catch (Exception e) {
            log.error("Token续期失败: token={}, duration={}", token, duration, e);
            return false;
        }
    }

    /**
     * 获取Token对应的用户上下文
     * 从Redis中获取用户会话信息
     *
     * @param token Token值
     * @return 用户上下文
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
} 