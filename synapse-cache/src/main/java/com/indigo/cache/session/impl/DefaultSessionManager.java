package com.indigo.cache.session.impl;

import com.indigo.cache.core.CacheInvalidationService;
import com.indigo.cache.core.CacheInvalidationTracker;
import com.indigo.cache.core.CacheService;
import com.indigo.cache.infrastructure.CaffeineCacheManager;
import com.indigo.cache.manager.CacheKeyGenerator;
import com.indigo.cache.core.constants.SessionCacheConstants;
import com.indigo.cache.session.SessionManager;
import com.indigo.core.context.UserContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * 默认会话管理器实现
 * 基于缓存服务的会话管理，包括会话、Token和会话数据管理
 *
 * @author 史偕成
 * @date 2024/12/19
 */
@Slf4j
public class DefaultSessionManager implements SessionManager {

    /**
     * 本地缓存默认过期时间（秒）
     * 设置为 Redis 过期时间的 1/10，确保本地缓存先过期，避免数据不一致
     */
    private static final int LOCAL_CACHE_EXPIRE_SECONDS = 300; // 5分钟

    private final CacheService cacheService;
    private final CacheKeyGenerator keyGenerator;
    private final CaffeineCacheManager caffeineCacheManager;
    private final CacheInvalidationService cacheInvalidationService;
    private final CacheInvalidationTracker invalidationTracker;

    /**
     * 构造函数（兼容旧版本，CaffeineCacheManager 和 CacheInvalidationService 为可选）
     */
    public DefaultSessionManager(CacheService cacheService, CacheKeyGenerator keyGenerator) {
        this.cacheService = cacheService;
        this.keyGenerator = keyGenerator;
        this.caffeineCacheManager = null;
        this.cacheInvalidationService = null;
        this.invalidationTracker = null;
    }

    /**
     * 构造函数（支持 Caffeine 缓存）
     */
    public DefaultSessionManager(CacheService cacheService, 
                                 CacheKeyGenerator keyGenerator,
                                 CaffeineCacheManager caffeineCacheManager) {
        this(cacheService, keyGenerator, caffeineCacheManager, null, null);
    }

    /**
     * 构造函数（支持 Caffeine 缓存和失效通知）
     */
    public DefaultSessionManager(CacheService cacheService, 
                                 CacheKeyGenerator keyGenerator,
                                 CaffeineCacheManager caffeineCacheManager,
                                 CacheInvalidationService cacheInvalidationService) {
        this(cacheService, keyGenerator, caffeineCacheManager, cacheInvalidationService, null);
    }

    /**
     * 构造函数（支持 Caffeine 缓存、失效通知和失效追踪）
     */
    public DefaultSessionManager(CacheService cacheService, 
                                 CacheKeyGenerator keyGenerator,
                                 CaffeineCacheManager caffeineCacheManager,
                                 CacheInvalidationService cacheInvalidationService,
                                 CacheInvalidationTracker invalidationTracker) {
        this.cacheService = cacheService;
        this.keyGenerator = keyGenerator;
        this.caffeineCacheManager = caffeineCacheManager;
        this.cacheInvalidationService = cacheInvalidationService;
        this.invalidationTracker = invalidationTracker;
    }

    // ========== 用户会话管理 ==========

    @Override
    public void storeUserSession(String token, UserContext userContext, long expiration) {
        String sessionKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "session", token);
        
        // 1. 写入 Redis（主存储）
        cacheService.setObject(sessionKey, userContext, expiration);
        
        // 2. 写入 Caffeine 本地缓存（如果可用）
        if (caffeineCacheManager != null) {
            try {
                int localExpireSeconds = calculateLocalCacheExpire(expiration);
                caffeineCacheManager.put(SessionCacheConstants.CACHE_NAME_USER_SESSION, token, userContext, localExpireSeconds);
                log.debug("Stored user session to local cache: token={}, expireSeconds={}", token, localExpireSeconds);
            } catch (Exception e) {
                log.warn("Failed to store user session to local cache: token={}", token, e);
            }
        }
        
        // 3. 发布缓存失效通知（通知其他节点清除本地缓存）
        if (cacheInvalidationService != null) {
            cacheInvalidationService.publishInvalidation(SessionCacheConstants.CACHE_TYPE_USER_SESSION, token);
        }
        
