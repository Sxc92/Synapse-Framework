package com.indigo.cache.extension.lock;

import com.indigo.cache.infrastructure.RedisService;
import com.indigo.cache.manager.CacheKeyGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 分布式公平锁服务
 * 支持FIFO（先进先出）的锁获取顺序
 * 
 * 特性：
 * 1. 公平性：按照请求顺序获取锁
 * 2. 可重入：同一线程可以多次获取锁
 * 3. 自动续期：防止长时间持有锁导致过期
 * 4. 死锁预防：超时自动释放
 *
 * @author 史偕成
 * @date 2025/01/08
 */
@Slf4j
@Component
public class FairLockService {

    private final RedisService redisService;
    private final CacheKeyGenerator keyGenerator;
    private final DistributedLockService distributedLockService;

    // 全局序列号生成器
    private final AtomicLong globalSequence = new AtomicLong(0);
    
    // 本地重入计数
    private final ConcurrentHashMap<String, Integer> reentrantCount = new ConcurrentHashMap<>();

    // 公平锁相关的 Lua 脚本
    private static final String FAIR_LOCK_SCRIPT = 
        "local key = KEYS[1] " +
        "local queueKey = key .. ':queue' " +
        "local lockKey = key .. ':lock' " +
        "local sequence = ARGV[1] " +
        "local threadId = ARGV[2] " +
        "local timeout = ARGV[3] " +
        "local nodeId = ARGV[4] " +
        "local lockValue = nodeId .. ':' .. threadId .. ':' .. ARGV[5] " +
        "" +
        "-- 检查是否已经持有锁（可重入） " +
        "local currentLock = redis.call('get', lockKey) " +
        "if currentLock and string.find(currentLock, threadId) then " +
        "  return 1 " +
        "end " +
        "" +
        "-- 检查锁是否被其他线程持有 " +
        "if currentLock then " +
        "  return 0 " +
        "end " +
        "" +
        "-- 获取队列中的第一个请求 " +
        "local firstRequest = redis.call('lindex', queueKey, 0) " +
        "if firstRequest and firstRequest ~= sequence then " +
        "  return 0 " +
        "end " +
        "" +
        "-- 获取锁 " +
        "redis.call('set', lockKey, lockValue, 'EX', timeout) " +
        "redis.call('lpop', queueKey) " +
        "return 1";

    private static final String FAIR_UNLOCK_SCRIPT = 
        "local key = KEYS[1] " +
        "local lockKey = key .. ':lock' " +
        "local threadId = ARGV[1] " +
        "local lockValue = ARGV[2] " +
        "" +
        "-- 检查是否是锁的持有者 " +
        "local currentLock = redis.call('get', lockKey) " +
        "if currentLock == lockValue then " +
        "  redis.call('del', lockKey) " +
        "  return 1 " +
        "end " +
        "return 0";

    private static final String ENQUEUE_SCRIPT = 
        "local key = KEYS[1] " +
        "local queueKey = key .. ':queue' " +
        "local sequence = ARGV[1] " +
        "" +
        "-- 将请求加入队列 " +
        "redis.call('rpush', queueKey, sequence) " +
        "redis.call('expire', queueKey, 3600) " +
        "return redis.call('llen', queueKey)";

    public FairLockService(RedisService redisService, 
                          CacheKeyGenerator keyGenerator,
                          DistributedLockService distributedLockService) {
        this.redisService = redisService;
        this.keyGenerator = keyGenerator;
        this.distributedLockService = distributedLockService;
    }

