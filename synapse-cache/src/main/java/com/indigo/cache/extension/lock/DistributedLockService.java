package com.indigo.cache.extension.lock;

import com.indigo.cache.infrastructure.RedisService;
import com.indigo.cache.manager.CacheKeyGenerator;
import com.indigo.core.utils.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 * 分布式可重入锁服务（内部实现）
 * 
 * ⚠️ 注意：这是内部实现类，请勿直接使用！
 * 应该通过 LockManager 统一入口访问分布式锁功能
 * 
 * 🔧 技术实现：
 * 1. 可重入锁：同一线程多次加锁只请求一次Redis，重入计数本地维护
 * 2. 分布式多节点看门狗：锁value包含nodeId:threadId:uuid，只有持有者节点续期
 * 3. 加锁/解锁/续期均用Lua脚本，保证原子性
 * 4. 自动续期：业务未完成时自动延长锁过期时间
 * 5. 支持便捷执行
 * 
 * 📋 正确用法：
 * ```java
 * @Autowired
 * private LockManager lockManager;  // 使用统一入口
 * 
 * lockManager.executeWithLock("order", "123", () -> {
 *     // 业务逻辑
 *     return processOrder();
 * });
 * ```
 *
 * @author 史偕成
 * @date 2025/05/16 15:00
 * @version 2.0 (内部实现，通过LockManager访问)
 */
@Slf4j
public class DistributedLockService implements DisposableBean {

    private final RedisService redisService;
    private final CacheKeyGenerator keyGenerator;

    // 默认锁超时时间（秒）
    private static final int DEFAULT_LOCK_TIMEOUT = 10;
    // 默认重试间隔（毫秒）
    private static final long DEFAULT_RETRY_INTERVAL = 100;
    // 看门狗检查频率（毫秒）
    private static final long WATCHDOG_INTERVAL = 3000;
    // 续期时长（秒）
    private static final int RENEWAL_SECONDS = 10;

    // 节点唯一ID（JVM级别）
    private static final String NODE_ID = UUID.nameUUIDFromBytes(
            (ManagementFactory.getRuntimeMXBean().getName() + System.getProperty("user.dir")).getBytes()
    ).toString();

    // Lua脚本定义
    private static final String LOCK_SCRIPT = 
            "if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then redis.call('expire', KEYS[1], ARGV[2]) return 1 else return 0 end";

    private static final String UNLOCK_SCRIPT = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    private static final String RENEWAL_SCRIPT = 
            "if string.sub(redis.call('get', KEYS[1]) or '', 1, #ARGV[1]) == ARGV[1] then return redis.call('expire', KEYS[1], ARGV[2]) else return 0 end";

    // 本地可重入锁计数（ThreadLocal，线程隔离）
    private final ThreadLocal<Map<String, ReentrantInfo>> reentrantLocks = ThreadLocal.withInitial(HashMap::new);
    // 全局持有锁信息（仅本节点持有的锁，用于看门狗续期）
    private final ConcurrentMap<String, LockInfo> localLocks = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final ScheduledFuture<?> watchdogFuture;

    // 等待获取锁的线程队列
    private final ConcurrentMap<String, Set<Thread>> waitingThreads = new ConcurrentHashMap<>();
    // 等待线程的唤醒时间记录
    private final ConcurrentMap<Thread, Long> threadWakeupTime = new ConcurrentHashMap<>();

    public DistributedLockService(RedisService redisService, 
                                CacheKeyGenerator keyGenerator,
                                ThreadUtils threadUtils) {
        this.redisService = redisService;
        this.keyGenerator = keyGenerator;
        /**
         * 线程工具类，用于管理看门狗任务
         * 通过scheduleWithFixedDelay方法被使用，用于启动和调度看门狗任务
         * 虽然不在destroy方法中直接使用，但它是watchdogFuture的创建者
         */
        // 使用ThreadUtils的scheduledThreadPool启动看门狗任务
        this.watchdogFuture = threadUtils.scheduleWithFixedDelay(this::renewalTask, 
            WATCHDOG_INTERVAL, WATCHDOG_INTERVAL, TimeUnit.MILLISECONDS);
    }

    /**
     * 可重入分布式锁加锁（Lua原子操作，value为nodeId:threadId:uuid）
     * @param lockName 锁名称
     * @param key      业务键
     * @return 锁唯一标识（value），null表示获取失败
     */
    public String tryLock(String lockName, String key) {
        return tryLock(lockName, key, DEFAULT_LOCK_TIMEOUT);
    }

