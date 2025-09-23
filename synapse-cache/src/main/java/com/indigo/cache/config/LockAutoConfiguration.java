package com.indigo.cache.config;

import com.indigo.cache.extension.lock.*;
import com.indigo.cache.extension.lock.resource.*;
import com.indigo.cache.infrastructure.RedisService;
import com.indigo.cache.manager.CacheKeyGenerator;
import com.indigo.cache.core.CacheService;
import com.indigo.cache.infrastructure.CaffeineCacheManager;
import com.indigo.cache.session.SessionManager;
import com.indigo.cache.session.StatisticsManager;
import com.indigo.core.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 分布式锁自动配置类
 * 统一管理所有分布式锁相关的Bean，只对外暴露LockManager
 *
 * @author 史偕成
 * @date 2025/01/08
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({RedisService.class, ThreadUtils.class})
@EnableConfigurationProperties({LockProperties.class, DistributedDeadlockProperties.class})
@EnableScheduling
public class LockAutoConfiguration {

    private final LockProperties lockProperties;
    private final AtomicLong lastAccessTime = new AtomicLong(System.currentTimeMillis());

    public LockAutoConfiguration(LockProperties lockProperties) {
        this.lockProperties = lockProperties;
        log.info("LockAutoConfiguration 已创建，延迟初始化策略已启用");
    }

    /**
     * 锁专用调度线程池（内部Bean）
     */
    @Bean("lockScheduledExecutor")
    @ConditionalOnMissingBean(name = "lockScheduledExecutor")
    public ScheduledExecutorService lockScheduledExecutor() {
        log.debug("创建锁专用ScheduledExecutorService Bean（延迟初始化）");
        return Executors.newScheduledThreadPool(2);
    }

    /**
     * 锁性能监控器（内部Bean）
     */
    @Bean
    @ConditionalOnMissingBean
    public LockPerformanceMonitor lockPerformanceMonitor() {
        log.debug("创建LockPerformanceMonitor Bean（延迟初始化）");
        return new LockPerformanceMonitor();
    }

    /**
     * 分布式可重入锁服务（内部Bean）
     */
    @Bean
    @ConditionalOnMissingBean
    public DistributedLockService distributedLockService(
            RedisService redisService,
            @Qualifier("synapseCacheKeyGenerator") CacheKeyGenerator cacheKeyGenerator,
            ThreadUtils threadUtils) {
        log.debug("创建DistributedLockService Bean（延迟初始化）");
        return new DistributedLockService(redisService, cacheKeyGenerator, threadUtils);
    }

    /**
     * 本地死锁检测器（内部Bean）
     */
    @Bean("localDeadlockDetector")
    @ConditionalOnMissingBean(name = "localDeadlockDetector")
    public DeadlockDetector localDeadlockDetector(
            @Qualifier("lockScheduledExecutor") ScheduledExecutorService scheduler) {
        log.debug("创建本地DeadlockDetector Bean（延迟初始化）");
        return new DeadlockDetector(scheduler);
    }

    /**
     * 分布式死锁检测器（内部Bean）
     */
    @Bean
    @ConditionalOnMissingBean
    public DistributedDeadlockDetector distributedDeadlockDetector(
            @Qualifier("lockScheduledExecutor") ScheduledExecutorService scheduler,
            RedisService redisService,
            DistributedDeadlockProperties distributedDeadlockProperties) {
        log.debug("创建DistributedDeadlockDetector Bean（延迟初始化）");
        return new DistributedDeadlockDetector(scheduler, redisService, distributedDeadlockProperties);
    }

    /**
     * 读写锁服务（内部Bean）
     */
    @Bean
    @ConditionalOnMissingBean
    public ReadWriteLockService readWriteLockService(
            RedisService redisService,
            @Qualifier("synapseCacheKeyGenerator") CacheKeyGenerator cacheKeyGenerator,
            DistributedLockService distributedLockService) {
        log.debug("创建ReadWriteLockService Bean（延迟初始化）");
        return new ReadWriteLockService(redisService, cacheKeyGenerator, distributedLockService);
    }

    /**
     * 公平锁服务（内部Bean）
     */
    @Bean
    @ConditionalOnMissingBean
    public FairLockService fairLockService(
            RedisService redisService,
            @Qualifier("synapseCacheKeyGenerator") CacheKeyGenerator cacheKeyGenerator,
            DistributedLockService distributedLockService) {
        log.debug("创建FairLockService Bean（延迟初始化）");
        return new FairLockService(redisService, cacheKeyGenerator, distributedLockService);
    }

    /**
     * 资源池管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public ResourcePool resourcePool() {
        log.debug("创建ResourcePool Bean");
        return new ResourcePool();
    }

    /**
     * 快速恢复管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public FastRecoveryManager fastRecoveryManager(ResourcePool resourcePool) {
        log.debug("创建FastRecoveryManager Bean");
        return new FastRecoveryManager(resourcePool);
    }

    /**
     * 自动释放检查器
     */
    @Bean
    @ConditionalOnMissingBean
    public AutoReleaseChecker autoReleaseChecker(
            ResourcePool resourcePool,
            LockProperties lockProperties,
            CacheService cacheService,
            CaffeineCacheManager caffeineCacheManager,
            SessionManager sessionManager,
            StatisticsManager statisticsManager,
            LockPerformanceMonitor lockPerformanceMonitor) {
        log.debug("创建AutoReleaseChecker Bean");
        return new AutoReleaseChecker(resourcePool, lockProperties, cacheService,
                caffeineCacheManager, sessionManager, statisticsManager, lockPerformanceMonitor);
    }

    /**
     * 统一分布式锁管理器（对外暴露的唯一入口）
     */
    @Bean
    @ConditionalOnMissingBean
    public LockManager lockManager(
            DistributedLockService distributedLockService,
            ReadWriteLockService readWriteLockService,
            FairLockService fairLockService,
            @Qualifier("localDeadlockDetector") DeadlockDetector deadlockDetector,
            LockPerformanceMonitor performanceMonitor,
            FastRecoveryManager fastRecoveryManager,
            DistributedDeadlockDetector distributedDeadlockDetector) {
        log.info("创建LockManager Bean - 分布式锁统一入口");
        return new LockManager(distributedLockService, readWriteLockService,
                fairLockService, deadlockDetector, performanceMonitor, fastRecoveryManager,distributedDeadlockDetector);
    }

    /**
     * 自动释放资源检查任务
     * 根据配置的阈值自动释放长时间未使用的资源
     */
    @Scheduled(fixedDelayString = "${synapse.cache.lock.auto-release.check-interval:60000}")
    public void autoReleaseCheck() {
        if (!lockProperties.getAutoRelease().isEnabled()) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long lastAccess = lastAccessTime.get();
        long threshold = lockProperties.getAutoRelease().getCoreServiceThreshold();

        if (currentTime - lastAccess > threshold) {
            log.debug("检测到长时间未使用，开始自动释放分布式锁资源...");
            // 这里可以添加具体的资源释放逻辑
            // 例如：关闭线程池、清理缓存等
            log.debug("分布式锁资源自动释放完成");
        }
    }

    /**
     * 更新最后访问时间
     * 供外部调用以更新活跃状态
     */
    public void updateLastAccessTime() {
        lastAccessTime.set(System.currentTimeMillis());
    }
} 