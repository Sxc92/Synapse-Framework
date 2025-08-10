package com.indigo.cache.core;

import com.indigo.cache.config.CacheProperties;
import com.indigo.cache.manager.CacheKeyGenerator;
import com.indigo.cache.infrastructure.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 缓存穿透防护服务
 * 支持空值缓存、布隆过滤器和限流防护
 *
 * @author 史偕成
 * @date 2025/01/08
 */
@Slf4j
@Component
public class CachePenetrationProtectionService {

    private final TwoLevelCacheService cacheService;
    private final CacheProperties cacheProperties;
    private final CacheKeyGenerator keyGenerator;
    private final RedisService redisService;

    // 空值缓存标记
    private static final String NULL_VALUE_MARKER = "NULL_VALUE_MARKER";
    
    // 请求计数器（用于限流）
    private final ConcurrentHashMap<String, LongAdder> requestCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> lastResetTime = new ConcurrentHashMap<>();

    @Autowired
    public CachePenetrationProtectionService(TwoLevelCacheService cacheService,
                                             CacheProperties cacheProperties,
                                             CacheKeyGenerator keyGenerator,
                                             RedisService redisService) {
        this.cacheService = cacheService;
        this.cacheProperties = cacheProperties;
        this.keyGenerator = keyGenerator;
        this.redisService = redisService;
    }

    /**
     * 获取缓存数据（带穿透防护）
     *
     * @param module    模块名
     * @param key       缓存键
     * @param strategy  缓存策略
     * @param <T>       数据类型
     * @return 缓存数据
     */
    public <T> java.util.Optional<T> getWithProtection(String module, String key, 
                                                       TwoLevelCacheService.CacheStrategy strategy) {
        if (!cacheProperties.getPenetrationProtection().isEnabled()) {
            return cacheService.get(module, key, strategy);
        }

        try {
            // 1. 限流检查
            if (!checkRateLimit(module, key)) {
                log.warn("缓存请求被限流: {}:{}", module, key);
                return java.util.Optional.empty();
            }

            // 2. 布隆过滤器检查（如果启用）
            if (cacheProperties.getPenetrationProtection().isEnableBloomFilter()) {
                if (!checkBloomFilter(module, key)) {
                    log.info("布隆过滤器显示键不存在: {}:{}", module, key);
                    return java.util.Optional.empty();
                }
            }

            // 3. 尝试获取缓存
            java.util.Optional<T> result = cacheService.get(module, key, strategy);
            
            if (result.isPresent()) {
                // 缓存命中
                return result;
            } else {
                // 缓存未命中，检查是否为空值缓存
                String nullKey = generateNullKey(module, key);
                java.util.Optional<Object> nullValue = cacheService.get("null_cache", nullKey, strategy);
                
                if (nullValue.isPresent()) {
                    log.info("空值缓存命中: {}:{}", module, key);
                    return java.util.Optional.empty();
                }
                
                // 不是空值缓存，返回空
                return java.util.Optional.empty();
            }
        } catch (Exception e) {
            log.error("缓存穿透防护异常: {}:{}", module, key, e);
            return java.util.Optional.empty();
        }
    }

    /**
     * 保存缓存数据（带穿透防护）
     *
     * @param module        模块名
     * @param key           缓存键
     * @param value         缓存值
     * @param expireSeconds 过期时间
     * @param strategy      缓存策略
     * @param <T>           数据类型
     */
    public <T> void saveWithProtection(String module, String key, T value, long expireSeconds, 
                                      TwoLevelCacheService.CacheStrategy strategy) {
        if (!cacheProperties.getPenetrationProtection().isEnabled()) {
            cacheService.save(module, key, value, expireSeconds, strategy);
            return;
        }

        try {
            // 保存实际数据
            cacheService.save(module, key, value, expireSeconds, strategy);
            
            // 如果启用了布隆过滤器，添加到过滤器
            if (cacheProperties.getPenetrationProtection().isEnableBloomFilter()) {
                addToBloomFilter(module, key);
            }
            
            log.info("缓存数据保存成功（带防护）: {}:{}", module, key);
        } catch (Exception e) {
            log.error("缓存数据保存失败（带防护）: {}:{}", module, key, e);
        }
    }

    /**
     * 保存空值缓存
     *
     * @param module 模块名
     * @param key    缓存键
     */
    public void saveNullValue(String module, String key) {
        if (!cacheProperties.getPenetrationProtection().isEnabled()) {
            return;
        }

        try {
            long nullExpireSeconds = cacheProperties.getPenetrationProtection().getNullValueExpire().getSeconds();
            String nullKey = generateNullKey(module, key);
            
            // 保存空值标记
            cacheService.save("null_cache", nullKey, NULL_VALUE_MARKER, nullExpireSeconds, 
                             TwoLevelCacheService.CacheStrategy.LOCAL_AND_REDIS);
            
            // 如果启用了布隆过滤器，添加到过滤器
            if (cacheProperties.getPenetrationProtection().isEnableBloomFilter()) {
                addToBloomFilter(module, key);
            }
            
            log.info("空值缓存保存成功: {}:{}", module, key);
        } catch (Exception e) {
            log.error("空值缓存保存失败: {}:{}", module, key, e);
        }
    }