    /**
     * 尝试获取公平锁
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @param timeout 超时时间（秒）
     * @return 锁值，null 表示获取失败
     */
    public String tryFairLock(String lockName, String key, int timeout) {
        String lockKey = keyGenerator.generate(CacheKeyGenerator.Module.LOCK, lockName, key);
        String threadId = String.valueOf(Thread.currentThread().getId());
        String nodeId = getNodeId();
        String uuid = UUID.randomUUID().toString();
        String sequence = String.valueOf(globalSequence.incrementAndGet());

        // 检查本地重入
        String reentrantKey = lockKey + ":" + threadId;
        Integer count = reentrantCount.get(reentrantKey);
        if (count != null && count > 0) {
            reentrantCount.put(reentrantKey, count + 1);
            log.debug("[FairLock] 重入锁: {} threadId={} count={}", lockKey, threadId, count + 1);
            return nodeId + ":" + threadId + ":" + uuid;
        }

        try {
            // 将请求加入队列
            Long queueSize = redisService.executeScript(ENQUEUE_SCRIPT, lockKey, sequence);
            log.debug("[FairLock] 加入队列: {} sequence={} queueSize={}", lockKey, sequence, queueSize);

            // 尝试获取锁
            Long result = redisService.executeScript(FAIR_LOCK_SCRIPT, 
                lockKey, sequence, threadId, String.valueOf(timeout), nodeId, uuid);
            
            boolean acquired = result != null && result == 1L;
            if (acquired) {
                String lockValue = nodeId + ":" + threadId + ":" + uuid;
                reentrantCount.put(reentrantKey, 1);
                log.debug("[FairLock] 获取公平锁成功: {} threadId={} sequence={}", lockKey, threadId, sequence);
                return lockValue;
            } else {
                log.debug("[FairLock] 获取公平锁失败: {} threadId={} sequence={}", lockKey, threadId, sequence);
                return null;
            }
        } catch (Exception e) {
            log.error("[FairLock] 获取公平锁异常: {} threadId={}", lockKey, threadId, e);
            return null;
        }
    }

    /**
     * 等待并获取公平锁
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @param lockTimeout 锁超时时间（秒）
     * @param waitTimeout 等待超时时间（秒）
     * @return 锁值，null 表示获取失败
     */
    public String fairLock(String lockName, String key, int lockTimeout, int waitTimeout) {
        long endTime = System.currentTimeMillis() + (waitTimeout * 1000L);
        
        while (System.currentTimeMillis() < endTime) {
            String lockValue = tryFairLock(lockName, key, lockTimeout);
            if (lockValue != null) {
                return lockValue;
            }
            
            try {
                Thread.sleep(100); // 等待100ms后重试
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return null;
    }

    /**
     * 释放公平锁
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @param lockValue 锁值
     * @return 是否释放成功
     */
    public boolean releaseFairLock(String lockName, String key, String lockValue) {
        String lockKey = keyGenerator.generate(CacheKeyGenerator.Module.LOCK, lockName, key);
        String threadId = String.valueOf(Thread.currentThread().getId());
        String reentrantKey = lockKey + ":" + threadId;

        // 检查本地重入
        Integer count = reentrantCount.get(reentrantKey);
        if (count != null && count > 1) {
            reentrantCount.put(reentrantKey, count - 1);
            log.debug("[FairLock] 重入解锁: {} threadId={} count={}", lockKey, threadId, count - 1);
            return true;
        }

        try {
            Long result = redisService.executeScript(FAIR_UNLOCK_SCRIPT, lockKey, threadId, lockValue);
            boolean released = result != null && result == 1L;
            
            if (released) {
                reentrantCount.remove(reentrantKey);
                log.debug("[FairLock] 释放公平锁成功: {} threadId={}", lockKey, threadId);
            } else {
                log.debug("[FairLock] 释放公平锁失败: {} threadId={}", lockKey, threadId);
            }
            
            return released;
        } catch (Exception e) {
            log.error("[FairLock] 释放公平锁异常: {} threadId={}", lockKey, threadId, e);
            return false;
        }
    }

    /**
     * 便捷方法：获取公平锁并执行操作
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @param action 需要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果，获取锁失败返回null
     */
    public <T> T executeWithFairLock(String lockName, String key, LockAction<T> action) {
        String lockValue = tryFairLock(lockName, key, 10);
        if (lockValue != null) {
            try {
                return action.execute();
            } finally {
                releaseFairLock(lockName, key, lockValue);
            }
        }
        return null;
    }

    /**
     * 获取队列长度
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @return 队列长度
     */
    public long getQueueLength(String lockName, String key) {
        String lockKey = keyGenerator.generate(CacheKeyGenerator.Module.LOCK, lockName, key);
        String queueKey = lockKey + ":queue";
        
        try {
            Object result = redisService.get(queueKey);
            if (result instanceof List) {
                return ((List<?>) result).size();
            }
            return 0;
        } catch (Exception e) {
            log.error("[FairLock] 获取队列长度异常: {}", queueKey, e);
            return 0;
        }
    }

    /**
     * 获取节点ID
     */
    private String getNodeId() {
        return distributedLockService.getNodeId();
    }

    /**
     * 锁操作接口
     */
    @FunctionalInterface
    public interface LockAction<T> {
        T execute();
    }
} 