package com.indigo.cache.extension.lock.resource;

import com.indigo.cache.config.LockProperties;
import com.indigo.cache.core.CacheService;
import com.indigo.cache.infrastructure.CaffeineCacheManager;
import com.indigo.cache.session.SessionManager;
import com.indigo.cache.session.StatisticsManager;
import com.indigo.cache.extension.lock.LockPerformanceMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * 自动释放检查器
 * 负责定期检查并自动释放资源
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoReleaseChecker {
    
    private final ResourcePool resourcePool;
    private final LockProperties lockProperties;
    private final CacheService cacheService;
    private final CaffeineCacheManager caffeineCacheManager;
    private final SessionManager sessionManager;
    private final StatisticsManager statisticsManager;
    private final LockPerformanceMonitor lockPerformanceMonitor;
    
    @Value("${synapse.cache.temp.dir:${java.io.tmpdir}/synapse-cache}")
    private String tempDirectory;
    
    @Value("${synapse.cache.monitoring.data-retention-hours:24}")
    private int monitoringDataRetentionHours;
    
    @Value("${synapse.cache.session.cleanup-expired:true}")
    private boolean cleanupExpiredSessions;
    
    @Value("${synapse.cache.lock.cleanup-expired:true}")
    private boolean cleanupExpiredLocks;
    
    /**
     * 检查并自动释放资源
     * 默认每分钟检查一次
     */
    @Scheduled(fixedDelayString = "${synapse.cache.lock.auto-release.check-interval:60000}")
    public void checkAndAutoRelease() {
        log.debug("开始检查资源自动释放...");
        
        for (ResourceType resourceType : ResourceType.values()) {
            if (resourceType.getReleaseLevel() == ReleaseLevel.NEVER) {
                continue; // 跳过永不释放的资源
            }
            
            if (shouldAutoRelease(resourceType)) {
                autoReleaseResource(resourceType);
            }
        }
        
        log.debug("资源自动释放检查完成");
    }
    
    /**
     * 判断是否应该自动释放资源
     */
    private boolean shouldAutoRelease(ResourceType resourceType) {
        ResourceState state = resourcePool.getResourceState(resourceType);
        if (state == null || !state.isAvailable()) {
            return false; // 资源不可用或已释放
        }
        
        // 检查是否超过释放阈值
        long timeSinceLastAccess = state.getTimeSinceLastAccess();
        long releaseThreshold = getReleaseThreshold(resourceType);
        
        boolean shouldRelease = timeSinceLastAccess > releaseThreshold;
        
        if (shouldRelease) {
            log.debug("资源 {} 超过释放阈值: {}ms > {}ms", 
                     resourceType, timeSinceLastAccess, releaseThreshold);
        }
        
        return shouldRelease;
    }
    
    /**
     * 获取释放阈值
     */
    private long getReleaseThreshold(ResourceType resourceType) {
        switch (resourceType) {
            case CORE_SERVICE:
                return lockProperties.getAutoRelease().getCoreServiceThreshold();
            case BUSINESS_CACHE:
                return lockProperties.getAutoRelease().getBusinessCacheThreshold();
            case TEMPORARY:
                return lockProperties.getAutoRelease().getTemporaryThreshold();
            default:
                return 300000; // 默认5分钟
        }
    }
    
    /**
     * 自动释放资源
     */
    private void autoReleaseResource(ResourceType resourceType) {
        log.info("自动释放资源: {}", resourceType);
        
        try {
            // 标记资源为已释放
            resourcePool.markResourceReleased(resourceType);
            
            // 执行具体的资源释放逻辑
            executeResourceRelease(resourceType);
            
            log.debug("资源 {} 自动释放完成", resourceType);
            
        } catch (Exception e) {
            log.error("资源 {} 自动释放失败", resourceType, e);
        }
    }
    
    /**
     * 执行具体的资源释放逻辑
     */
    private void executeResourceRelease(ResourceType resourceType) {
        switch (resourceType) {
            case CORE_SERVICE:
                // 释放核心服务资源（保留预分配池）
                releaseCoreServiceResources();
                break;
            case BUSINESS_CACHE:
                // 释放业务缓存
                releaseBusinessCacheResources();
                break;
            case TEMPORARY:
                // 释放临时资源
                releaseTemporaryResources();
                break;
            default:
                log.warn("未知的资源类型: {}", resourceType);
        }
    }
    
    /**
     * 释放核心服务资源
     * 注意：这里只释放业务相关的资源，保留预分配池
     */
    private void releaseCoreServiceResources() {
        log.debug("开始释放核心服务资源");
        
        try {
            // 1. 清理业务缓存
            clearBusinessCaches();
            
            // 2. 释放业务对象
            releaseBusinessObjects();
            
            // 3. 清理会话信息
            clearSessionInfo();
            
            // 4. 重置统计信息
            resetStatistics();
            
            // 5. 清理过期的监控数据
            clearExpiredMonitoringData();
            
            log.debug("核心服务资源释放完成");
            
        } catch (Exception e) {
            log.error("释放核心服务资源时发生异常", e);
        }
    }
    
    /**
     * 释放业务缓存资源
     */
    private void releaseBusinessCacheResources() {
        log.debug("开始释放业务缓存资源");
        
        try {
            // 1. 清理业务缓存
            clearBusinessCaches();
            
            // 2. 释放缓存对象
            releaseCacheObjects();
            
            // 3. 清理过期数据
            clearExpiredData();
            
            // 4. 压缩缓存空间
            compactCacheSpace();
            
            log.debug("业务缓存资源释放完成");
            
        } catch (Exception e) {
            log.error("释放业务缓存资源时发生异常", e);
        }
    }
    
    /**
     * 释放临时资源
     */
    private void releaseTemporaryResources() {
        log.debug("开始释放临时资源");
        
        try {
            // 1. 清理临时文件
            clearTemporaryFiles();
            
            // 2. 释放临时内存
            releaseTemporaryMemory();
            
            // 3. 清理过期缓存
            clearExpiredCache();
            
            // 4. 清理临时会话
            clearTemporarySessions();
            
            // 5. 清理临时锁
            clearTemporaryLocks();
            
            log.debug("临时资源释放完成");
            
        } catch (Exception e) {
            log.error("释放临时资源时发生异常", e);
        }
    }
    
    // ==================== 具体资源释放实现 ====================
    
    /**
     * 清理业务缓存
     */
    private void clearBusinessCaches() {
        log.debug("清理业务缓存");
        try {
            // 清理过期的业务缓存
            long clearedCount = cacheService.clearExpiredEntries();
            log.info("清理过期业务缓存完成，清理数量: {}", clearedCount);
            
            // 清理本地缓存中的过期条目
            caffeineCacheManager.getAllCaches().forEach((cacheName, cache) -> {
                cache.cleanUp();
                log.debug("清理本地缓存: {}", cacheName);
            });
            
        } catch (Exception e) {
            log.error("清理业务缓存失败", e);
        }
    }
    
    /**
     * 释放业务对象
     */
    private void releaseBusinessObjects() {
        log.debug("释放业务对象");
        try {
            // 清理业务对象池中的过期对象
            // 这里可以根据具体的业务对象池实现来清理
            // 例如：businessObjectPool.releaseExpiredObjects();
            
            // 清理缓存中的大对象
            long largeObjectCount = cacheService.clearLargeObjects(1024 * 1024); // 1MB以上
            log.info("释放大对象完成，释放数量: {}", largeObjectCount);
            
        } catch (Exception e) {
            log.error("释放业务对象失败", e);
        }
    }
    
    /**
     * 清理会话信息
     */
    private void clearSessionInfo() {
        log.debug("清理会话信息");
        try {
            // 注意：Redis的TTL机制会自动清理过期的会话和权限信息
            // 无需手动清理，这里只记录日志
            if (cleanupExpiredSessions) {
                log.info("Redis自动清理机制已处理过期会话和权限信息");
            }
            
        } catch (Exception e) {
            log.error("清理会话信息失败", e);
        }
    }
    
    /**
     * 重置统计信息
     */
    private void resetStatistics() {
        log.debug("重置统计信息");
        try {
            // 重置锁性能统计
            lockPerformanceMonitor.resetStats(null);
            log.info("重置锁性能统计完成");
            
            // 重置会话统计
            if (statisticsManager != null) {
                statisticsManager.resetStatistics();
                log.info("重置会话统计完成");
            }
            
        } catch (Exception e) {
            log.error("重置统计信息失败", e);
        }
    }
    
    /**
     * 清理过期的监控数据
     */
    private void clearExpiredMonitoringData() {
        log.debug("清理过期的监控数据");
        try {
            // 清理过期的性能指标、日志等
            LocalDateTime cutoffTime = LocalDateTime.now().minus(monitoringDataRetentionHours, ChronoUnit.HOURS);
            
            // 清理过期的锁性能数据
            long lockDataCount = lockPerformanceMonitor.clearExpiredData(cutoffTime);
            log.info("清理过期锁性能数据完成，清理数量: {}", lockDataCount);
            
            // 清理过期的会话统计数据
            if (statisticsManager != null) {
                long sessionDataCount = statisticsManager.clearExpiredData(cutoffTime);
                log.info("清理过期会话统计数据完成，清理数量: {}", sessionDataCount);
            }
            
        } catch (Exception e) {
            log.error("清理过期监控数据失败", e);
        }
    }
    
    /**
     * 释放缓存对象
     */
    private void releaseCacheObjects() {
        log.debug("释放缓存对象");
        try {
            // 释放缓存中的大对象，保留小对象
            long largeObjectCount = cacheService.clearLargeObjects(512 * 1024); // 512KB以上
            log.info("释放大缓存对象完成，释放数量: {}", largeObjectCount);
            
            // 清理本地缓存中的大对象
            caffeineCacheManager.getAllCaches().forEach((cacheName, cache) -> {
                cache.asMap().entrySet().removeIf(entry -> {
                    if (entry.getValue() instanceof String) {
                        return ((String) entry.getValue()).length() > 10000; // 10KB以上
                    }
                    return false;
                });
                log.debug("清理本地缓存大对象: {}", cacheName);
            });
            
        } catch (Exception e) {
            log.error("释放缓存对象失败", e);
        }
    }
    
    /**
     * 清理过期数据
     */
    private void clearExpiredData() {
        log.debug("清理过期数据");
        try {
            // 清理所有过期的缓存数据
            long expiredCount = cacheService.clearExpiredEntries();
            log.info("清理过期缓存数据完成，清理数量: {}", expiredCount);
            
            // 清理本地缓存过期条目
            caffeineCacheManager.getAllCaches().forEach((cacheName, cache) -> {
                long beforeSize = cache.estimatedSize();
                cache.cleanUp();
                long afterSize = cache.estimatedSize();
                log.debug("清理本地缓存过期条目: {} ({} -> {})", cacheName, beforeSize, afterSize);
            });
            
        } catch (Exception e) {
            log.error("清理过期数据失败", e);
        }
    }
    
    /**
     * 压缩缓存空间
     */
    private void compactCacheSpace() {
        log.debug("压缩缓存空间");
        try {
            // 压缩缓存空间，回收碎片
            long compactedSize = cacheService.compact();
            log.info("压缩缓存空间完成，压缩后大小: {} bytes", compactedSize);
            
            // 压缩本地缓存
            caffeineCacheManager.getAllCaches().forEach((cacheName, cache) -> {
                cache.cleanUp();
                log.debug("压缩本地缓存: {}", cacheName);
            });
            
        } catch (Exception e) {
            log.error("压缩缓存空间失败", e);
        }
    }
    
    /**
     * 清理临时文件
     */
    private void clearTemporaryFiles() {
        log.debug("清理临时文件");
        try {
            Path tempPath = Paths.get(tempDirectory);
            if (Files.exists(tempPath)) {
                // 清理超过1小时的临时文件
                long cutoffTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
                
                long deletedCount = Files.walk(tempPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .mapToLong(path -> {
                        try {
                            Files.delete(path);
                            return 1;
                        } catch (Exception e) {
                            log.warn("删除临时文件失败: {}", path, e);
                            return 0;
                        }
                    })
                    .sum();
                
                log.info("清理临时文件完成，删除数量: {}", deletedCount);
            }
            
        } catch (Exception e) {
            log.error("清理临时文件失败", e);
        }
    }
    
    /**
     * 释放临时内存
     */
    private void releaseTemporaryMemory() {
        log.debug("释放临时内存");
        try {
            // 释放临时分配的内存
            System.gc(); // 建议垃圾回收
            
            // 清理本地缓存中的临时数据
            caffeineCacheManager.getAllCaches().forEach((cacheName, cache) -> {
                long beforeSize = cache.estimatedSize();
                cache.cleanUp();
                long afterSize = cache.estimatedSize();
                log.debug("释放本地缓存临时内存: {} ({} -> {})", cacheName, beforeSize, afterSize);
            });
            
            log.info("临时内存释放完成");
            
        } catch (Exception e) {
            log.error("释放临时内存失败", e);
        }
    }
    
    /**
     * 清理过期缓存
     */
    private void clearExpiredCache() {
        log.debug("清理过期缓存");
        try {
            // 清理过期的临时缓存
            long expiredCount = cacheService.clearExpiredEntries();
            log.info("清理过期临时缓存完成，清理数量: {}", expiredCount);
            
            // 清理本地临时缓存
            caffeineCacheManager.getAllCaches().forEach((cacheName, cache) -> {
                if (cacheName.contains("temp") || cacheName.contains("tmp")) {
                    long beforeSize = cache.estimatedSize();
                    cache.cleanUp();
                    long afterSize = cache.estimatedSize();
                    log.debug("清理本地临时缓存: {} ({} -> {})", cacheName, beforeSize, afterSize);
                }
            });
            
        } catch (Exception e) {
            log.error("清理过期缓存失败", e);
        }
    }
    
    /**
     * 清理临时会话
     */
    private void clearTemporarySessions() {
        log.debug("清理临时会话");
        try {
            if (cleanupExpiredSessions) {
                // 注意：Redis的TTL机制会自动清理过期的临时会话
                // 无需手动清理，这里只记录日志
                log.info("Redis自动清理机制已处理过期临时会话");
            }
            
        } catch (Exception e) {
            log.error("清理临时会话失败", e);
        }
    }
    
    /**
     * 清理临时锁
     */
    private void clearTemporaryLocks() {
        log.debug("清理临时锁");
        try {
            if (cleanupExpiredLocks) {
                // 清理过期的临时锁
                long clearedCount = lockPerformanceMonitor.clearExpiredLocks();
                log.info("清理过期临时锁完成，清理数量: {}", clearedCount);
            }
            
        } catch (Exception e) {
            log.error("清理临时锁失败", e);
        }
    }
    
    /**
     * 手动触发资源释放检查
     */
    public void manualCheckAndRelease() {
        log.info("手动触发资源释放检查");
        checkAndAutoRelease();
    }
    
    /**
     * 获取资源释放统计信息
     */
    public String getReleaseStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("资源释放统计:\n");
        
        for (ResourceType resourceType : ResourceType.values()) {
            ResourceState state = resourcePool.getResourceState(resourceType);
            if (state != null) {
                stats.append(String.format("  %s: 可用=%s, 最后访问=%dms前, 访问次数=%d\n",
                    resourceType.getName(),
                    state.isAvailable(),
                    state.getTimeSinceLastAccess(),
                    state.getAccessCount()));
            }
        }
        
        return stats.toString();
    }
} 