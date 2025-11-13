package com.indigo.cache.core;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存失效追踪器
 * 用于追踪已失效的缓存键，防止在失效事件后写入旧数据
 * 
 * 解决并发问题：
 * 1. 当收到失效事件时，记录失效时间戳
 * 2. 写入本地缓存前，检查是否在失效之后
 * 3. 如果数据时间戳早于失效时间戳，则不写入
 * 
 * @author 史偕成
 * @date 2025/01/13
 */
@Slf4j
public class CacheInvalidationTracker {

    /**
     * 存储已失效的缓存键及其失效时间戳
     * Key: cacheType:cacheKey
     * Value: 失效时间戳（毫秒）
     */
    private final Map<String, Long> invalidationTimestamps = new ConcurrentHashMap<>();

    /**
     * 记录缓存失效
     * 
     * @param cacheType 缓存类型
     * @param cacheKey  缓存键
     */
    public void recordInvalidation(String cacheType, String cacheKey) {
        String key = buildKey(cacheType, cacheKey);
        long timestamp = System.currentTimeMillis();
        invalidationTimestamps.put(key, timestamp);
        log.debug("记录缓存失效: key={}, timestamp={}", key, timestamp);
        
        // 定期清理过期的失效记录（避免内存泄漏）
        // 只保留最近 1 小时的失效记录
        cleanupOldRecords(timestamp - 3600_000L);
    }

    /**
     * 检查缓存是否已失效
     * 
     * @param cacheType 缓存类型
     * @param cacheKey  缓存键
     * @param dataTimestamp 数据时间戳（从 Redis 读取数据的时间）
     * @return true 如果缓存已失效，且数据时间戳早于失效时间戳
     */
    public boolean isInvalidated(String cacheType, String cacheKey, long dataTimestamp) {
        String key = buildKey(cacheType, cacheKey);
        Long invalidationTimestamp = invalidationTimestamps.get(key);
        
        if (invalidationTimestamp == null) {
            // 没有失效记录，可以写入
            return false;
        }
        
        // 如果数据时间戳早于失效时间戳，说明数据是旧的，不应该写入
        boolean invalidated = dataTimestamp < invalidationTimestamp;
        if (invalidated) {
            log.debug("检测到缓存已失效，跳过写入: key={}, dataTimestamp={}, invalidationTimestamp={}", 
                    key, dataTimestamp, invalidationTimestamp);
        }
        
        return invalidated;
    }

    /**
     * 清除失效记录（当数据成功更新后）
     * 
     * @param cacheType 缓存类型
     * @param cacheKey  缓存键
     */
    public void clearInvalidation(String cacheType, String cacheKey) {
        String key = buildKey(cacheType, cacheKey);
        invalidationTimestamps.remove(key);
        log.debug("清除失效记录: key={}", key);
    }

    /**
     * 构建缓存键
     */
    private String buildKey(String cacheType, String cacheKey) {
        return cacheType + ":" + cacheKey;
    }

    /**
     * 清理过期的失效记录
     * 
     * @param cutoffTimestamp 截止时间戳，早于此时间的记录将被清理
     */
    private void cleanupOldRecords(long cutoffTimestamp) {
        // 每 100 次操作清理一次，避免频繁清理
        if (invalidationTimestamps.size() % 100 == 0) {
            invalidationTimestamps.entrySet().removeIf(entry -> entry.getValue() < cutoffTimestamp);
            log.debug("清理过期失效记录，剩余记录数: {}", invalidationTimestamps.size());
        }
    }

    /**
     * 获取失效记录数量（用于监控）
     */
    public int getInvalidationCount() {
        return invalidationTimestamps.size();
    }
}

