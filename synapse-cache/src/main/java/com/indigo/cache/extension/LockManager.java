package com.indigo.cache.extension;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 统一分布式锁管理器
 * 整合可重入锁、读写锁、公平锁等功能，提供统一的API
 * 
 * 特性：
 * 1. 统一API：提供一致的锁操作接口
 * 2. 多种锁类型：支持可重入锁、读写锁、公平锁
 * 3. 性能监控：集成锁性能监控
 * 4. 死锁检测：集成死锁检测和预防
 * 5. 自动管理：自动选择合适的锁类型
 *
 * @author 史偕成
 * @date 2025/01/08
 */
@Slf4j
@Component
public class LockManager {

    private final DistributedLockService distributedLockService;
    private final ReadWriteLockService readWriteLockService;
    private final FairLockService fairLockService;
    private final DeadlockDetector deadlockDetector;
    private final LockPerformanceMonitor performanceMonitor;

    public LockManager(DistributedLockService distributedLockService,
                      ReadWriteLockService readWriteLockService,
                      FairLockService fairLockService,
                      DeadlockDetector deadlockDetector,
                      LockPerformanceMonitor performanceMonitor) {
        this.distributedLockService = distributedLockService;
        this.readWriteLockService = readWriteLockService;
        this.fairLockService = fairLockService;
        this.deadlockDetector = deadlockDetector;
        this.performanceMonitor = performanceMonitor;
    }

    /**
     * 锁类型枚举
     */
    public enum LockType {
        REENTRANT,  // 可重入锁
        READ_WRITE, // 读写锁
        FAIR        // 公平锁
    }

    /**
     * 锁操作类型枚举
     */
    public enum LockOperation {
        READ,   // 读操作
        WRITE   // 写操作
    }

    /**
     * 尝试获取锁（可重入锁）
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @param timeout 超时时间（秒）
     * @return 锁值，null表示获取失败
     */
    public String tryLock(String lockName, String key, int timeout) {
        return tryLock(lockName, key, timeout, LockType.REENTRANT);
    }

    /**
     * 尝试获取锁
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @param timeout 超时时间（秒）
     * @param lockType 锁类型
     * @return 锁值，null表示获取失败
     */
    public String tryLock(String lockName, String key, int timeout, LockType lockType) {
        long startTime = System.currentTimeMillis();
        String threadId = String.valueOf(Thread.currentThread().getId());
        
        try {
            performanceMonitor.recordLockAttempt(lockName, key, startTime);
            
            String lockValue = null;
            switch (lockType) {
                case REENTRANT:
                    lockValue = distributedLockService.tryLock(lockName, key, timeout);
                    break;
                case READ_WRITE:
                    // 默认使用写锁
                    lockValue = readWriteLockService.tryWriteLock(lockName, key, timeout);
                    break;
                case FAIR:
                    lockValue = fairLockService.tryFairLock(lockName, key, timeout);
                    break;
            }
            
            if (lockValue != null) {
                performanceMonitor.recordLockSuccess(lockName, key, startTime, lockValue);
                deadlockDetector.recordLockAcquired(threadId, generateLockKey(lockName, key));
                log.debug("[LockManager] 获取锁成功: {}:{} 类型: {}", lockName, key, lockType);
            } else {
                performanceMonitor.recordLockFailure(lockName, key, startTime, "获取失败");
                log.debug("[LockManager] 获取锁失败: {}:{} 类型: {}", lockName, key, lockType);
            }
            
            return lockValue;
        } catch (Exception e) {
            performanceMonitor.recordLockFailure(lockName, key, startTime, e.getMessage());
            log.error("[LockManager] 获取锁异常: {}:{} 类型: {}", lockName, key, lockType, e);
            return null;
        }
    }

    /**
     * 尝试获取读锁
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @param timeout 超时时间（秒）
     * @return 锁值，null表示获取失败
     */
    public String tryReadLock(String lockName, String key, int timeout) {
        long startTime = System.currentTimeMillis();
        String threadId = String.valueOf(Thread.currentThread().getId());
        
        try {
            performanceMonitor.recordLockAttempt(lockName, key, startTime);
            
            String lockValue = readWriteLockService.tryReadLock(lockName, key, timeout);
            
            if (lockValue != null) {
                performanceMonitor.recordLockSuccess(lockName, key, startTime, lockValue);
                deadlockDetector.recordLockAcquired(threadId, generateLockKey(lockName, key));
                log.debug("[LockManager] 获取读锁成功: {}:{}", lockName, key);
            } else {
                performanceMonitor.recordLockFailure(lockName, key, startTime, "获取读锁失败");
                log.debug("[LockManager] 获取读锁失败: {}:{}", lockName, key);
            }
            
            return lockValue;
        } catch (Exception e) {
            performanceMonitor.recordLockFailure(lockName, key, startTime, e.getMessage());
            log.error("[LockManager] 获取读锁异常: {}:{}", lockName, key, e);
            return null;
        }
    }

