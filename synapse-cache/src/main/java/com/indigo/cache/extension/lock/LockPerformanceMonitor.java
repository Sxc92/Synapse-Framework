package com.indigo.cache.extension.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁性能监控器
 * 监控锁的获取、释放、等待时间等性能指标
 * 
 * 监控指标：
 * 1. 锁获取成功率
 * 2. 锁等待时间
 * 3. 锁持有时间
 * 4. 锁竞争情况
 * 5. 死锁检测次数
 *
 * @author 史偕成
 * @date 2025/01/08
 */
@Slf4j
@Component
public class LockPerformanceMonitor {

    // 锁性能统计：锁名称 -> 统计信息
    private final ConcurrentHashMap<String, LockStats> lockStats = new ConcurrentHashMap<>();
    
    // 全局统计
    private final LongAdder totalLockAttempts = new LongAdder();
    private final LongAdder totalLockSuccesses = new LongAdder();
    private final LongAdder totalLockFailures = new LongAdder();
    private final LongAdder totalDeadlockDetections = new LongAdder();
    
    // 性能阈值配置
    private static final long SLOW_LOCK_THRESHOLD_MS = 1000; // 慢锁阈值
    private static final long DEADLOCK_DETECTION_THRESHOLD_MS = 5000; // 死锁检测阈值

    /**
     * 记录锁获取尝试
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @param startTime 开始时间
     */
    public void recordLockAttempt(String lockName, String key, long startTime) {
        totalLockAttempts.increment();
        LockStats stats = getOrCreateStats(lockName);
        stats.attempts.increment();
        stats.lastAttemptTime = System.currentTimeMillis();
        
        log.info("[LockMonitor] 记录锁获取尝试: {}:{}", lockName, key);
    }

    /**
     * 记录锁获取成功
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @param startTime 开始时间
     * @param lockValue 锁值
     */
    public void recordLockSuccess(String lockName, String key, long startTime, String lockValue) {
        totalLockSuccesses.increment();
        long duration = System.currentTimeMillis() - startTime;
        
        LockStats stats = getOrCreateStats(lockName);
        stats.successes.increment();
        stats.totalWaitTime.add(duration);
        stats.maxWaitTime.updateAndGet(current -> Math.max(current, duration));
        stats.minWaitTime.updateAndGet(current -> current == 0 ? duration : Math.min(current, duration));
        stats.lastSuccessTime = System.currentTimeMillis();
        
        // 检查是否为慢锁
        if (duration > SLOW_LOCK_THRESHOLD_MS) {
            stats.slowLocks.increment();
            log.warn("[LockMonitor] 检测到慢锁: {}:{} 耗时: {}ms", lockName, key, duration);
        }
        
        log.info("[LockMonitor] 记录锁获取成功: {}:{} 耗时: {}ms", lockName, key, duration);
    }

    /**
     * 记录锁获取失败
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @param startTime 开始时间
     * @param reason 失败原因
     */
    public void recordLockFailure(String lockName, String key, long startTime, String reason) {
        totalLockFailures.increment();
        long duration = System.currentTimeMillis() - startTime;
        
        LockStats stats = getOrCreateStats(lockName);
        stats.failures.increment();
        stats.totalWaitTime.add(duration);
        stats.lastFailureTime = System.currentTimeMillis();
        
        log.info("[LockMonitor] 记录锁获取失败: {}:{} 原因: {} 耗时: {}ms", lockName, key, reason, duration);
    }

    /**
     * 记录锁释放
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @param lockValue 锁值
     * @param holdTime 持有时间
     */
    public void recordLockRelease(String lockName, String key, String lockValue, long holdTime) {
        LockStats stats = getOrCreateStats(lockName);
        stats.releases.increment();
        stats.totalHoldTime.add(holdTime);
        stats.maxHoldTime.updateAndGet(current -> Math.max(current, holdTime));
        stats.minHoldTime.updateAndGet(current -> current == 0 ? holdTime : Math.min(current, holdTime));
        stats.lastReleaseTime = System.currentTimeMillis();
        
        log.info("[LockMonitor] 记录锁释放: {}:{} 持有时间: {}ms", lockName, key, holdTime);
    }

    /**
     * 记录死锁检测
     * 
     * @param deadlockCycles 死锁环数量
     * @param detectionTime 检测耗时
     */
    public void recordDeadlockDetection(int deadlockCycles, long detectionTime) {
        totalDeadlockDetections.increment();
        
        if (detectionTime > DEADLOCK_DETECTION_THRESHOLD_MS) {
            log.warn("[LockMonitor] 死锁检测耗时过长: {}ms 死锁环数量: {}", detectionTime, deadlockCycles);
        }
        
        log.info("[LockMonitor] 死锁检测完成: 耗时: {}ms 死锁环数量: {}", detectionTime, deadlockCycles);
    }

