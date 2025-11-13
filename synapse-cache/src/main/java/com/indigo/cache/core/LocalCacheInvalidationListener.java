package com.indigo.cache.core;

import com.indigo.cache.infrastructure.CaffeineCacheManager;
import com.indigo.cache.core.constants.SessionCacheConstants;
import lombok.extern.slf4j.Slf4j;

/**
 * 本地缓存失效监听器
 * 当收到缓存失效事件时，清除本地 Caffeine 缓存
 * 
 * @author 史偕成
 * @date 2025/01/13
 */
@Slf4j
public class LocalCacheInvalidationListener implements CacheInvalidationService.CacheInvalidationListener {


    private final CaffeineCacheManager caffeineCacheManager;
    private final CacheInvalidationTracker invalidationTracker;

    public LocalCacheInvalidationListener(CaffeineCacheManager caffeineCacheManager,
                                         CacheInvalidationTracker invalidationTracker) {
        this.caffeineCacheManager = caffeineCacheManager;
        this.invalidationTracker = invalidationTracker;
    }

    @Override
    public void onCacheInvalidation(String cacheType, String cacheKey) {
        if (caffeineCacheManager == null) {
            return;
        }

        try {
            // 1. 记录失效时间戳（用于防止后续写入旧数据）
            invalidationTracker.recordInvalidation(cacheType, cacheKey);
            
            // 2. 清除本地缓存
            String cacheName = getCacheName(cacheType);
            if (cacheName != null) {
                caffeineCacheManager.remove(cacheName, cacheKey);
                log.debug("清除本地缓存: cacheType={}, cacheKey={}", cacheType, cacheKey);
            } else {
                log.warn("未知的缓存类型: cacheType={}, cacheKey={}", cacheType, cacheKey);
            }
        } catch (Exception e) {
            log.error("清除本地缓存失败: cacheType={}, cacheKey={}", cacheType, cacheKey, e);
        }
    }

    /**
     * 根据缓存类型获取缓存名称
     */
    private String getCacheName(String cacheType) {
        return switch (cacheType) {
            case SessionCacheConstants.CACHE_TYPE_USER_SESSION -> SessionCacheConstants.CACHE_NAME_USER_SESSION;
            case SessionCacheConstants.CACHE_TYPE_USER_TOKEN -> SessionCacheConstants.CACHE_NAME_USER_TOKEN;
            case SessionCacheConstants.CACHE_TYPE_USER_PERMISSIONS -> SessionCacheConstants.CACHE_NAME_USER_PERMISSIONS;
            case SessionCacheConstants.CACHE_TYPE_USER_ROLES -> SessionCacheConstants.CACHE_NAME_USER_ROLES;
            case SessionCacheConstants.CACHE_TYPE_USER_MENUS -> SessionCacheConstants.CACHE_NAME_USER_MENUS;
            case SessionCacheConstants.CACHE_TYPE_USER_RESOURCES -> SessionCacheConstants.CACHE_NAME_USER_RESOURCES;
            case SessionCacheConstants.CACHE_TYPE_USER_SYSTEMS -> SessionCacheConstants.CACHE_NAME_USER_SYSTEMS;
            default -> null;
        };
    }
}