    /**
     * 可重入分布式锁加锁（Lua原子操作，value为nodeId:threadId:uuid）
     * @param lockName    锁名称
     * @param key         业务键
     * @param lockTimeout 锁超时时间（秒）
     * @return 锁唯一标识（value），null表示获取失败
     */
    public String tryLock(String lockName, String key, int lockTimeout) {
        String lockKey = keyGenerator.generate(CacheKeyGenerator.Module.LOCK, lockName, key);
        String threadId = String.valueOf(Thread.currentThread().getId());
        String lockValue = NODE_ID + ":" + threadId + ":" + UUID.randomUUID();
        Map<String, ReentrantInfo> threadLocks = reentrantLocks.get();
        // 可重入：本线程已持有锁，重入计数+1
        if (threadLocks.containsKey(lockKey)) {
            ReentrantInfo info = threadLocks.get(lockKey);
            info.reentrantCount++;
            log.info("[ReentrantLock] 重入锁: {} count={}", lockKey, info.reentrantCount);
            return info.lockValue;
        }
        // 使用 RedisService 执行Lua脚本
        Long result = redisService.executeScript(LOCK_SCRIPT, lockKey, lockValue, String.valueOf(lockTimeout));
        boolean acquired = result != null && result == 1L;
        
        if (acquired) {
            // 记录本地锁信息，便于自动续期和解锁
            ReentrantInfo info = new ReentrantInfo(lockKey, lockValue, 1);
            threadLocks.put(lockKey, info);
            localLocks.put(lockKey, new LockInfo(lockName, key, lockValue));
            log.info("[Lock] 获取锁成功: {} value={}", lockKey, lockValue);
            return lockValue;
        } else {
            log.info("[Lock] 获取锁失败: {}", lockKey);
            return null;
        }
    }

    /**
     * 等待并获取可重入分布式锁（带超时等待，自动续期）
     * @param lockName    锁名称
     * @param key         业务键
     * @param lockTimeout 锁超时时间（秒）
     * @param waitTimeout 等待超时时间（秒）
     * @return 锁唯一标识（value），null表示获取失败
     */
    public String lock(String lockName, String key, int lockTimeout, int waitTimeout) {
        String lockKey = keyGenerator.generate(CacheKeyGenerator.Module.LOCK, lockName, key);
        long waitEndTime = System.currentTimeMillis() + (waitTimeout * 1000L);
        Thread currentThread = Thread.currentThread();

        // 将当前线程添加到等待队列
        waitingThreads.computeIfAbsent(lockKey, k -> ConcurrentHashMap.newKeySet()).add(currentThread);
        try {
            while (System.currentTimeMillis() < waitEndTime) {
                // 尝试获取锁
                String lockValue = tryLock(lockName, key, lockTimeout);
                if (lockValue != null) {
                    return lockValue;
                }

                // 计算剩余等待时间
                long remainingTime = waitEndTime - System.currentTimeMillis();
                if (remainingTime <= 0) {
                    break;
                }

                // 记录线程唤醒时间
                threadWakeupTime.put(currentThread, System.currentTimeMillis() + DEFAULT_RETRY_INTERVAL);
                
                // 使用LockSupport.parkNanos进行等待，避免忙等待
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(Math.min(remainingTime, DEFAULT_RETRY_INTERVAL)));
                
                // 检查是否被中断
                if (currentThread.isInterrupted()) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            return null;
        } finally {
            // 清理等待状态
            waitingThreads.computeIfPresent(lockKey, (k, threads) -> {
                threads.remove(currentThread);
                return threads.isEmpty() ? null : threads;
            });
            threadWakeupTime.remove(currentThread);
        }
    }

    /**
     * 解锁时尝试唤醒等待的线程
     */
    private void tryWakeupWaitingThreads(String lockKey) {
        Set<Thread> threads = waitingThreads.get(lockKey);
        if (threads != null) {
            long now = System.currentTimeMillis();
            for (Thread thread : threads) {
                Long wakeupTime = threadWakeupTime.get(thread);
                if (wakeupTime != null && wakeupTime <= now) {
                    LockSupport.unpark(thread);
                }
            }
        }
    }

    /**
     * 可重入分布式锁解锁（Lua原子操作，只有持有者才能释放）
     * @param lockName  锁名称
     * @param key       业务键
     * @param lockValue 锁唯一标识（获取锁时返回的value）
     * @return 是否释放成功
     */
    public boolean unlock(String lockName, String key, String lockValue) {
        String lockKey = keyGenerator.generate(CacheKeyGenerator.Module.LOCK, lockName, key);
        Map<String, ReentrantInfo> threadLocks = reentrantLocks.get();
        ReentrantInfo info = threadLocks.get(lockKey);
        if (info == null) {
            log.warn("[ReentrantLock] 当前线程未持有锁: {}", lockKey);
            return false;
        }
        // 可重入：重入计数>1时仅减计数，不释放Redis锁
        if (info.reentrantCount > 1) {
            info.reentrantCount--;
            log.info("[ReentrantLock] 解锁重入: {} 剩余count={}", lockKey, info.reentrantCount);
            return true;
        }
        // 使用 RedisService 执行Lua脚本
        Long result = redisService.executeScript(UNLOCK_SCRIPT, lockKey, lockValue);
        boolean released = result != null && result == 1L;
        
        if (released) {
            threadLocks.remove(lockKey);
            localLocks.remove(lockKey);
            // 尝试唤醒等待的线程
            tryWakeupWaitingThreads(lockKey);
            log.info("[Lock] 释放锁成功: {} value={}", lockKey, lockValue);
            return true;
        } else {
            log.info("[Lock] 释放锁失败: {} value={}", lockKey, lockValue);
            return false;
        }
    }

