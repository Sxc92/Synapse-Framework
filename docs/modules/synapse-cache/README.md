# Synapse Cache 模块

## 概述

Synapse Cache 模块是 Synapse Framework 的缓存管理模块，提供了统一的缓存抽象、多种缓存实现以及智能的缓存策略管理。支持本地缓存、分布式缓存和混合缓存模式。

## 主要特性

- 🚀 **多种缓存实现**：Redis、Caffeine、EhCache、Hazelcast
- 🔄 **缓存策略**：TTL、LRU、LFU、FIFO 等
- 🎯 **注解驱动**：基于注解的缓存操作
- 🔒 **分布式锁**：基于缓存的分布式锁实现
- 📊 **缓存监控**：缓存命中率、性能统计
- 🧠 **智能缓存**：自动缓存预热、失效策略
- 🔧 **自动配置**：Spring Boot 自动配置支持

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.indigo</groupId>
    <artifactId>synapse-cache</artifactId>
    <version>${synapse.version}</version>
</dependency>
```

### 2. 基础配置

```yaml
synapse:
  cache:
    # 默认缓存类型
    default-type: REDIS
    # 缓存前缀
    key-prefix: "synapse:"
    # 默认过期时间（秒）
    default-ttl: 3600
    
    # Redis 配置
    redis:
      host: localhost
      port: 6379
      password: 
      database: 0
      timeout: 3000
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms
    
    # Caffeine 配置
    caffeine:
      maximum-size: 1000
      expire-after-write: 1h
      expire-after-access: 30m
```

### 3. 使用示例

```java
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    // 缓存查询结果
    @Cacheable(value = "user", key = "#id")
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }
    
    // 更新缓存
    @CachePut(value = "user", key = "#user.id")
    public User createUser(User user) {
        userMapper.insert(user);
        return user;
    }
    
    // 删除缓存
    @CacheEvict(value = "user", key = "#id")
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }
    
    // 清空所有缓存
    @CacheEvict(value = "user", allEntries = true)
    public void clearAllCache() {
        // 清空缓存的逻辑
    }
}
```

## 配置说明

### 1. 缓存类型配置

**Redis 缓存**
```yaml
synapse:
  cache:
    redis:
      host: localhost
      port: 6379
      password: your-password
      database: 0
      timeout: 3000
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms
      # 序列化配置
      serializer: JACKSON
      # 压缩配置
      compression: true
```

**Caffeine 本地缓存**
```yaml
synapse:
  cache:
    caffeine:
      # 最大缓存条目数
      maximum-size: 1000
      # 写入后过期时间
      expire-after-write: 1h
      # 访问后过期时间
      expire-after-access: 30m
      # 最大权重
      maximum-weight: 10000
      # 统计信息
      record-stats: true
```

**EhCache 配置**
```yaml
synapse:
  cache:
    ehcache:
      # 配置文件路径
      config-location: classpath:ehcache.xml
      # 最大堆内存
      max-heap-size: 100MB
      # 最大堆外内存
      max-off-heap-size: 200MB
```

### 2. 缓存策略配置

**TTL 策略**
```yaml
synapse:
  cache:
    # 默认过期时间
    default-ttl: 3600
    # 最大过期时间
    max-ttl: 86400
    # 最小过期时间
    min-ttl: 60
```

**LRU 策略**
```yaml
synapse:
  cache:
    # 最大缓存条目数
    maximum-size: 1000
    # 淘汰策略
    eviction-policy: LRU
```

### 3. 分布式锁配置

```yaml
synapse:
  cache:
    # 分布式锁配置
    distributed-lock:
      # 锁超时时间
      timeout: 30000
      # 重试次数
      retry-times: 3
      # 重试间隔
      retry-interval: 1000
      # 锁前缀
      key-prefix: "lock:"
```

## 高级功能

### 1. 缓存注解

**基础缓存注解**
```java
// 缓存查询结果
@Cacheable(value = "user", key = "#id", unless = "#result == null")

// 更新缓存
@CachePut(value = "user", key = "#user.id")

// 删除缓存
@CacheEvict(value = "user", key = "#id")

