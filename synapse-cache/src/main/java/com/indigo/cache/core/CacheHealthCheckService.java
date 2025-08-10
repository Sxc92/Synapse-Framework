package com.indigo.cache.core;

import com.indigo.cache.config.CacheProperties;
import com.indigo.cache.infrastructure.CaffeineCacheManager;
import com.indigo.cache.infrastructure.RedisService;
import com.indigo.core.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存健康检查服务
 * 监控本地缓存和Redis缓存的健康状态
 *
 * @author 史偕成
 * @date 2025/01/08
 */
@Slf4j
@Component
public class CacheHealthCheckService {

    private final TwoLevelCacheService cacheService;
    private final CaffeineCacheManager localCache;
    private final RedisService redisService;
    private final CacheProperties cacheProperties;
    private final ThreadUtils threadUtils;

    private ScheduledFuture<?> healthCheckTask;
    private final AtomicBoolean isHealthy = new AtomicBoolean(true);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastCheckTime = new AtomicLong(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);

    @Autowired
    public CacheHealthCheckService(TwoLevelCacheService cacheService,
                                   CaffeineCacheManager localCache,
                                   RedisService redisService,
                                   CacheProperties cacheProperties,
                                   ThreadUtils threadUtils) {
        this.cacheService = cacheService;
        this.localCache = localCache;
        this.redisService = redisService;
        this.cacheProperties = cacheProperties;
        this.threadUtils = threadUtils;
        
        // 启动健康检查任务
        startHealthCheck();
    }

    /**
     * 启动健康检查任务
     */
    private void startHealthCheck() {
        if (!cacheProperties.getHealthCheck().isEnabled()) {
            log.info("缓存健康检查功能已禁用");
            return;
        }

        long intervalSeconds = cacheProperties.getHealthCheck().getInterval().getSeconds();
        log.info("启动缓存健康检查任务，间隔: {}秒", intervalSeconds);

        healthCheckTask = threadUtils.scheduleWithFixedDelay(
            this::performHealthCheck,
            intervalSeconds,
            intervalSeconds,
            TimeUnit.SECONDS
        );
    }

    /**
     * 执行健康检查
     */
    private void performHealthCheck() {
        try {
            long startTime = System.currentTimeMillis();
            
            boolean localCacheHealthy = checkLocalCache();
            boolean redisCacheHealthy = checkRedisCache();
            
            boolean overallHealthy = localCacheHealthy && redisCacheHealthy;
            
            if (overallHealthy) {
                if (!isHealthy.get()) {
                    log.info("缓存服务已恢复健康状态");
                }
                isHealthy.set(true);
                failureCount.set(0);
            } else {
                int failures = failureCount.incrementAndGet();
                isHealthy.set(false);
                lastFailureTime.set(System.currentTimeMillis());
                
                // 只在失败次数变化时记录警告
                if (failures == 1 || failures % 5 == 0) {
                    log.warn("缓存健康检查失败，失败次数: {}", failures);
                }
                
                // 检查是否超过失败阈值
                if (failures >= cacheProperties.getHealthCheck().getFailureThreshold()) {
                    log.error("缓存健康检查失败次数超过阈值: {}", failures);
                }
            }
            
            lastCheckTime.set(System.currentTimeMillis());
            long duration = System.currentTimeMillis() - startTime;
            
            // 只在耗时过长时记录警告
            if (duration > cacheProperties.getHealthCheck().getTimeout().toMillis()) {
                log.warn("缓存健康检查耗时过长: {}ms", duration);
            }
            
            // 只在调试模式下记录详细信息
            log.info("缓存健康检查完成，耗时: {}ms，状态: {}", duration, overallHealthy ? "健康" : "异常");
        } catch (Exception e) {
            log.error("缓存健康检查异常", e);
            isHealthy.set(false);
            failureCount.incrementAndGet();
            lastFailureTime.set(System.currentTimeMillis());
        }
    }

    /**
     * 检查本地缓存健康状态
     *
     * @return 是否健康
     */
    private boolean checkLocalCache() {
        try {
            // 检查本地缓存是否可用
            String testKey = "health_check_local_" + System.currentTimeMillis();
            String testValue = "test_value";
            
            // 尝试保存和获取数据
            localCache.put("health_check", testKey, testValue);
            java.util.Optional<Object> result = localCache.get("health_check", testKey);
            
            if (result.isPresent() && testValue.equals(result.get())) {
                // 清理测试数据
                localCache.remove("health_check", testKey);
                return true;
            } else {
                log.info("本地缓存健康检查失败：数据不一致");
                return false;
            }
        } catch (Exception e) {
            log.error("本地缓存健康检查异常", e);
            return false;
        }
    }

