package com.indigo.cache.extension.lock;

import com.indigo.cache.extension.lock.resource.FastRecoveryManager;
import com.indigo.cache.extension.lock.resource.ResourceType;
import com.indigo.cache.extension.lock.resource.ResourceState;
import com.indigo.cache.extension.lock.resource.RecoveryState;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 统一分布式锁管理器
 * 
 * 作为分布式锁系统的唯一对外入口，整合所有锁类型和功能：
 * 
 * 📋 架构层次：
 * ┌─────────────────────────────────────────┐
 * │              LockManager                │  ← 统一入口（对外暴露）
 * │        (extension.LockManager)          │
 * ├─────────────────────────────────────────┤
 * │  DistributedLockService (可重入锁)       │
 * │  ReadWriteLockService   (读写锁)        │  ← 底层实现（内部Bean）
 * │  FairLockService       (公平锁)        │
 * │  DeadlockDetector      (死锁检测)       │
 * │  LockPerformanceMonitor(性能监控)       │
 * └─────────────────────────────────────────┘
 * 
 * 🔧 核心特性：
 * 1. 统一API：提供一致的锁操作接口，屏蔽底层复杂性
 * 2. 多种锁类型：支持可重入锁、读写锁、公平锁
 * 3. 性能监控：集成锁性能监控和统计
 * 4. 死锁检测：集成死锁检测和预防机制
 * 5. 自动管理：自动选择合适的锁类型和超时策略
 * 6. 异常处理：统一的异常处理和日志记录
 * 
 * 🚀 使用示例：
 * ```java
 * // 注入统一管理器
 * @Autowired
 * private LockManager lockManager;
 * 
 * // 简单加锁
 * String lockValue = lockManager.tryLock("order", "123", 10);
 * 
 * // 便捷执行
 * Result result = lockManager.executeWithLock("order", "123", () -> {
 *     // 业务逻辑
 *     return processOrder();
 * });
 * 
 * // 读写锁
 * lockManager.executeWithReadLock("data", "key", () -> {
 *     return readData();
 * });
 * ```
 * 
 * ⚠️ 重要说明：
 * - 这是分布式锁的唯一对外入口，请勿直接使用底层服务
 * - 底层服务(DistributedLockService等)为内部Bean，不对外暴露
 * - 所有锁操作都应通过此管理器进行，确保统一监控和管理
 * 
 * <p><b>注意：</b>此类通过 {@link LockAutoConfiguration} 中的 {@code @Bean} 方法注册为 Bean，
 * 不需要 {@code @Component} 注解。如果同时使用 {@code @Component} 和 {@code @Bean}，
 * 会导致创建多个 Bean 实例，引发冲突。
 *
 * @author 史偕成
 * @date 2025/01/08
 * @version 2.0 (重构为统一入口架构)
 */
@Slf4j
public class LockManager {

    private final DistributedLockService distributedLockService;
    private final ReadWriteLockService readWriteLockService;
    private final FairLockService fairLockService;
    private final DeadlockDetector deadlockDetector;
    private final LockPerformanceMonitor performanceMonitor;
    private final FastRecoveryManager fastRecoveryManager;
    
    // 分布式死锁检测器（可选）
    private final DistributedDeadlockDetector distributedDeadlockDetector;
    
    // 延迟初始化相关字段
    private volatile boolean isInitialized = false;
    private final AtomicLong lastAccessTime = new AtomicLong(System.currentTimeMillis());

    public LockManager(DistributedLockService distributedLockService,
                      ReadWriteLockService readWriteLockService,
                      FairLockService fairLockService,
                      DeadlockDetector deadlockDetector,
                      LockPerformanceMonitor performanceMonitor,
                      FastRecoveryManager fastRecoveryManager,
                      DistributedDeadlockDetector distributedDeadlockDetector) {
        this.distributedLockService = distributedLockService;
        this.readWriteLockService = readWriteLockService;
        this.fairLockService = fairLockService;
        this.deadlockDetector = deadlockDetector;
        this.performanceMonitor = performanceMonitor;
        this.fastRecoveryManager = fastRecoveryManager;
        this.distributedDeadlockDetector = distributedDeadlockDetector;
        
        log.debug("LockManager Bean 已创建，采用延迟初始化策略");
    }
    