    /**
     * 分布式多节点看门狗自动续期任务
     * 仅为本节点持有的锁续期，宕机后其他节点不会续期，防止锁失效
     */
    private void renewalTask() {
        if (!running.get()) return;
        long now = System.currentTimeMillis();
        for (LockInfo info : localLocks.values()) {
            // 如果锁剩余时间小于一半，则续期
            if (now - info.lastRenewTime > (info.expireSeconds * 1000L) / 2) {
                renewLock(info);
            }
        }
    }

    /**
     * 续期单个锁（Lua原子操作，只有持有者节点/线程才能续期）
     * @param info 锁信息
     */
    private void renewLock(LockInfo info) {
        // 校验value前缀（nodeId:threadId）一致才续期
        String[] parts = info.value.split(":", 3);
        if (parts.length < 3) return;
        String expectedPrefix = parts[0] + ":" + parts[1];
        // 使用 RedisService 执行Lua脚本
        Long result = redisService.executeScript(RENEWAL_SCRIPT, info.key, expectedPrefix, String.valueOf(RENEWAL_SECONDS));
        Boolean renewed = result != null && result == 1L;
        
        if (renewed) {
            info.lastRenewTime = System.currentTimeMillis();
            info.expireSeconds = RENEWAL_SECONDS;
            log.info("[Lock] 自动续期成功: {} value={}", info.key, info.value);
        } else {
            // 续期失败可能表示锁被其他进程释放或过期，需要记录但不要太频繁
            log.warn("[Lock] 自动续期失败: {} value={}", info.key, info.value);
            // 从本地锁信息中移除，避免重复尝试
            localLocks.remove(info.key);
        }
    }

    /**
     * 便捷方法：加锁并执行操作，自动解锁
     * @param lockName 锁名称
     * @param key      业务键
     * @param action   需要执行的操作
     * @param <T>      返回值类型
     * @return 操作结果，获取锁失败返回null
     */
    public <T> T executeWithLock(String lockName, String key, LockAction<T> action) {
        String lockValue = tryLock(lockName, key, DEFAULT_LOCK_TIMEOUT);
        if (lockValue != null) {
            try {
                return action.execute();
            } catch (Exception e) {
                log.error("[DistributedLock] 执行操作异常: {}:{}", lockName, key, e);
                throw new RuntimeException("分布式锁内操作执行失败", e);
            } finally {
                unlock(lockName, key, lockValue);
            }
        }
        return null;
    }

    /**
     * 便捷方法：等待加锁并执行操作，自动解锁
     * @param lockName    锁名称
     * @param key         业务键
     * @param lockTimeout 锁超时时间（秒）
     * @param waitTimeout 等待超时时间（秒）
     * @param action      需要执行的操作
     * @param <T>         返回值类型
     * @return 操作结果，获取锁失败返回null
     */
    public <T> T executeWithLockAndWait(String lockName, String key, int lockTimeout, int waitTimeout, LockAction<T> action) {
        String lockValue = lock(lockName, key, lockTimeout, waitTimeout);
        if (lockValue != null) {
            try {
                return action.execute();
            } catch (Exception e) {
                log.error("[DistributedLock] 等待锁操作异常: {}:{}", lockName, key, e);
                throw new RuntimeException("分布式锁等待操作执行失败", e);
            } finally {
                unlock(lockName, key, lockValue);
            }
        }
        return null;
    }

    /**
     * 获取节点ID
     */
    public String getNodeId() {
        return NODE_ID;
    }

    /**
     * 关闭看门狗任务
     */
    @Override
    public void destroy() {
        running.set(false);
        if (watchdogFuture != null) {
            watchdogFuture.cancel(true);
        }
    }



    /**
     * 本地锁信息（仅本节点持有的锁）
     */
    public static class LockInfo {
        public final String lockName;
        public final String key;
        public final String value;
        public volatile int expireSeconds;
        public volatile long lastRenewTime;
        public LockInfo(String lockName, String key, String value) {
            this.lockName = lockName;
            this.key = key;
            this.value = value;
        }
    }

    /**
     * 可重入锁信息（线程隔离）
     */
    private static class ReentrantInfo {
        final String lockKey;
        final String lockValue;
        int reentrantCount;
        ReentrantInfo(String lockKey, String lockValue, int reentrantCount) {
            this.lockKey = lockKey;
            this.lockValue = lockValue;
            this.reentrantCount = reentrantCount;
        }
    }
} 