    /**
     * 检查Redis缓存健康状态
     *
     * @return 是否健康
     */
    private boolean checkRedisCache() {
        try {
            // 检查Redis连接和基本操作
            String testKey = "health_check_redis_" + System.currentTimeMillis();
            String testValue = "test_value";
            
            // 尝试保存和获取数据
            redisService.set(testKey, testValue, 60);
            Object result = redisService.get(testKey);
            
            if (testValue.equals(result)) {
                // 清理测试数据
                redisService.delete(testKey);
                return true;
            } else {
                log.info("Redis缓存健康检查失败：数据不一致");
                return false;
            }
        } catch (Exception e) {
            log.error("Redis缓存健康检查异常", e);
            return false;
        }
    }

    /**
     * 手动执行健康检查
     *
     * @return 健康检查结果
     */
    public HealthCheckResult manualHealthCheck() {
        log.info("开始手动健康检查");
        
        long startTime = System.currentTimeMillis();
        boolean localCacheHealthy = checkLocalCache();
        boolean redisCacheHealthy = checkRedisCache();
        boolean overallHealthy = localCacheHealthy && redisCacheHealthy;
        long duration = System.currentTimeMillis() - startTime;
        
        HealthCheckResult result = HealthCheckResult.builder()
            .overallHealthy(overallHealthy)
            .localCacheHealthy(localCacheHealthy)
            .redisCacheHealthy(redisCacheHealthy)
            .checkTime(System.currentTimeMillis())
            .duration(duration)
            .failureCount(failureCount.get())
            .lastFailureTime(lastFailureTime.get())
            .build();
        
        log.info("手动健康检查完成: {}", result);
        return result;
    }

    /**
     * 获取健康状态
     *
     * @return 是否健康
     */
    public boolean isHealthy() {
        return isHealthy.get();
    }

    /**
     * 获取详细健康状态
     *
     * @return 详细健康状态
     */
    public DetailedHealthStatus getDetailedHealthStatus() {
        return DetailedHealthStatus.builder()
            .isHealthy(isHealthy.get())
            .failureCount(failureCount.get())
            .lastCheckTime(lastCheckTime.get())
            .lastFailureTime(lastFailureTime.get())
            .healthCheckEnabled(cacheProperties.getHealthCheck().isEnabled())
            .healthCheckInterval(cacheProperties.getHealthCheck().getInterval())
            .failureThreshold(cacheProperties.getHealthCheck().getFailureThreshold())
            .recoveryThreshold(cacheProperties.getHealthCheck().getRecoveryThreshold())
            .scheduledTaskRunning(healthCheckTask != null && !healthCheckTask.isCancelled())
            .build();
    }

    /**
     * 停止健康检查
     */
    public void stopHealthCheck() {
        if (healthCheckTask != null && !healthCheckTask.isCancelled()) {
            healthCheckTask.cancel(true);
            log.info("缓存健康检查任务已停止");
        }
    }

    /**
     * 重置健康状态
     */
    public void resetHealthStatus() {
        isHealthy.set(true);
        failureCount.set(0);
        lastFailureTime.set(0);
        log.info("缓存健康状态已重置");
    }

    /**
     * 健康检查结果
     */
    public static class HealthCheckResult {
        private final boolean overallHealthy;
        private final boolean localCacheHealthy;
        private final boolean redisCacheHealthy;
        private final long checkTime;
        private final long duration;
        private final int failureCount;
        private final long lastFailureTime;

        public HealthCheckResult(boolean overallHealthy, boolean localCacheHealthy, boolean redisCacheHealthy,
                                long checkTime, long duration, int failureCount, long lastFailureTime) {
            this.overallHealthy = overallHealthy;
            this.localCacheHealthy = localCacheHealthy;
            this.redisCacheHealthy = redisCacheHealthy;
            this.checkTime = checkTime;
            this.duration = duration;
            this.failureCount = failureCount;
            this.lastFailureTime = lastFailureTime;
        }

        public static Builder builder() {
            return new Builder();
        }

        public boolean isOverallHealthy() { return overallHealthy; }
        public boolean isLocalCacheHealthy() { return localCacheHealthy; }
        public boolean isRedisCacheHealthy() { return redisCacheHealthy; }
        public long getCheckTime() { return checkTime; }
        public long getDuration() { return duration; }
        public int getFailureCount() { return failureCount; }
        public long getLastFailureTime() { return lastFailureTime; }

        @Override
        public String toString() {
            return String.format("HealthCheckResult{overallHealthy=%s, localCacheHealthy=%s, redisCacheHealthy=%s, " +
                "checkTime=%d, duration=%dms, failureCount=%d}", 
                overallHealthy, localCacheHealthy, redisCacheHealthy, checkTime, duration, failureCount);
        }