    /**
     * 确保服务已初始化
     * 采用延迟初始化策略，首次使用时才启动相关服务
     */
    private synchronized void ensureInitialized() {
        if (!isInitialized) {
            log.debug("首次使用分布式锁服务，开始初始化相关组件...");
            
            // 启动死锁检测器
            if (deadlockDetector != null) {
                log.debug("启动死锁检测器");
            }
            
            // 启动性能监控器
            if (performanceMonitor != null) {
                log.debug("启动性能监控器");
            }
            
            isInitialized = true;
            log.debug("分布式锁服务初始化完成");
        }
        
        // 更新最后访问时间
        lastAccessTime.set(System.currentTimeMillis());
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
        ensureInitialized();
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
                log.info("[LockManager] 获取锁成功: {}:{} 类型: {}", lockName, key, lockType);
            } else {
                performanceMonitor.recordLockFailure(lockName, key, startTime, "获取失败");
                log.info("[LockManager] 获取锁失败: {}:{} 类型: {}", lockName, key, lockType);
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
        ensureInitialized();
        long startTime = System.currentTimeMillis();
        String threadId = String.valueOf(Thread.currentThread().getId());
        
        try {
            performanceMonitor.recordLockAttempt(lockName, key, startTime);
            
            String lockValue = readWriteLockService.tryReadLock(lockName, key, timeout);
            
            if (lockValue != null) {
                performanceMonitor.recordLockSuccess(lockName, key, startTime, lockValue);
                deadlockDetector.recordLockAcquired(threadId, generateLockKey(lockName, key));
                log.info("[LockManager] 获取读锁成功: {}:{}", lockName, key);
            } else {
                performanceMonitor.recordLockFailure(lockName, key, startTime, "获取读锁失败");
                log.info("[LockManager] 获取读锁失败: {}:{}", lockName, key);
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
        ensureInitialized();
        long startTime = System.currentTimeMillis();
        String threadId = String.valueOf(Thread.currentThread().getId());
        
        try {
            performanceMonitor.recordLockAttempt(lockName, key, startTime);
            
            String lockValue = readWriteLockService.tryWriteLock(lockName, key, timeout);
            
            if (lockValue != null) {
                performanceMonitor.recordLockSuccess(lockName, key, startTime, lockValue);
                deadlockDetector.recordLockAcquired(threadId, generateLockKey(lockName, key));
                log.info("[LockManager] 获取写锁成功: {}:{}", lockName, key);
            } else {
                performanceMonitor.recordLockFailure(lockName, key, startTime, "获取写锁失败");
                log.info("[LockManager] 获取写锁失败: {}:{}", lockName, key);
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
        ensureInitialized();
        long startTime = System.currentTimeMillis();
        String threadId = String.valueOf(Thread.currentThread().getId());
        
        try {
            performanceMonitor.recordLockAttempt(lockName, key, startTime);
            deadlockDetector.recordLockWaitStart(threadId, generateLockKey(lockName, key));
            
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
                log.info("[LockManager] 等待获取锁成功: {}:{} 类型: {}", lockName, key, lockType);
            } else {
                performanceMonitor.recordLockFailure(lockName, key, startTime, "等待超时");
                deadlockDetector.recordLockWaitEnd(threadId, generateLockKey(lockName, key));
                log.info("[LockManager] 等待获取锁失败: {}:{} 类型: {}", lockName, key, lockType);
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
                log.info("[LockManager] 释放锁成功: {}:{} 类型: {}", lockName, key, lockType);
            } else {
                log.info("[LockManager] 释放锁失败: {}:{} 类型: {}", lockName, key, lockType);
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
                log.info("[LockManager] 释放读锁成功: {}:{}", lockName, key);
            } else {
                log.info("[LockManager] 释放读锁失败: {}:{}", lockName, key);
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
                log.info("[LockManager] 释放写锁成功: {}:{}", lockName, key);
            } else {
                log.info("[LockManager] 释放写锁失败: {}:{}", lockName, key);
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
            } catch (Exception e) {
                log.error("[LockManager] 执行操作异常: {}:{}", lockName, key, e);
                throw new RuntimeException("锁内操作执行失败", e);
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
            } catch (Exception e) {
                log.error("[LockManager] 读锁操作异常: {}:{}", lockName, key, e);
                throw new RuntimeException("读锁内操作执行失败", e);
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
            } catch (Exception e) {
                log.error("[LockManager] 写锁操作异常: {}:{}", lockName, key, e);
                throw new RuntimeException("写锁内操作执行失败", e);
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
     * 获取分布式死锁检测状态
     * 
     * @return 分布式死锁检测状态
     */
    public Map<String, Object> getDistributedDeadlockStatus() {
        if (distributedDeadlockDetector != null) {
            return distributedDeadlockDetector.getGlobalStatus();
        }
        return Collections.emptyMap();
    }

    /**
     * 检测全局死锁
     * 
     * @return 全局死锁环列表
     */
    public List<Set<String>> detectGlobalDeadlocks() {
        if (distributedDeadlockDetector != null) {
            return distributedDeadlockDetector.detectGlobalDeadlocks();
        }
        return Collections.emptyList();
    }

    /**
     * 同步本地状态到全局
     */
    public void syncLocalStateToGlobal() {
        if (distributedDeadlockDetector != null) {
            distributedDeadlockDetector.syncLocalStateToGlobal();
        }
    }

    /**
     * 启用/禁用全局检测
     * 
     * @param enabled 是否启用
     */
    public void setGlobalDetectionEnabled(boolean enabled) {
        if (distributedDeadlockDetector != null) {
            distributedDeadlockDetector.setGlobalDetectionEnabled(enabled);
        }
    }

    /**
     * 检查是否启用了分布式死锁检测
     * 
     * @return 是否启用
     */
    public boolean isDistributedDeadlockEnabled() {
        return distributedDeadlockDetector != null;
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
     * 获取当前初始化状态
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * 获取最后访问时间
     */
    public long getLastAccessTime() {
        return lastAccessTime.get();
    }
    
    /**
     * 手动触发初始化（用于测试或特殊情况）
     */
    public void forceInitialize() {
        ensureInitialized();
    }
    
    // ==================== 快速恢复相关方法 ====================
    
    /**
     * 快速恢复资源
     * 
     * @param resourceType 资源类型
     * @param resourceClass 资源类
     * @param <T> 资源类型
     * @return 恢复的资源实例
     */
    public <T> T fastRecoverResource(ResourceType resourceType, Class<T> resourceClass) {
        log.debug("开始快速恢复资源: {} - {}", resourceType, resourceClass.getSimpleName());
        return fastRecoveryManager.fastRecover(resourceType, resourceClass);
    }
    
    /**
     * 检查资源是否可用
     * 
     * @param resourceType 资源类型
     * @return 是否可用
     */
    public boolean isResourceAvailable(ResourceType resourceType) {
        return fastRecoveryManager.getResourcePool().isResourceAvailable(resourceType);
    }
    
    /**
     * 获取资源状态
     * 
     * @param resourceType 资源类型
     * @return 资源状态
     */
    public ResourceState getResourceState(ResourceType resourceType) {
        return fastRecoveryManager.getResourcePool().getResourceState(resourceType);
    }
    
    /**
     * 获取恢复状态
     * 
     * @param resourceType 资源类型
     * @return 恢复状态
     */
    public RecoveryState getRecoveryState(ResourceType resourceType) {
        return fastRecoveryManager.getRecoveryState(resourceType);
    }
    
    /**
     * 手动触发资源释放检查
     */
    public void triggerResourceReleaseCheck() {
        log.info("手动触发资源释放检查");
        // 这里可以调用AutoReleaseChecker的方法
        // 或者直接通过Spring容器获取Bean
    }
    
    /**
     * 获取资源管理统计信息
     * 
     * @return 统计信息字符串
     */
    public String getResourceManagementStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("资源管理统计:\n");
        
        // 添加资源状态统计
        for (ResourceType resourceType : ResourceType.values()) {
            ResourceState state = getResourceState(resourceType);
            if (state != null) {
                stats.append(String.format("  %s: 可用=%s, 最后访问=%dms前, 访问次数=%d\n",
                    resourceType.getName(),
                    state.isAvailable(),
                    state.getTimeSinceLastAccess(),
                    state.getAccessCount()));
            }
        }
        
        // 添加恢复状态统计
        stats.append("\n恢复状态统计:\n");
        for (ResourceType resourceType : ResourceType.values()) {
            RecoveryState recoveryState = getRecoveryState(resourceType);
            if (recoveryState != null) {
                stats.append(String.format("  %s: 恢复次数=%d, 平均恢复时间=%.2fms\n",
                    resourceType.getName(),
                    recoveryState.getRecoveryCount(),
                    recoveryState.getAverageRecoveryTime()));
            }
        }
        
        return stats.toString();
    }
} 