    /**
     * 删除缓存数据（带穿透防护）
     *
     * @param module   模块名
     * @param key      缓存键
     * @param strategy 缓存策略
     */
    public void deleteWithProtection(String module, String key, TwoLevelCacheService.CacheStrategy strategy) {
        if (!cacheProperties.getPenetrationProtection().isEnabled()) {
            cacheService.delete(module, key, strategy);
            return;
        }

        try {
            // 删除实际数据
            cacheService.delete(module, key, strategy);
            
            // 删除空值缓存
            String nullKey = generateNullKey(module, key);
            cacheService.delete("null_cache", nullKey, strategy);
            
            // 如果启用了布隆过滤器，从过滤器中移除（注意：布隆过滤器不支持删除）
            // 这里可以考虑使用计数布隆过滤器或其他方案
            
            log.info("缓存数据删除成功（带防护）: {}:{}", module, key);
        } catch (Exception e) {
            log.error("缓存数据删除失败（带防护）: {}:{}", module, key, e);
        }
    }

    /**
     * 检查限流
     *
     * @param module 模块名
     * @param key    缓存键
     * @return 是否允许请求
     */
    private boolean checkRateLimit(String module, String key) {
        if (!cacheProperties.getPenetrationProtection().getRateLimit().isEnabled()) {
            return true;
        }

        try {
            String rateLimitKey = generateRateLimitKey(module, key);
            long currentTime = System.currentTimeMillis();
            long windowSize = cacheProperties.getPenetrationProtection().getRateLimit().getWindowSize().toMillis();
            
            // 获取或创建计数器
            LongAdder counter = requestCounters.computeIfAbsent(rateLimitKey, k -> new LongAdder());
            AtomicLong lastReset = lastResetTime.computeIfAbsent(rateLimitKey, k -> new AtomicLong(currentTime));
            
            // 检查是否需要重置计数器
            if (currentTime - lastReset.get() > windowSize) {
                counter.reset();
                lastReset.set(currentTime);
            }
            
            // 增加计数
            counter.increment();
            
            // 检查是否超过限制
            int limit = cacheProperties.getPenetrationProtection().getRateLimit().getRequestsPerSecond();
            return counter.sum() <= limit;
        } catch (Exception e) {
            log.error("限流检查异常: {}:{}", module, key, e);
            return true; // 异常时允许请求
        }
    }

    /**
     * 检查布隆过滤器
     *
     * @param module 模块名
     * @param key    缓存键
     * @return 是否可能存在
     */
    private boolean checkBloomFilter(String module, String key) {
        try {
            String bloomFilterKey = generateBloomFilterKey(module);
            String element = module + ":" + key;
            
            // 这里应该使用实际的布隆过滤器实现
            // 目前使用Redis的位图模拟布隆过滤器
            String hashKey = String.valueOf(Math.abs(element.hashCode()));
            return redisService.getBit(bloomFilterKey, Long.parseLong(hashKey));
        } catch (Exception e) {
            log.error("布隆过滤器检查异常: {}:{}", module, key, e);
            return true; // 异常时假设可能存在
        }
    }

    /**
     * 添加到布隆过滤器
     *
     * @param module 模块名
     * @param key    缓存键
     */
    private void addToBloomFilter(String module, String key) {
        try {
            String bloomFilterKey = generateBloomFilterKey(module);
            String element = module + ":" + key;
            
            // 这里应该使用实际的布隆过滤器实现
            // 目前使用Redis的位图模拟布隆过滤器
            String hashKey = String.valueOf(Math.abs(element.hashCode()));
            redisService.setBit(bloomFilterKey, Long.parseLong(hashKey), true);
        } catch (Exception e) {
            log.error("添加到布隆过滤器异常: {}:{}", module, key, e);
        }
    }

    /**
     * 生成空值缓存键
     *
     * @param module 模块名
     * @param key    缓存键
     * @return 空值缓存键
     */
    private String generateNullKey(String module, String key) {
        return "null:" + module + ":" + key;
    }

    /**
     * 生成限流键
     *
     * @param module 模块名
     * @param key    缓存键
     * @return 限流键
     */
    private String generateRateLimitKey(String module, String key) {
        return "rate_limit:" + module + ":" + key;
    }

    /**
     * 生成布隆过滤器键
     *
     * @param module 模块名
     * @return 布隆过滤器键
     */
    private String generateBloomFilterKey(String module) {
        return "bloom_filter:" + module;
    }

    /**
     * 获取穿透防护状态
     *
     * @return 穿透防护状态
     */
    public PenetrationProtectionStatus getProtectionStatus() {
        return PenetrationProtectionStatus.builder()
            .enabled(cacheProperties.getPenetrationProtection().isEnabled())
            .nullValueExpire(cacheProperties.getPenetrationProtection().getNullValueExpire())
            .enableBloomFilter(cacheProperties.getPenetrationProtection().isEnableBloomFilter())
            .bloomFilterSize(cacheProperties.getPenetrationProtection().getBloomFilterSize())
            .rateLimitEnabled(cacheProperties.getPenetrationProtection().getRateLimit().isEnabled())
            .requestsPerSecond(cacheProperties.getPenetrationProtection().getRateLimit().getRequestsPerSecond())
            .requestCountersSize(requestCounters.size())
            .build();
    }

