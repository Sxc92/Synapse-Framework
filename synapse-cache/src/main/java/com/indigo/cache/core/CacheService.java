package com.indigo.cache.core;

import com.indigo.cache.infrastructure.RedisService;
import com.indigo.cache.model.CacheObject;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 通用缓存服务类，封装RedisService，提供基于CacheObject的缓存操作
 *
 * @author 史偕成
 * @date 2025/05/16 09:25
 */
@Getter
@Service
@SuppressWarnings({"unchecked"})
public class CacheService {

    /**
     * -- GETTER --
     *  直接访问底层RedisService
     *
     */
    private final RedisService redisService;

    public CacheService(RedisService redisService) {
        this.redisService = redisService;
    }

    /**
     * 生成缓存键
     *
     * @param prefix 前缀
     * @param keys   键值部分
     * @return 完整的缓存键
     */
    public String generateKey(String prefix, Object... keys) {
        StringBuilder sb = new StringBuilder(prefix);
        for (Object key : keys) {
            sb.append(":").append(key);
        }
        return sb.toString();
    }

    /**
     * 保存缓存对象
     *
     * @param cacheObject 缓存对象
     * @param <T>         数据类型
     */
    public <T> void save(CacheObject<T> cacheObject) {
        if (cacheObject.getExpireSeconds() > 0) {
            redisService.set(cacheObject.getKey(), cacheObject, cacheObject.getExpireSeconds());
        } else {
            redisService.set(cacheObject.getKey(), cacheObject);
        }
    }

    /**
     * 获取缓存对象
     *
     * @param key 缓存键
     * @param <T> 数据类型
     * @return 缓存对象
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<CacheObject<T>> get(String key) {
        Object obj = redisService.get(key);
        if (obj instanceof CacheObject<?> cacheObject) {

            // 检查是否过期（理论上Redis会自动处理过期，这里是双重检查）
            if (cacheObject.isExpired()) {
                redisService.delete(key);
                return Optional.empty();
            }
            
            return Optional.of((CacheObject<T>) cacheObject);
        }
        return Optional.empty();
    }

    /**
     * 获取缓存数据，如果缓存不存在或已过期，返回空
     *
     * @param key 缓存键
     * @param <T> 数据类型
     * @return 缓存数据
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getData(String key) {
        Optional<CacheObject<?>> optionalCache = (Optional<CacheObject<?>>) (Optional<?>) get(key);
        return optionalCache.map(cacheObject -> (T) cacheObject.getData());
    }

    // ========== 基础Redis操作方法 ==========
    
    /**
     * 设置键值对
     *
     * @param key   键
     * @param value 值
     */
    public void set(String key, Object value) {
        redisService.set(key, value);
    }

    /**
     * 设置键值对并指定过期时间
     *
     * @param key           键
     * @param value         值
     * @param expireSeconds 过期时间（秒）
     */
    public void set(String key, Object value, long expireSeconds) {
        redisService.set(key, value, expireSeconds);
    }

    /**
     * 获取值（字符串格式）
     * 使用 StringRedisTemplate，返回 JSON 字符串
     *
     * @param key 键
     * @return 值（JSON 字符串）
     */
    public String getValue(String key) {
        return redisService.get(key);
    }