    /**
     * 记录锁竞争
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @param waitingThreads 等待线程数
     */
    public void recordLockContention(String lockName, String key, int waitingThreads) {
        LockStats stats = getOrCreateStats(lockName);
        stats.maxWaitingThreads.updateAndGet(current -> Math.max(current, waitingThreads));
        stats.totalContentionEvents.increment();
        
        if (waitingThreads > 5) {
            log.warn("[LockMonitor] 检测到锁竞争: {}:{} 等待线程数: {}", lockName, key, waitingThreads);
        }
        
        log.info("[LockMonitor] 记录锁竞争: {}:{} 等待线程数: {}", lockName, key, waitingThreads);
    }

    /**
     * 获取锁统计信息
     * 
     * @param lockName 锁名称
     * @return 统计信息
     */
    public LockStats getLockStats(String lockName) {
        return lockStats.get(lockName);
    }

    /**
     * 获取所有锁统计信息
     * 
     * @return 所有统计信息
     */
    public Map<String, LockStats> getAllLockStats() {
        return new ConcurrentHashMap<>(lockStats);
    }

    /**
     * 获取全局统计信息
     * 
     * @return 全局统计
     */
    public GlobalStats getGlobalStats() {
        GlobalStats stats = new GlobalStats();
        stats.totalAttempts = totalLockAttempts.sum();
        stats.totalSuccesses = totalLockSuccesses.sum();
        stats.totalFailures = totalLockFailures.sum();
        stats.totalDeadlockDetections = totalDeadlockDetections.sum();
        
        if (stats.totalAttempts > 0) {
            stats.successRate = (double) stats.totalSuccesses / stats.totalAttempts;
        }
        
        return stats;
    }

    /**
     * 重置统计信息
     * 
     * @param lockName 锁名称，null表示重置所有
     */
    public void resetStats(String lockName) {
        if (lockName == null) {
            lockStats.clear();
            totalLockAttempts.reset();
            totalLockSuccesses.reset();
            totalLockFailures.reset();
            totalDeadlockDetections.reset();
            log.info("[LockMonitor] 重置所有统计信息");
        } else {
            lockStats.remove(lockName);
            log.info("[LockMonitor] 重置锁统计信息: {}", lockName);
        }
    }

    /**
     * 获取或创建锁统计信息
     * 
     * @param lockName 锁名称
     * @return 统计信息
     */
    private LockStats getOrCreateStats(String lockName) {
        return lockStats.computeIfAbsent(lockName, k -> new LockStats());
    }

    /**
     * 锁统计信息
     */
    public static class LockStats {
        public final LongAdder attempts = new LongAdder();
        public final LongAdder successes = new LongAdder();
        public final LongAdder failures = new LongAdder();
        public final LongAdder releases = new LongAdder();
        public final LongAdder slowLocks = new LongAdder();
        public final LongAdder totalWaitTime = new LongAdder();
        public final LongAdder totalHoldTime = new LongAdder();
        public final LongAdder totalContentionEvents = new LongAdder();
        
        public final AtomicLong maxWaitTime = new AtomicLong(0);
        public final AtomicLong minWaitTime = new AtomicLong(0);
        public final AtomicLong maxHoldTime = new AtomicLong(0);
        public final AtomicLong minHoldTime = new AtomicLong(0);
        public final AtomicLong maxWaitingThreads = new AtomicLong(0);
        
        public long lastAttemptTime = 0;
        public long lastSuccessTime = 0;
        public long lastFailureTime = 0;
        public long lastReleaseTime = 0;

        /**
         * 获取成功率
         */
        public double getSuccessRate() {
            long total = attempts.sum();
            return total > 0 ? (double) successes.sum() / total : 0.0;
        }

        /**
         * 获取平均等待时间
         */
        public double getAverageWaitTime() {
            long total = successes.sum();
            return total > 0 ? (double) totalWaitTime.sum() / total : 0.0;
        }

        /**
         * 获取平均持有时间
         */
        public double getAverageHoldTime() {
            long total = releases.sum();
            return total > 0 ? (double) totalHoldTime.sum() / total : 0.0;
        }

        /**
         * 获取慢锁比例
         */
        public double getSlowLockRate() {
            long total = successes.sum();
            return total > 0 ? (double) slowLocks.sum() / total : 0.0;
        }
    }

    /**
     * 全局统计信息
     */
    public static class GlobalStats {
        public long totalAttempts = 0;
        public long totalSuccesses = 0;
        public long totalFailures = 0;
        public long totalDeadlockDetections = 0;
        public double successRate = 0.0;
    }
} 