// 条件缓存
@Cacheable(value = "user", condition = "#id > 0", unless = "#result == null")
```

**自定义缓存注解**
```java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Cacheable(value = "user", key = "#id", unless = "#result == null")
public @interface UserCache {
    String value() default "user";
    String key() default "#id";
}
```

### 2. 分布式锁

**注解方式使用**
```java
@Service
public class OrderService {
    
    @DistributedLock(key = "order:#{#orderId}", timeout = 30000)
    public void processOrder(Long orderId) {
        // 处理订单逻辑
        // 分布式锁会自动管理
    }
}
```

**编程方式使用**
```java
@Service
public class OrderService {
    
    @Autowired
    private DistributedLockManager lockManager;
    
    public void processOrder(Long orderId) {
        String lockKey = "order:" + orderId;
        
        try {
            // 获取锁
            if (lockManager.tryLock(lockKey, 30000)) {
                try {
                    // 处理订单逻辑
                    processOrderLogic(orderId);
                } finally {
                    // 释放锁
                    lockManager.unlock(lockKey);
                }
            } else {
                throw new RuntimeException("获取锁失败");
            }
        } catch (Exception e) {
            log.error("处理订单失败", e);
            throw e;
        }
    }
}
```

### 3. 缓存管理

**缓存统计信息**
```java
@Service
public class CacheStatisticsService {
    
    @Autowired
    private CacheManager cacheManager;
    
    public CacheStatistics getStatistics(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache instanceof CacheStatistics) {
            return (CacheStatistics) cache;
        }
        return null;
    }
    
    public Map<String, CacheStatistics> getAllStatistics() {
        Map<String, CacheStatistics> statistics = new HashMap<>();
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CacheStatistics) {
                statistics.put(cacheName, (CacheStatistics) cache);
            }
        });
        
        return statistics;
    }
}
```

**缓存预热**
```java
@Component
public class CacheWarmupService {
    
    @Autowired
    private UserService userService;
    
    @EventListener(ApplicationReadyEvent.class)
    public void warmupCache() {
        log.info("开始缓存预热...");
        
        // 预热用户缓存
        List<Long> userIds = Arrays.asList(1L, 2L, 3L, 4L, 5L);
        userIds.forEach(id -> {
            try {
                userService.getUserById(id);
            } catch (Exception e) {
                log.warn("预热用户缓存失败: {}", id, e);
            }
        });
        
        log.info("缓存预热完成");
    }
}
```

## 最佳实践

### 1. 缓存键设计

- 使用有意义的键名：`user:profile:123`
- 避免键名过长：使用缩写和编码
- 保持键名一致性：遵循命名规范

### 2. 缓存策略选择

- **热点数据**：使用本地缓存（Caffeine）
- **共享数据**：使用分布式缓存（Redis）
- **大对象**：考虑压缩和序列化策略

### 3. 缓存失效策略

- **时间失效**：设置合理的 TTL
- **事件失效**：数据更新时主动失效
- **容量失效**：达到容量上限时淘汰

### 4. 性能优化

- 使用批量操作减少网络开销
- 合理设置连接池参数
- 监控缓存命中率和性能指标

## 故障排除

### 常见问题

1. **缓存穿透**
   - 使用布隆过滤器
   - 缓存空值
   - 接口限流

2. **缓存雪崩**
   - 设置随机过期时间
   - 使用熔断器
   - 多级缓存

3. **缓存击穿**
   - 使用分布式锁
   - 热点数据永不过期
   - 异步更新缓存

### 日志配置

```yaml
logging:
  level:
    com.indigo.cache: DEBUG
    org.springframework.cache: DEBUG
```

## 版本历史

| 版本 | 更新内容 |
|------|----------|
| 1.0.0 | 初始版本，基础缓存功能 |
| 1.1.0 | 添加分布式锁功能 |
| 1.2.0 | 集成多种缓存实现 |
| 1.3.0 | 优化缓存策略和性能 |
| 1.4.0 | 添加缓存监控和统计 |

## 贡献

欢迎提交 Issue 和 Pull Request 来改进这个模块。

## 许可证

本项目采用 MIT 许可证。 