    /**
     * 清除限流计数器
     */
    public void clearRateLimitCounters() {
        requestCounters.clear();
        lastResetTime.clear();
        log.info("限流计数器已清除");
    }

    /**
     * 检查键是否在布隆过滤器中
     *
     * @param key 缓存键
     * @return 是否在布隆过滤器中
     */
    public boolean isKeyInBloomFilter(String key) {
        return checkBloomFilter("default", key);
    }

    /**
     * 添加键到布隆过滤器
     *
     * @param key 缓存键
     */
    public void addKeyToBloomFilter(String key) {
        addToBloomFilter("default", key);
    }

    /**
     * 获取保护统计信息
     *
     * @return 保护统计信息
     */
    public ProtectionStatistics getProtectionStatistics() {
        return new ProtectionStatistics(
            requestCounters.size(),
            requestCounters.values().stream().mapToLong(LongAdder::sum).sum(),
            0, // nullValueCaches count
            0  // bloomFilterHits count
        );
    }

    /**
     * 保护统计信息
     */
    public static class ProtectionStatistics {
        private final int totalRequests;
        private final long totalHits;
        private final int nullValueCaches;
        private final int bloomFilterHits;

        public ProtectionStatistics(int totalRequests, long totalHits, int nullValueCaches, int bloomFilterHits) {
            this.totalRequests = totalRequests;
            this.totalHits = totalHits;
            this.nullValueCaches = nullValueCaches;
            this.bloomFilterHits = bloomFilterHits;
        }

        public int getTotalRequests() { return totalRequests; }
        public long getTotalHits() { return totalHits; }
        public int getNullValueCaches() { return nullValueCaches; }
        public int getBloomFilterHits() { return bloomFilterHits; }
    }

    /**
     * 穿透防护状态
     */
    public static class PenetrationProtectionStatus {
        private final boolean enabled;
        private final java.time.Duration nullValueExpire;
        private final boolean enableBloomFilter;
        private final long bloomFilterSize;
        private final boolean rateLimitEnabled;
        private final int requestsPerSecond;
        private final int requestCountersSize;

        public PenetrationProtectionStatus(boolean enabled, java.time.Duration nullValueExpire,
                                          boolean enableBloomFilter, long bloomFilterSize,
                                          boolean rateLimitEnabled, int requestsPerSecond,
                                          int requestCountersSize) {
            this.enabled = enabled;
            this.nullValueExpire = nullValueExpire;
            this.enableBloomFilter = enableBloomFilter;
            this.bloomFilterSize = bloomFilterSize;
            this.rateLimitEnabled = rateLimitEnabled;
            this.requestsPerSecond = requestsPerSecond;
            this.requestCountersSize = requestCountersSize;
        }

        public static Builder builder() {
            return new Builder();
        }

        public boolean isEnabled() { return enabled; }
        public java.time.Duration getNullValueExpire() { return nullValueExpire; }
        public boolean isEnableBloomFilter() { return enableBloomFilter; }
        public long getBloomFilterSize() { return bloomFilterSize; }
        public boolean isRateLimitEnabled() { return rateLimitEnabled; }
        public int getRequestsPerSecond() { return requestsPerSecond; }
        public int getRequestCountersSize() { return requestCountersSize; }

        public static class Builder {
            private boolean enabled;
            private java.time.Duration nullValueExpire;
            private boolean enableBloomFilter;
            private long bloomFilterSize;
            private boolean rateLimitEnabled;
            private int requestsPerSecond;
            private int requestCountersSize;

            public Builder enabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }

            public Builder nullValueExpire(java.time.Duration nullValueExpire) {
                this.nullValueExpire = nullValueExpire;
                return this;
            }

            public Builder enableBloomFilter(boolean enableBloomFilter) {
                this.enableBloomFilter = enableBloomFilter;
                return this;
            }

            public Builder bloomFilterSize(long bloomFilterSize) {
                this.bloomFilterSize = bloomFilterSize;
                return this;
            }

            public Builder rateLimitEnabled(boolean rateLimitEnabled) {
                this.rateLimitEnabled = rateLimitEnabled;
                return this;
            }

            public Builder requestsPerSecond(int requestsPerSecond) {
                this.requestsPerSecond = requestsPerSecond;
                return this;
            }

            public Builder requestCountersSize(int requestCountersSize) {
                this.requestCountersSize = requestCountersSize;
                return this;
            }

            public PenetrationProtectionStatus build() {
                return new PenetrationProtectionStatus(enabled, nullValueExpire, enableBloomFilter,
                    bloomFilterSize, rateLimitEnabled, requestsPerSecond, requestCountersSize);
            }
        }
    }
} 