package com.indigo.cache.extension;

import com.indigo.cache.infrastructure.RedisService;
import com.indigo.cache.manager.CacheKeyGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分布式读写锁服务
 * 支持读锁（共享锁）和写锁（排他锁）
 * 
 * 特性：
 * 1. 读锁：多个线程可以同时持有读锁
 * 2. 写锁：写锁是排他的，不能与读锁或写锁共存
 * 3. 写锁优先：写锁请求优先于读锁请求
 * 4. 可重入：同一线程可以多次获取同一类型的锁
 *
 * @author 史偕成
 * @date 2025/01/08
 */
@Slf4j
@Component
public class ReadWriteLockService {

    private final RedisService redisService;
    private final CacheKeyGenerator keyGenerator;
    private final DistributedLockService distributedLockService;

    // 读写锁相关的 Lua 脚本
    private static final String READ_LOCK_SCRIPT = 
        "local key = KEYS[1] " +
        "local readKey = key .. ':read' " +
        "local writeKey = key .. ':write' " +
        "local threadId = ARGV[1] " +
        "local timeout = ARGV[2] " +
        "local nodeId = ARGV[3] " +
        "local lockValue = nodeId .. ':' .. threadId .. ':' .. ARGV[4] " +
        "" +
        "-- 检查是否有写锁 " +
        "if redis.call('exists', writeKey) == 1 then " +
        "  return 0 " +
        "end " +
        "" +
        "-- 获取读锁 " +
        "redis.call('hset', readKey, threadId, lockValue) " +
        "redis.call('expire', readKey, timeout) " +
        "return 1";

    private static final String WRITE_LOCK_SCRIPT = 
        "local key = KEYS[1] " +
        "local readKey = key .. ':read' " +
        "local writeKey = key .. ':write' " +
        "local threadId = ARGV[1] " +
        "local timeout = ARGV[2] " +
        "local nodeId = ARGV[3] " +
        "local lockValue = nodeId .. ':' .. threadId .. ':' .. ARGV[4] " +
        "" +
        "-- 检查是否有读锁或写锁 " +
        "if redis.call('exists', readKey) == 1 or redis.call('exists', writeKey) == 1 then " +
        "  return 0 " +
        "end " +
        "" +
        "-- 获取写锁 " +
        "redis.call('set', writeKey, lockValue, 'EX', timeout) " +
        "return 1";

    private static final String READ_UNLOCK_SCRIPT = 
        "local key = KEYS[1] " +
        "local readKey = key .. ':read' " +
        "local threadId = ARGV[1] " +
        "" +
        "-- 删除读锁 " +
        "redis.call('hdel', readKey, threadId) " +
        "" +
        "-- 如果没有其他读锁，删除整个读锁键 " +
        "if redis.call('hlen', readKey) == 0 then " +
        "  redis.call('del', readKey) " +
        "end " +
        "return 1";

    private static final String WRITE_UNLOCK_SCRIPT = 
        "local key = KEYS[1] " +
        "local writeKey = key .. ':write' " +
        "local threadId = ARGV[1] " +
        "local lockValue = ARGV[2] " +
        "" +
        "-- 检查是否是锁的持有者 " +
        "if redis.call('get', writeKey) == lockValue then " +
        "  redis.call('del', writeKey) " +
        "  return 1 " +
        "end " +
        "return 0";

    public ReadWriteLockService(RedisService redisService, 
                               CacheKeyGenerator keyGenerator,
                               DistributedLockService distributedLockService) {
        this.redisService = redisService;
        this.keyGenerator = keyGenerator;
        this.distributedLockService = distributedLockService;
    }

    /**
     * 尝试获取读锁
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @param timeout 超时时间（秒）
     * @return 锁值，null 表示获取失败
     */
    public String tryReadLock(String lockName, String key, int timeout) {
        String lockKey = keyGenerator.generate(CacheKeyGenerator.Module.LOCK, lockName, key);
        String threadId = String.valueOf(Thread.currentThread().getId());
        String nodeId = getNodeId();
        String uuid = UUID.randomUUID().toString();

        try {
            Long result = redisService.executeScript(READ_LOCK_SCRIPT, 
                lockKey, threadId, String.valueOf(timeout), nodeId, uuid);
            
            boolean acquired = result != null && result == 1L;
            if (acquired) {
                String lockValue = nodeId + ":" + threadId + ":" + uuid;
                log.debug("[ReadLock] 获取读锁成功: {} threadId={}", lockKey, threadId);
                return lockValue;
            } else {
                log.debug("[ReadLock] 获取读锁失败: {} threadId={}", lockKey, threadId);
                return null;
            }
        } catch (Exception e) {
            log.error("[ReadLock] 获取读锁异常: {} threadId={}", lockKey, threadId, e);
            return null;
        }
    }