    /**
     * 尝试获取写锁
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @param timeout 超时时间（秒）
     * @return 锁值，null表示获取失败
     */
    public String tryWriteLock(String lockName, String key, int timeout) {
        long startTime = System.currentTimeMillis();
        String threadId = String.valueOf(Thread.currentThread().getId());
        
        try {
            performanceMonitor.recordLockAttempt(lockName, key, startTime);
            
            String lockValue = readWriteLockService.tryWriteLock(lockName, key, timeout);
            
            if (lockValue != null) {
                performanceMonitor.recordLockSuccess(lockName, key, startTime, lockValue);
                deadlockDetector.recordLockAcquired(threadId, generateLockKey(lockName, key));
                log.debug("[LockManager] 获取写锁成功: {}:{}", lockName, key);
            } else {
                performanceMonitor.recordLockFailure(lockName, key, startTime, "获取写锁失败");
                log.debug("[LockManager] 获取写锁失败: {}:{}", lockName, key);
            }
            
            return lockValue;
        } catch (Exception e) {
            performanceMonitor.recordLockFailure(lockName, key, startTime, e.getMessage());
            log.error("[LockManager] 获取写锁异常: {}:{}", lockName, key, e);
            return null;
        }
    }

    /**
     * 等待并获取锁
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @param lockTimeout 锁超时时间（秒）
     * @param waitTimeout 等待超时时间（秒）
     * @param lockType 锁类型
     * @return 锁值，null表示获取失败
     */
    public String lock(String lockName, String key, int lockTimeout, int waitTimeout, LockType lockType) {
        long startTime = System.currentTimeMillis();
        String threadId = String.valueOf(Thread.currentThread().getId());
        
        try {
            performanceMonitor.recordLockAttempt(lockName, key, startTime);
            deadlockDetector.recordLockWait(threadId, generateLockKey(lockName, key));
            
            String lockValue = null;
            switch (lockType) {
                case REENTRANT:
                    lockValue = distributedLockService.lock(lockName, key, lockTimeout, waitTimeout);
                    break;
                case READ_WRITE:
                    lockValue = readWriteLockService.writeLock(lockName, key, lockTimeout, waitTimeout);
                    break;
                case FAIR:
                    lockValue = fairLockService.fairLock(lockName, key, lockTimeout, waitTimeout);
                    break;
            }
            
            if (lockValue != null) {
                performanceMonitor.recordLockSuccess(lockName, key, startTime, lockValue);
                deadlockDetector.recordLockWaitEnd(threadId, generateLockKey(lockName, key));
                log.debug("[LockManager] 等待获取锁成功: {}:{} 类型: {}", lockName, key, lockType);
            } else {
                performanceMonitor.recordLockFailure(lockName, key, startTime, "等待超时");
                deadlockDetector.recordLockWaitEnd(threadId, generateLockKey(lockName, key));
                log.debug("[LockManager] 等待获取锁失败: {}:{} 类型: {}", lockName, key, lockType);
            }
            
            return lockValue;
        } catch (Exception e) {
            performanceMonitor.recordLockFailure(lockName, key, startTime, e.getMessage());
            deadlockDetector.recordLockWaitEnd(threadId, generateLockKey(lockName, key));
            log.error("[LockManager] 等待获取锁异常: {}:{} 类型: {}", lockName, key, lockType, e);
            return null;
        }
    }

    /**
     * 释放锁
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @param lockValue 锁值
     * @param lockType 锁类型
     * @return 是否释放成功
     */
    public boolean unlock(String lockName, String key, String lockValue, LockType lockType) {
        long startTime = System.currentTimeMillis();
        String threadId = String.valueOf(Thread.currentThread().getId());
        String lockKey = generateLockKey(lockName, key);
        
        try {
            boolean released = false;
            switch (lockType) {
                case REENTRANT:
                    released = distributedLockService.unlock(lockName, key, lockValue);
                    break;
                case READ_WRITE:
                    // 需要根据锁值判断是读锁还是写锁
                    released = readWriteLockService.releaseWriteLock(lockName, key, lockValue);
                    if (!released) {
                        released = readWriteLockService.releaseReadLock(lockName, key);
                    }
                    break;
                case FAIR:
                    released = fairLockService.releaseFairLock(lockName, key, lockValue);
                    break;
            }
            
            if (released) {
                long holdTime = System.currentTimeMillis() - startTime;
                performanceMonitor.recordLockRelease(lockName, key, lockValue, holdTime);
                deadlockDetector.recordLockReleased(threadId, lockKey);
                log.debug("[LockManager] 释放锁成功: {}:{} 类型: {}", lockName, key, lockType);
            } else {
                log.debug("[LockManager] 释放锁失败: {}:{} 类型: {}", lockName, key, lockType);
            }
            
            return released;
        } catch (Exception e) {
            log.error("[LockManager] 释放锁异常: {}:{} 类型: {}", lockName, key, lockType, e);
            return false;
        }
    }