        public static class Builder {
            private boolean overallHealthy;
            private boolean localCacheHealthy;
            private boolean redisCacheHealthy;
            private long checkTime;
            private long duration;
            private int failureCount;
            private long lastFailureTime;

            public Builder overallHealthy(boolean overallHealthy) {
                this.overallHealthy = overallHealthy;
                return this;
            }

            public Builder localCacheHealthy(boolean localCacheHealthy) {
                this.localCacheHealthy = localCacheHealthy;
                return this;
            }

            public Builder redisCacheHealthy(boolean redisCacheHealthy) {
                this.redisCacheHealthy = redisCacheHealthy;
                return this;
            }

            public Builder checkTime(long checkTime) {
                this.checkTime = checkTime;
                return this;
            }

            public Builder duration(long duration) {
                this.duration = duration;
                return this;
            }

            public Builder failureCount(int failureCount) {
                this.failureCount = failureCount;
                return this;
            }

            public Builder lastFailureTime(long lastFailureTime) {
                this.lastFailureTime = lastFailureTime;
                return this;
            }

            public HealthCheckResult build() {
                return new HealthCheckResult(overallHealthy, localCacheHealthy, redisCacheHealthy,
                    checkTime, duration, failureCount, lastFailureTime);
            }
        }
    }

    /**
     * 详细健康状态
     */
    public static class DetailedHealthStatus {
        private final boolean isHealthy;
        private final int failureCount;
        private final long lastCheckTime;
        private final long lastFailureTime;
        private final boolean healthCheckEnabled;
        private final Duration healthCheckInterval;
        private final int failureThreshold;
        private final int recoveryThreshold;
        private final boolean scheduledTaskRunning;

        public DetailedHealthStatus(boolean isHealthy, int failureCount, long lastCheckTime, long lastFailureTime,
                                   boolean healthCheckEnabled, Duration healthCheckInterval, int failureThreshold,
                                   int recoveryThreshold, boolean scheduledTaskRunning) {
            this.isHealthy = isHealthy;
            this.failureCount = failureCount;
            this.lastCheckTime = lastCheckTime;
            this.lastFailureTime = lastFailureTime;
            this.healthCheckEnabled = healthCheckEnabled;
            this.healthCheckInterval = healthCheckInterval;
            this.failureThreshold = failureThreshold;
            this.recoveryThreshold = recoveryThreshold;
            this.scheduledTaskRunning = scheduledTaskRunning;
        }

        public static Builder builder() {
            return new Builder();
        }

        public boolean isHealthy() { return isHealthy; }
        public int getFailureCount() { return failureCount; }
        public long getLastCheckTime() { return lastCheckTime; }
        public long getLastFailureTime() { return lastFailureTime; }
        public boolean isHealthCheckEnabled() { return healthCheckEnabled; }
        public Duration getHealthCheckInterval() { return healthCheckInterval; }
        public int getFailureThreshold() { return failureThreshold; }
        public int getRecoveryThreshold() { return recoveryThreshold; }
        public boolean isScheduledTaskRunning() { return scheduledTaskRunning; }

        public static class Builder {
            private boolean isHealthy;
            private int failureCount;
            private long lastCheckTime;
            private long lastFailureTime;
            private boolean healthCheckEnabled;
            private Duration healthCheckInterval;
            private int failureThreshold;
            private int recoveryThreshold;
            private boolean scheduledTaskRunning;

            public Builder isHealthy(boolean isHealthy) {
                this.isHealthy = isHealthy;
                return this;
            }

            public Builder failureCount(int failureCount) {
                this.failureCount = failureCount;
                return this;
            }

            public Builder lastCheckTime(long lastCheckTime) {
                this.lastCheckTime = lastCheckTime;
                return this;
            }

            public Builder lastFailureTime(long lastFailureTime) {
                this.lastFailureTime = lastFailureTime;
                return this;
            }

            public Builder healthCheckEnabled(boolean healthCheckEnabled) {
                this.healthCheckEnabled = healthCheckEnabled;
                return this;
            }

            public Builder healthCheckInterval(Duration healthCheckInterval) {
                this.healthCheckInterval = healthCheckInterval;
                return this;
            }

            public Builder failureThreshold(int failureThreshold) {
                this.failureThreshold = failureThreshold;
                return this;
            }

            public Builder recoveryThreshold(int recoveryThreshold) {
                this.recoveryThreshold = recoveryThreshold;
                return this;
            }

            public Builder scheduledTaskRunning(boolean scheduledTaskRunning) {
                this.scheduledTaskRunning = scheduledTaskRunning;
                return this;
            }

            public DetailedHealthStatus build() {
                return new DetailedHealthStatus(isHealthy, failureCount, lastCheckTime, lastFailureTime,
                    healthCheckEnabled, healthCheckInterval, failureThreshold, recoveryThreshold, scheduledTaskRunning);
            }
        }
    }
} 