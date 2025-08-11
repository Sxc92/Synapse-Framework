package com.indigo.cache.session.impl;

import com.indigo.cache.core.CacheService;
import com.indigo.cache.manager.CacheKeyGenerator;
import com.indigo.cache.session.SessionManager;
import com.indigo.core.context.UserContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认会话管理器实现
 * 基于缓存服务的会话管理，包括会话、Token和会话数据管理
 *
 * @author 史偕成
 * @date 2024/12/19
 */
@Slf4j
public class DefaultSessionManager implements SessionManager {

    private final CacheService cacheService;
    private final CacheKeyGenerator keyGenerator;

    public DefaultSessionManager(CacheService cacheService, CacheKeyGenerator keyGenerator) {
        this.cacheService = cacheService;
        this.keyGenerator = keyGenerator;
    }

    // ========== 用户会话管理 ==========

    @Override
    public void storeUserSession(String token, UserContext userContext, long expiration) {
        String sessionKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "session", token);
        cacheService.setObject(sessionKey, userContext, expiration);
        log.info("Stored user session for token: {}", token);
    }

    @Override
    public UserContext getUserSession(String token) {
        String sessionKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "session", token);
        UserContext userContext = cacheService.getObject(sessionKey, UserContext.class);
        if (userContext != null) {
            userContext.setToken(token);
            // 更新最后访问时间
            updateLastAccessTime(userContext);
        }
        return userContext;
    }

    @Override
    public boolean hasUserSession(String token) {
        String sessionKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "session", token);
        return cacheService.exists(sessionKey);
    }

    @Override
    public void removeUserSession(String token) {
        String sessionKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "session", token);
        cacheService.delete(sessionKey);
        log.info("Removed user session for token: {}", token);
    }

    @Override
    public void extendUserSession(String token, long expiration) {
        String sessionKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "session", token);
        cacheService.resetExpiry(sessionKey, expiration);
        log.info("Extended user session for token: {}", token);
    }

    @Override
    public long getTokenRemainingTime(String token) {
        String sessionKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "session", token);
        return cacheService.getTimeToLive(sessionKey);
    }

    @Override
    public boolean renewToken(String token, long duration) {
        try {
            String sessionKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "session", token);
            UserContext userContext = cacheService.getObject(sessionKey, UserContext.class);
            if (userContext != null) {
                cacheService.setObject(sessionKey, userContext, duration);
                log.info("Renewed token: {}", token);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to renew token: {}", token, e);
            return false;
        }
    }

    // ========== Token基础管理 ==========
    
    @Override
    public void storeToken(String token, String userId, long expireSeconds) {
        String tokenKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "token", token);
        cacheService.set(tokenKey, userId, expireSeconds);
        log.info("Stored token: {} for user: {}", token, userId);
    }
    
    @Override
    public String validateToken(String token) {
        String tokenKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "token", token);
        Object result = cacheService.getValue(tokenKey);
        return result != null ? result.toString() : null;
    }
    
    @Override
    public boolean refreshToken(String token, long expireSeconds) {
        String tokenKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "token", token);
        boolean result = cacheService.expire(tokenKey, expireSeconds);
        return result;
    }
    
    @Override
    public void removeToken(String token) {
        String tokenKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "token", token);
        cacheService.delete(tokenKey);
        log.info("Removed token: {}", token);
    }
    
    @Override
    public boolean tokenExists(String token) {
        String tokenKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "token", token);
        boolean result = cacheService.hasKey(tokenKey);
        return result;
    }
    
    @Override
    public long getTokenTtl(String token) {
        String tokenKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "token", token);
        long result = cacheService.getExpire(tokenKey);
        return result;
    }

    // ========== 会话数据管理 ==========
    
    @Override
    public void storeUserSessionData(String userId, Object sessionData, long expireSeconds) {
        String dataKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "data", userId);
        cacheService.setObject(dataKey, sessionData, expireSeconds);
        log.info("Stored session data for user: {}", userId);
    }
    
    @Override
    public <T> T getUserSessionData(String userId, Class<T> clazz) {
        String dataKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "data", userId);
        return cacheService.getObject(dataKey, clazz);
    }
    
    @Override
    public void removeUserSessionData(String userId) {
        String dataKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "data", userId);
        cacheService.delete(dataKey);
        log.info("Removed session data for user: {}", userId);
    }



    /**
     * 更新用户最后访问时间
     *
     * @param userContext 用户上下文
     */
    private void updateLastAccessTime(UserContext userContext) {
        userContext.setLastAccessTime(System.currentTimeMillis());
        String sessionKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "session", userContext.getToken());
        // 获取当前过期时间
        long expireTime = cacheService.getTimeToLive(sessionKey);
        if (expireTime > 0) {
            cacheService.setObject(sessionKey, userContext, expireTime);
        }
    }
} 