    /**
     * 释放读锁
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @return 是否释放成功
     */
    public boolean releaseReadLock(String lockName, String key) {
        long startTime = System.currentTimeMillis();
        String threadId = String.valueOf(Thread.currentThread().getId());
        String lockKey = generateLockKey(lockName, key);
        
        try {
            boolean released = readWriteLockService.releaseReadLock(lockName, key);
            
            if (released) {
                long holdTime = System.currentTimeMillis() - startTime;
                performanceMonitor.recordLockRelease(lockName, key, "read", holdTime);
                deadlockDetector.recordLockReleased(threadId, lockKey);
                log.debug("[LockManager] 释放读锁成功: {}:{}", lockName, key);
            } else {
                log.debug("[LockManager] 释放读锁失败: {}:{}", lockName, key);
            }
            
            return released;
        } catch (Exception e) {
            log.error("[LockManager] 释放读锁异常: {}:{}", lockName, key, e);
            return false;
        }
    }

    /**
     * 释放写锁
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @param lockValue 锁值
     * @return 是否释放成功
     */
    public boolean releaseWriteLock(String lockName, String key, String lockValue) {
        long startTime = System.currentTimeMillis();
        String threadId = String.valueOf(Thread.currentThread().getId());
        String lockKey = generateLockKey(lockName, key);
        
        try {
            boolean released = readWriteLockService.releaseWriteLock(lockName, key, lockValue);
            
            if (released) {
                long holdTime = System.currentTimeMillis() - startTime;
                performanceMonitor.recordLockRelease(lockName, key, lockValue, holdTime);
                deadlockDetector.recordLockReleased(threadId, lockKey);
                log.debug("[LockManager] 释放写锁成功: {}:{}", lockName, key);
            } else {
                log.debug("[LockManager] 释放写锁失败: {}:{}", lockName, key);
            }
            
            return released;
        } catch (Exception e) {
            log.error("[LockManager] 释放写锁异常: {}:{}", lockName, key, e);
            return false;
        }
    }

    /**
     * 便捷方法：获取锁并执行操作
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @param action 需要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果，获取锁失败返回null
     */
    public <T> T executeWithLock(String lockName, String key, LockAction<T> action) {
        return executeWithLock(lockName, key, action, LockType.REENTRANT);
    }

    /**
     * 便捷方法：获取锁并执行操作
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @param action 需要执行的操作
     * @param lockType 锁类型
     * @param <T> 返回值类型
     * @return 操作结果，获取锁失败返回null
     */
    public <T> T executeWithLock(String lockName, String key, LockAction<T> action, LockType lockType) {
        String lockValue = tryLock(lockName, key, 10, lockType);
        if (lockValue != null) {
            try {
                return action.execute();
            } finally {
                unlock(lockName, key, lockValue, lockType);
            }
        }
        return null;
    }

    /**
     * 便捷方法：获取读锁并执行操作
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @param action 需要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果，获取锁失败返回null
     */
    public <T> T executeWithReadLock(String lockName, String key, LockAction<T> action) {
        String lockValue = tryReadLock(lockName, key, 10);
        if (lockValue != null) {
            try {
                return action.execute();
            } finally {
                releaseReadLock(lockName, key);
            }
        }
        return null;
    }

    /**
     * 便捷方法：获取写锁并执行操作
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @param action 需要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果，获取锁失败返回null
     */
    public <T> T executeWithWriteLock(String lockName, String key, LockAction<T> action) {
        String lockValue = tryWriteLock(lockName, key, 10);
        if (lockValue != null) {
            try {
                return action.execute();
            } finally {
                releaseWriteLock(lockName, key, lockValue);
            }
        }
        return null;
    }

    /**
     * 获取锁统计信息
     * 
     * @param lockName 锁名称
     * @return 统计信息
     */
    public LockPerformanceMonitor.LockStats getLockStats(String lockName) {
        return performanceMonitor.getLockStats(lockName);
    }

    /**
     * 获取所有锁统计信息
     * 
     * @return 所有统计信息
     */
    public Map<String, LockPerformanceMonitor.LockStats> getAllLockStats() {
        return performanceMonitor.getAllLockStats();
    }

    /**
     * 获取全局统计信息
     * 
     * @return 全局统计
     */
    public LockPerformanceMonitor.GlobalStats getGlobalStats() {
        return performanceMonitor.getGlobalStats();
    }

    /**
     * 获取死锁检测状态
     * 
     * @return 死锁检测状态
     */
    public Map<String, Object> getDeadlockStatus() {
        return deadlockDetector.getStatus();
    }

    /**
     * 重置统计信息
     * 
     * @param lockName 锁名称，null表示重置所有
     */
    public void resetStats(String lockName) {
        performanceMonitor.resetStats(lockName);
    }

    /**
     * 生成锁键
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @return 锁键
     */
    private String generateLockKey(String lockName, String key) {
        return "lock:" + lockName + ":" + key;
    }

    /**
     * 锁操作接口
     */
    @FunctionalInterface
    public interface LockAction<T> {
        T execute();
    }
} 