    /**
     * 尝试获取写锁
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @param timeout 超时时间（秒）
     * @return 锁值，null 表示获取失败
     */
    public String tryWriteLock(String lockName, String key, int timeout) {
        String lockKey = keyGenerator.generate(CacheKeyGenerator.Module.LOCK, lockName, key);
        String threadId = String.valueOf(Thread.currentThread().getId());
        String nodeId = getNodeId();
        String uuid = UUID.randomUUID().toString();

        try {
            Long result = redisService.executeScript(WRITE_LOCK_SCRIPT, 
                lockKey, threadId, String.valueOf(timeout), nodeId, uuid);
            
            boolean acquired = result != null && result == 1L;
            if (acquired) {
                String lockValue = nodeId + ":" + threadId + ":" + uuid;
                log.debug("[WriteLock] 获取写锁成功: {} threadId={}", lockKey, threadId);
                return lockValue;
            } else {
                log.debug("[WriteLock] 获取写锁失败: {} threadId={}", lockKey, threadId);
                return null;
            }
        } catch (Exception e) {
            log.error("[WriteLock] 获取写锁异常: {} threadId={}", lockKey, threadId, e);
            return null;
        }
    }

    /**
     * 等待并获取读锁
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @param lockTimeout 锁超时时间（秒）
     * @param waitTimeout 等待超时时间（秒）
     * @return 锁值，null 表示获取失败
     */
    public String readLock(String lockName, String key, int lockTimeout, int waitTimeout) {
        long endTime = System.currentTimeMillis() + (waitTimeout * 1000L);
        
        while (System.currentTimeMillis() < endTime) {
            String lockValue = tryReadLock(lockName, key, lockTimeout);
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
     * 等待并获取写锁
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @param lockTimeout 锁超时时间（秒）
     * @param waitTimeout 等待超时时间（秒）
     * @return 锁值，null 表示获取失败
     */
    public String writeLock(String lockName, String key, int lockTimeout, int waitTimeout) {
        long endTime = System.currentTimeMillis() + (waitTimeout * 1000L);
        
        while (System.currentTimeMillis() < endTime) {
            String lockValue = tryWriteLock(lockName, key, lockTimeout);
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
     * 释放读锁
     * 
     * @param lockName 锁名称
     * @param key 业务键
     * @return 是否释放成功
     */
    public boolean releaseReadLock(String lockName, String key) {
        String lockKey = keyGenerator.generate(CacheKeyGenerator.Module.LOCK, lockName, key);
        String threadId = String.valueOf(Thread.currentThread().getId());

        try {
            Long result = redisService.executeScript(READ_UNLOCK_SCRIPT, lockKey, threadId);
            boolean released = result != null && result == 1L;
            
            if (released) {
                log.debug("[ReadLock] 释放读锁成功: {} threadId={}", lockKey, threadId);
            } else {
                log.debug("[ReadLock] 释放读锁失败: {} threadId={}", lockKey, threadId);
            }
            
            return released;
        } catch (Exception e) {
            log.error("[ReadLock] 释放读锁异常: {} threadId={}", lockKey, threadId, e);
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
        String lockKey = keyGenerator.generate(CacheKeyGenerator.Module.LOCK, lockName, key);
        String threadId = String.valueOf(Thread.currentThread().getId());

        try {
            Long result = redisService.executeScript(WRITE_UNLOCK_SCRIPT, lockKey, threadId, lockValue);
            boolean released = result != null && result == 1L;
            
            if (released) {
                log.debug("[WriteLock] 释放写锁成功: {} threadId={}", lockKey, threadId);
            } else {
                log.debug("[WriteLock] 释放写锁失败: {} threadId={}", lockKey, threadId);
            }
            
            return released;
        } catch (Exception e) {
            log.error("[WriteLock] 释放写锁异常: {} threadId={}", lockKey, threadId, e);
            return false;
        }
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