    /**
     * 设置键的过期时间
     *
     * @param key           键
     * @param expireSeconds 过期时间（秒）
     * @return 是否设置成功
     */
    public boolean expire(String key, long expireSeconds) {
        Boolean result = redisService.expire(key, expireSeconds);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 检查键是否存在
     *
     * @param key 键
     * @return 是否存在
     */
    public boolean hasKey(String key) {
        Boolean result = redisService.hasKey(key);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 获取键的剩余过期时间
     *
     * @param key 键
     * @return 剩余过期时间（秒），-1表示永不过期，-2表示键不存在
     */
    public long getExpire(String key) {
        Long result = redisService.getExpire(key);
        return result != null ? result : -2;
    }

    /**
     * 扫描匹配模式的键
     *
     * @param pattern 匹配模式
     * @return 匹配的键集合
     */
    public Set<String> scan(String pattern) {
        return redisService.scan(pattern);
    }

    /**
     * 获取缓存数据，如果缓存不存在或已过期，使用提供的函数加载数据并缓存
     *
     * @param key      缓存键
     * @param loader   数据加载函数
     * @param expireSeconds 过期时间（秒）
     * @param <T>      数据类型
     * @return 缓存数据
     */
    public <T> T getOrLoad(String key, Supplier<T> loader, long expireSeconds) {
        Optional<T> cachedData = getData(key);
        
        if (cachedData.isPresent()) {
            return cachedData.get();
        }
        
        // 加载数据
        T data = loader.get();
        
        // 缓存数据
        if (data != null) {
            save(new CacheObject<>(key, data, expireSeconds));
        }
        
        return data;
    }

    /**
     * 删除缓存
     *
     * @param key 缓存键
     * @return 是否成功
     */
    public boolean delete(String key) {
        return redisService.delete(key);
    }

    /**
     * 批量删除缓存
     *
     * @param keys 缓存键集合
     * @return 成功删除的数量
     */
    public long deleteMulti(List<String> keys) {
        Long count = redisService.delete(keys);
        return count == null ? 0 : count;
    }

    /**
     * 删除指定前缀的所有缓存
     *
     * @param prefix 前缀
     * @return 删除的数量
     */
    public long deleteByPrefix(String prefix) {
        String pattern = prefix + "*";
        Set<Object> keys = redisService.setMembers(pattern);
        if (keys != null && !keys.isEmpty()) {
            List<String> keyList = keys.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
            return deleteMulti(keyList);
        }
        return 0;
    }

    /**
     * 更新缓存数据，保留原有的过期时间
     *
     * @param key  缓存键
     * @param data 新数据
     * @param <T>  数据类型
     * @return 是否成功
     */
    @SuppressWarnings("unchecked")
    public <T> boolean update(String key, T data) {
        Optional<CacheObject<?>> optCacheObject = (Optional<CacheObject<?>>) (Optional<?>) get(key);
        if (optCacheObject.isPresent()) {
            CacheObject<?> cacheObject = optCacheObject.get();
            // 使用反射或其他机制更新数据 - 简化处理，创建新对象
            CacheObject<T> newCacheObject = new CacheObject<>(
                    key, 
                    data, 
                    cacheObject.getExpireSeconds()
            );
            save(newCacheObject);
            return true;
        }
        return false;
    }

    /**
     * 延长缓存过期时间
     *
     * @param key              缓存键
     * @param additionalSeconds 增加的秒数
     * @return 是否成功
     */
    public boolean extendExpiry(String key, long additionalSeconds) {
        Optional<CacheObject<?>> optCacheObject = (Optional<CacheObject<?>>) (Optional<?>) get(key);
        if (optCacheObject.isPresent()) {
            CacheObject<?> cacheObject = optCacheObject.get();
            long newExpireSeconds = cacheObject.getExpireSeconds() + additionalSeconds;
            if (newExpireSeconds > 0) {
                CacheObject<Object> newCacheObject = new CacheObject<>(
                        key, 
                        cacheObject.getData(), 
                        newExpireSeconds
                );
                save(newCacheObject);
                return true;
            }
        }
        return false;
    }
    
    // ========== 缓存管理方法 ==========
    
    /**
     * 清理过期的缓存条目
     * 
     * @return 清理的条目数量
     */
    public long clearExpiredEntries() {
        // 这里可以实现具体的过期条目清理逻辑
        // 由于Redis会自动清理过期键，这里主要处理本地缓存
        // log.debug("清理过期缓存条目"); // Original code had this line commented out
        return 0; // 暂时返回0，实际实现中可以扫描并清理过期条目
    }
    
    /**
     * 清理大对象缓存
     * 
     * @param sizeThreshold 大小阈值（字节）
     * @return 清理的对象数量
     */
    public long clearLargeObjects(long sizeThreshold) {
        // 这里可以实现清理大对象的逻辑
        // 可以通过序列化大小或其他方式判断对象大小
        // log.debug("清理大对象缓存，阈值: {} bytes", sizeThreshold); // Original code had this line commented out
        return 0; // 暂时返回0，实际实现中可以扫描并清理大对象
    }
    
    /**
     * 压缩缓存空间
     * 
     * @return 压缩后的大小（字节）
     */
    public long compact() {
        // 这里可以实现缓存空间压缩逻辑
        // 可以清理碎片、合并小对象等
        // log.debug("压缩缓存空间"); // Original code had this line commented out
        return 0; // 暂时返回0，实际实现中可以返回压缩后的大小
    }
    
    /**
     * 设置对象到缓存
     * 使用 StringRedisTemplate，自动将对象序列化为 JSON 字符串存储
     * 这样可以确保 key 和 value 都是可读的字符串格式
     * 
     * @param key 缓存键（字符串）
     * @param value 缓存值（对象会被序列化为 JSON）
     * @param expireSeconds 过期时间（秒）
     * @param <T> 数据类型
     */
    public <T> void setObject(String key, T value, long expireSeconds) {
        if (expireSeconds > 0) {
            redisService.set(key, value, expireSeconds);
        } else {
            redisService.set(key, value);
        }
    }
    
    /**
     * 从缓存获取对象
     * 使用 StringRedisTemplate，自动将 JSON 字符串反序列化为对象
     * 
     * @param key 缓存键
     * @param clazz 数据类型
     * @param <T> 数据类型
     * @return 缓存对象
     */
    public <T> T getObject(String key, Class<T> clazz) {
        return redisService.get(key, clazz);
    }
    
    /**
     * 检查缓存键是否存在
     * 
     * @param key 缓存键
     * @return 是否存在
     */
    public boolean exists(String key) {
        return redisService.hasKey(key);
    }
    
    /**
     * 获取缓存剩余过期时间
     * 
     * @param key 缓存键
     * @return 剩余过期时间（秒），-1表示永不过期，-2表示不存在
     */
    public long getTimeToLive(String key) {
        Long ttl = redisService.getExpire(key);
        return ttl != null ? ttl : -2;
    }
    
    /**
     * 重置缓存过期时间
     *
     * @param key           缓存键
     * @param expireSeconds 新的过期时间（秒）
     */
    public void resetExpiry(String key, long expireSeconds) {
        redisService.expire(key, expireSeconds);
    }

    /**
     * 保存哈希类型的缓存对象
     *
     * @param key        哈希键
     * @param field      字段
     * @param cacheObject 缓存对象
     * @param <T>        数据类型
     */
    public <T> void hashSave(String key, String field, CacheObject<T> cacheObject) {
        redisService.hashSet(key, field, cacheObject);
        
        // 如果有过期时间，设置整个哈希的过期时间
        if (cacheObject.getExpireSeconds() > 0) {
            redisService.expire(key, cacheObject.getExpireSeconds());
        }
    }

    /**
     * 获取哈希类型的缓存对象
     *
     * @param key   哈希键
     * @param field 字段
     * @param <T>   数据类型
     * @return 缓存对象
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<CacheObject<T>> hashGet(String key, String field) {
        Object obj = redisService.hashGet(key, field);
        if (obj instanceof CacheObject<?> cacheObject) {

            // 检查是否过期
            if (cacheObject.isExpired()) {
                redisService.hashDelete(key, field);
                return Optional.empty();
            }
            
            return Optional.of((CacheObject<T>) cacheObject);
        }
        return Optional.empty();
    }

    /**
     * 获取哈希类型的缓存数据
     *
     * @param key   哈希键
     * @param field 字段
     * @param <T>   数据类型
     * @return 缓存数据
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> hashGetData(String key, String field) {
        Optional<CacheObject<?>> optionalCache = (Optional<CacheObject<?>>) (Optional<?>) hashGet(key, field);
        return optionalCache.map(cacheObject -> (T) cacheObject.getData());
    }

    /**
     * 删除哈希类型的缓存
     *
     * @param key    哈希键
     * @param fields 字段列表
     * @return 删除的数量
     */
    public long hashDelete(String key, String... fields) {
        Long count = redisService.hashDelete(key, (Object[]) fields);
        return count == null ? 0 : count;
    }

}