        log.debug("Stored user session for token: {}, expiration: {} seconds", token, expiration);
    }

    @Override
    public UserContext getUserSession(String token) {
        // 1. 优先从 Caffeine 本地缓存读取
        if (caffeineCacheManager != null) {
            try {
                Optional<UserContext> cachedContext = caffeineCacheManager.get(SessionCacheConstants.CACHE_NAME_USER_SESSION, token);
                if (cachedContext.isPresent()) {
                    UserContext userContext = cachedContext.get();
                    log.debug("Retrieved user session from local cache: token={}", token);
                    return userContext;
                }
            } catch (Exception e) {
                log.warn("Failed to get user session from local cache: token={}", token, e);
            }
        }

        // 2. 从 Redis 读取
        String sessionKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "session", token);
        UserContext userContext = cacheService.getObject(sessionKey, UserContext.class);
        
        if (userContext != null) {
            // 3. 写入 Caffeine 本地缓存（如果可用）
            if (caffeineCacheManager != null) {
                try {
                    // 记录数据读取时间戳（用于并发控制）
                    long dataTimestamp = System.currentTimeMillis();
                    
                    // 检查是否在失效之后（防止写入旧数据）
                    if (invalidationTracker != null && 
                        invalidationTracker.isInvalidated(SessionCacheConstants.CACHE_TYPE_USER_SESSION, token, dataTimestamp)) {
                        log.debug("跳过写入本地缓存（数据已失效）: token={}", token);
                        return userContext;
                    }
                    
                    // 获取 Redis 中的剩余过期时间
                    long remainingTime = cacheService.getTimeToLive(sessionKey);
                    int localExpireSeconds = remainingTime > 0 
                        ? calculateLocalCacheExpire(remainingTime) 
                        : LOCAL_CACHE_EXPIRE_SECONDS;
                    caffeineCacheManager.put(SessionCacheConstants.CACHE_NAME_USER_SESSION, token, userContext, localExpireSeconds);
                    
                    // 清除失效记录（数据已成功更新）
                    if (invalidationTracker != null) {
                        invalidationTracker.clearInvalidation(SessionCacheConstants.CACHE_TYPE_USER_SESSION, token);
                    }
                    
                    log.debug("Stored user session to local cache after Redis read: token={}, expireSeconds={}", 
                            token, localExpireSeconds);
                } catch (Exception e) {
                    log.warn("Failed to store user session to local cache after Redis read: token={}", token, e);
                }
            }
            
            // 4. 检查并刷新过期时间（滑动过期）
            refreshSessionExpiry(sessionKey, token);
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
        
        // 1. 删除 Redis 中的会话
        cacheService.delete(sessionKey);
        
        // 2. 删除 Caffeine 本地缓存（如果可用）
        if (caffeineCacheManager != null) {
            try {
                caffeineCacheManager.remove(SessionCacheConstants.CACHE_NAME_USER_SESSION, token);
                log.debug("Removed user session from local cache: token={}", token);
            } catch (Exception e) {
                log.warn("Failed to remove user session from local cache: token={}", token, e);
            }
        }
        
        // 3. 发布缓存失效通知（通知其他节点清除本地缓存）
        if (cacheInvalidationService != null) {
            cacheInvalidationService.publishInvalidation(SessionCacheConstants.CACHE_TYPE_USER_SESSION, token);
        }
        
        log.debug("Removed user session for token: {}", token);
    }

    @Override
    public void extendUserSession(String token, long expiration) {
        String sessionKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "session", token);
        // 检查会话是否存在
        if (cacheService.exists(sessionKey)) {
            // 1. 更新 Redis 中的会话过期时间
            cacheService.resetExpiry(sessionKey, expiration);
            
            // 2. 同步更新本地缓存（如果存在）
            // 延长过期时间时，本地缓存的过期时间应该和 Redis 保持一致，确保数据同步
            if (caffeineCacheManager != null) {
                try {
                    Optional<UserContext> cachedContext = caffeineCacheManager.get(SessionCacheConstants.CACHE_NAME_USER_SESSION, token);
                    if (cachedContext.isPresent()) {
                        // 使用与 Redis 相同的过期时间（转换为 int，确保不超过 Integer.MAX_VALUE）
                        int localExpireSeconds = expiration > Integer.MAX_VALUE 
                                ? Integer.MAX_VALUE 
                                : (int) expiration;
                        caffeineCacheManager.put(SessionCacheConstants.CACHE_NAME_USER_SESSION, token, cachedContext.get(), localExpireSeconds);
                        log.debug("Updated local cache after session extension: token={}, localExpireSeconds={} (same as Redis)", 
                                token, localExpireSeconds);
                    }
                } catch (Exception e) {
                    log.warn("Failed to update local cache after session extension: token={}", token, e);
                }
            }
            
            log.debug("Extended user session for token: {}, new expiration: {} seconds", token, expiration);
        } else {
            log.warn("Failed to extend user session for token: {}, session may not exist", token);
        }
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
                // 1. 重新存储会话到 Redis，使用新的过期时间
                cacheService.setObject(sessionKey, userContext, duration);
                
                // 2. 同时刷新 token 的过期时间（如果存在）
                String tokenKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "token", token);
                if (cacheService.hasKey(tokenKey)) {
                    cacheService.expire(tokenKey, duration);
                }
                
                // 3. 更新本地缓存（session 和 token）
                // 刷新时，本地缓存的过期时间应该和 Redis 保持一致，确保数据同步
                if (caffeineCacheManager != null) {
                    try {
                        // 3.1 更新 session 本地缓存
                        // 刷新时使用与 Redis 相同的过期时间（转换为 int，确保不超过 Integer.MAX_VALUE）
                        int localExpireSeconds = duration > Integer.MAX_VALUE 
                                ? Integer.MAX_VALUE 
                                : (int) duration;
                        caffeineCacheManager.put(SessionCacheConstants.CACHE_NAME_USER_SESSION, token, userContext, localExpireSeconds);
                        
                        // 3.2 更新 token 本地缓存（如果存在）
                        Optional<String> cachedUserId = caffeineCacheManager.get(SessionCacheConstants.CACHE_NAME_USER_TOKEN, token);
                        if (cachedUserId.isPresent()) {
                            caffeineCacheManager.put(SessionCacheConstants.CACHE_NAME_USER_TOKEN, token, cachedUserId.get(), localExpireSeconds);
                        } else {
                            // 如果本地缓存中没有 token，从 Redis 获取并写入本地缓存
                            String userId = cacheService.getValue(tokenKey);
                            if (userId != null) {
                                caffeineCacheManager.put(SessionCacheConstants.CACHE_NAME_USER_TOKEN, token, userId, localExpireSeconds);
                            }
                        }
                        
                        log.debug("Updated local cache after token renewal: token={}, localExpireSeconds={} (same as Redis)", 
                                token, localExpireSeconds);
                    } catch (Exception e) {
                        log.warn("Failed to update local cache after token renewal: token={}", token, e);
                    }
                }
                
                // 4. 发布缓存失效通知（通知其他节点清除本地缓存，因为它们会从 Redis 获取最新数据）
                if (cacheInvalidationService != null) {
                    cacheInvalidationService.publishInvalidation(SessionCacheConstants.CACHE_TYPE_USER_SESSION, token);
                    cacheInvalidationService.publishInvalidation(SessionCacheConstants.CACHE_TYPE_USER_TOKEN, token);
                }
                
                log.debug("Renewed token: {}, new duration: {} seconds", token, duration);
                return true;
            }
            log.warn("Failed to renew token: {}, session not found", token);
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
        
        // 1. 写入 Redis（主存储）
        cacheService.set(tokenKey, userId, expireSeconds);
        
        // 2. 写入 Caffeine 本地缓存（如果可用）
        if (caffeineCacheManager != null) {
            try {
                int localExpireSeconds = calculateLocalCacheExpire(expireSeconds);
                caffeineCacheManager.put(SessionCacheConstants.CACHE_NAME_USER_TOKEN, token, userId, localExpireSeconds);
                log.debug("Stored token to local cache: token={}, userId={}, expireSeconds={}", token, userId, localExpireSeconds);
            } catch (Exception e) {
                log.warn("Failed to store token to local cache: token={}", token, e);
            }
        }
        
        // 3. 发布缓存失效通知（通知其他节点清除本地缓存）
        if (cacheInvalidationService != null) {
            cacheInvalidationService.publishInvalidation(SessionCacheConstants.CACHE_TYPE_USER_TOKEN, token);
        }
        
        log.debug("Stored token: {} for user: {}, expiration: {} seconds", token, userId, expireSeconds);
    }
    
    @Override
    public String validateToken(String token) {
        String tokenKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "token", token);
        
        // 1. 优先从 Caffeine 本地缓存读取
        if (caffeineCacheManager != null) {
            try {
                Optional<String> cachedUserId = caffeineCacheManager.get(SessionCacheConstants.CACHE_NAME_USER_TOKEN, token);
                if (cachedUserId.isPresent()) {
                    String userId = cachedUserId.get();
                    log.debug("Retrieved token from local cache: token={}, userId={}", token, userId);
                    return userId;
                }
            } catch (Exception e) {
                log.warn("Failed to get token from local cache: token={}", token, e);
            }
        }
        
        // 2. 从 Redis 读取
        String userId = cacheService.getValue(tokenKey);
        
        if (userId != null && !userId.isEmpty()) {
            // 3. 写入 Caffeine 本地缓存（如果可用）
            if (caffeineCacheManager != null) {
                try {
                    // 记录数据读取时间戳（用于并发控制）
                    long dataTimestamp = System.currentTimeMillis();
                    
                    // 检查是否在失效之后（防止写入旧数据）
                    if (invalidationTracker != null && 
                        invalidationTracker.isInvalidated(SessionCacheConstants.CACHE_TYPE_USER_TOKEN, token, dataTimestamp)) {
                        log.debug("跳过写入本地缓存（数据已失效）: token={}", token);
                        return userId;
                    }
                    
                    // 获取 Redis 中的剩余过期时间
                    long remainingTime = cacheService.getTimeToLive(tokenKey);
                    int localExpireSeconds = remainingTime > 0 
                        ? calculateLocalCacheExpire(remainingTime) 
                        : LOCAL_CACHE_EXPIRE_SECONDS;
                    caffeineCacheManager.put(SessionCacheConstants.CACHE_NAME_USER_TOKEN, token, userId, localExpireSeconds);
                    
                    // 清除失效记录（数据已成功更新）
                    if (invalidationTracker != null) {
                        invalidationTracker.clearInvalidation(SessionCacheConstants.CACHE_TYPE_USER_TOKEN, token);
                    }
                    
                    log.debug("Stored token to local cache after Redis read: token={}, expireSeconds={}", 
                            token, localExpireSeconds);
                } catch (Exception e) {
                    log.warn("Failed to store token to local cache after Redis read: token={}", token, e);
                }
            }
        }
        
        return userId;
    }
    
    @Override
    public boolean refreshToken(String token, long expireSeconds) {
        String tokenKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "token", token);
        boolean result = cacheService.expire(tokenKey, expireSeconds);
        if (result) {
            // 更新本地缓存过期时间
            // 刷新时，本地缓存的过期时间应该和 Redis 保持一致，确保数据同步
            if (caffeineCacheManager != null) {
                try {
                    Optional<String> cachedUserId = caffeineCacheManager.get(SessionCacheConstants.CACHE_NAME_USER_TOKEN, token);
                    if (cachedUserId.isPresent()) {
                        // 刷新时使用与 Redis 相同的过期时间（转换为 int，确保不超过 Integer.MAX_VALUE）
                        int localExpireSeconds = expireSeconds > Integer.MAX_VALUE 
                                ? Integer.MAX_VALUE 
                                : (int) expireSeconds;
                        caffeineCacheManager.put(SessionCacheConstants.CACHE_NAME_USER_TOKEN, token, cachedUserId.get(), localExpireSeconds);
                        log.debug("Updated local cache after token refresh: token={}, localExpireSeconds={} (same as Redis)", 
                                token, localExpireSeconds);
                    }
                } catch (Exception e) {
                    log.warn("Failed to refresh token in local cache: token={}", token, e);
                }
            }
            log.debug("Refreshed token: {}, new expiration: {} seconds", token, expireSeconds);
        } else {
            log.warn("Failed to refresh token: {}, token may not exist", token);
        }
        return result;
    }
    
    @Override
    public void removeToken(String token) {
        String tokenKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "token", token);
        
        // 1. 删除 Redis 中的 token
        cacheService.delete(tokenKey);
        
        // 2. 删除 Caffeine 本地缓存（如果可用）
        if (caffeineCacheManager != null) {
            try {
                caffeineCacheManager.remove(SessionCacheConstants.CACHE_NAME_USER_TOKEN, token);
                log.debug("Removed token from local cache: token={}", token);
            } catch (Exception e) {
                log.warn("Failed to remove token from local cache: token={}", token, e);
            }
        }
        
        // 3. 发布缓存失效通知（通知其他节点清除本地缓存）
        if (cacheInvalidationService != null) {
            cacheInvalidationService.publishInvalidation(SessionCacheConstants.CACHE_TYPE_USER_TOKEN, token);
        }
        
        log.debug("Removed token: {}", token);
    }
    
    @Override
    public boolean tokenExists(String token) {
        String tokenKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "token", token);
        
        // 1. 优先从 Caffeine 本地缓存检查
        if (caffeineCacheManager != null) {
            try {
                Optional<String> cachedUserId = caffeineCacheManager.get(SessionCacheConstants.CACHE_NAME_USER_TOKEN, token);
                if (cachedUserId.isPresent()) {
                    log.debug("Token exists in local cache: token={}", token);
                    return true;
                }
            } catch (Exception e) {
                log.warn("Failed to check token in local cache: token={}", token, e);
            }
        }
        
        // 2. 从 Redis 检查
        boolean exists = cacheService.hasKey(tokenKey);
        
        if (exists) {
            // 3. 如果存在，从 Redis 读取并写入本地缓存
            String userId = cacheService.getValue(tokenKey);
            if (userId != null && caffeineCacheManager != null) {
                try {
                    long remainingTime = cacheService.getTimeToLive(tokenKey);
                    int localExpireSeconds = remainingTime > 0 
                        ? calculateLocalCacheExpire(remainingTime) 
                        : LOCAL_CACHE_EXPIRE_SECONDS;
                    caffeineCacheManager.put(SessionCacheConstants.CACHE_NAME_USER_TOKEN, token, userId, localExpireSeconds);
                    log.debug("Stored token to local cache after exists check: token={}", token);
                } catch (Exception e) {
                    log.warn("Failed to store token to local cache after exists check: token={}", token, e);
                }
            }
        }
        
        return exists;
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
     * 计算本地缓存过期时间
     * 本地缓存过期时间应该小于 Redis 过期时间，确保本地缓存先过期
     * 
     * @param redisExpiration Redis 过期时间（秒）
     * @return 本地缓存过期时间（秒）
     */
    private int calculateLocalCacheExpire(long redisExpiration) {
        // 本地缓存过期时间 = min(Redis过期时间的1/10, 5分钟)
        long localExpire = Math.min(redisExpiration / 10, LOCAL_CACHE_EXPIRE_SECONDS);
        // 确保至少 1 分钟
        return (int) Math.max(localExpire, 60);
    }

    /**
     * 刷新会话过期时间（滑动过期）
     * 当用户访问时，自动延长会话过期时间
     * 
     * <p>滑动过期策略：
     * <ul>
     *   <li>如果剩余时间少于 30 分钟（1800 秒），则延长到 1 小时（3600 秒）</li>
     *   <li>这样可以确保活跃用户的会话不会过期，同时避免长期不活跃的会话占用资源</li>
     * </ul>
     *
     * @param sessionKey 会话键
     * @param token      访问令牌
     */
    private void refreshSessionExpiry(String sessionKey, String token) {
        try {
            // 获取当前剩余过期时间
            long remainingTime = cacheService.getTimeToLive(sessionKey);
            
            // 如果剩余时间少于 30 分钟，则延长到 1 小时（滑动过期）
            if (remainingTime > 0 && remainingTime < 1800) { // 30 分钟 = 1800 秒
                long newExpiration = 3600; // 延长到 1 小时
                cacheService.resetExpiry(sessionKey, newExpiration);
                
                // 同时更新本地缓存
                // 刷新时，本地缓存的过期时间应该和 Redis 保持一致，确保数据同步
                if (caffeineCacheManager != null) {
                    try {
                        Optional<UserContext> cachedContext = caffeineCacheManager.get(SessionCacheConstants.CACHE_NAME_USER_SESSION, token);
                        if (cachedContext.isPresent()) {
                            // 使用与 Redis 相同的过期时间（转换为 int，确保不超过 Integer.MAX_VALUE）
                            int localExpireSeconds = newExpiration > Integer.MAX_VALUE 
                                    ? Integer.MAX_VALUE 
                                    : (int) newExpiration;
                            caffeineCacheManager.put(SessionCacheConstants.CACHE_NAME_USER_SESSION, token, cachedContext.get(), localExpireSeconds);
                            log.debug("Updated local cache after session expiry refresh: token={}, localExpireSeconds={} (same as Redis)", 
                                    token, localExpireSeconds);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to update local cache expiry: token={}", token, e);
                    }
                }
                
                log.debug("Refreshed session expiry for token: {}, remaining: {}s, extended to: {}s", 
                        token, remainingTime, newExpiration);
            }
        } catch (Exception e) {
            log.warn("Failed to refresh session expiry for token: {}", token, e);
        }
    }
} 