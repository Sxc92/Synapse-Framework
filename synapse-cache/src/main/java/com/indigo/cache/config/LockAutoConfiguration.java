package com.indigo.cache.config;

import com.indigo.cache.extension.lock.*;
import com.indigo.cache.infrastructure.RedisService;
import com.indigo.cache.manager.CacheKeyGenerator;
import com.indigo.core.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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
public class LockAutoConfiguration {

    /**
     * 锁专用调度线程池（内部Bean）
     */
    @Bean("lockScheduledExecutor")
    @ConditionalOnMissingBean(name = "lockScheduledExecutor")
    public ScheduledExecutorService lockScheduledExecutor() {
        log.info("创建锁专用ScheduledExecutorService Bean");
        return Executors.newScheduledThreadPool(2);
    }

    /**
     * 锁性能监控器（内部Bean）
     */
    @Bean
    @ConditionalOnMissingBean
    public LockPerformanceMonitor lockPerformanceMonitor() {
        log.info("创建LockPerformanceMonitor Bean");
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
        log.info("创建DistributedLockService Bean");
        return new DistributedLockService(redisService, cacheKeyGenerator, threadUtils);
    }

    /**
     * 死锁检测器（内部Bean）
     */
    @Bean
    @ConditionalOnMissingBean
    public DeadlockDetector deadlockDetector(
            RedisService redisService,
            @Qualifier("synapseCacheKeyGenerator") CacheKeyGenerator cacheKeyGenerator,
            DistributedLockService distributedLockService,
            @Qualifier("lockScheduledExecutor") ScheduledExecutorService scheduler) {
        log.info("创建DeadlockDetector Bean");
        return new DeadlockDetector(redisService, cacheKeyGenerator, distributedLockService, scheduler);
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
        log.info("创建ReadWriteLockService Bean");
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
        log.info("创建FairLockService Bean");
        return new FairLockService(redisService, cacheKeyGenerator, distributedLockService);
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
            DeadlockDetector deadlockDetector,
            LockPerformanceMonitor performanceMonitor) {
        log.info("创建LockManager Bean - 分布式锁统一入口");
        return new LockManager(distributedLockService, readWriteLockService, 
                             fairLockService, deadlockDetector, performanceMonitor);
    }
} 