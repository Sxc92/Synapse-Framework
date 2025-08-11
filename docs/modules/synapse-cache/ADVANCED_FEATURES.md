# Synapse Cache 高级功能

## 📖 概述

本文档介绍 Synapse Cache 模块的高级功能，包括缓存预热、缓存穿透防护、缓存雪崩防护、缓存降级等企业级特性。这些功能帮助开发者构建高性能、高可用的缓存系统。

## 🚀 缓存预热

### 1. 功能说明
缓存预热是指在系统启动时或业务低峰期，主动将热点数据加载到缓存中，避免系统启动后缓存冷启动导致的性能问题。

### 2. 配置方式
```yaml
synapse:
  cache:
    warm-up:
      enabled: true
      # 预热策略
      strategy: GRADUAL  # GRADUAL, IMMEDIATE, SCHEDULED
      # 预热并发数
      concurrency: 5
      # 预热间隔（毫秒）
      interval: 100
      # 预热超时时间
      timeout: 30000
      # 预热数据源
      data-sources:
        - name: user-hot-data
          sql: "SELECT * FROM user WHERE status = 'ACTIVE' ORDER BY last_login_time DESC LIMIT 1000"
          cache-name: "user"
          key-generator: "userId"
```

### 3. 使用示例
```java
@Component
public class UserCacheWarmUp implements CacheWarmUp {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private CacheManager cacheManager;
    
    @Override
    public void warmUp() {
        log.info("开始预热用户缓存...");
        
        // 获取热点用户数据
        List<User> hotUsers = userMapper.selectHotUsers();
        
        // 批量预热缓存
        Cache cache = cacheManager.getCache("user");
        for (User user : hotUsers) {
            cache.put(user.getId(), user);
        }
        
        log.info("用户缓存预热完成，共预热 {} 条数据", hotUsers.size());
    }
    
    @Override
    public String getCacheName() {
        return "user";
    }
    
    @Override
    public int getPriority() {
        return 1; // 优先级，数字越小优先级越高
    }
}
```

### 4. 预热策略
- **GRADUAL**：渐进式预热，分批加载数据，避免对系统造成压力
- **IMMEDIATE**：立即预热，系统启动后立即加载所有预热数据
- **SCHEDULED**：定时预热，在指定时间点进行预热操作

## 🛡️ 缓存穿透防护

### 1. 功能说明
缓存穿透是指查询一个不存在的数据，由于缓存中没有，每次请求都会打到数据库，导致数据库压力过大。

### 2. 防护策略
```yaml
synapse:
  cache:
    penetration-protection:
      enabled: true
      # 空值缓存策略
      null-value-strategy: CACHE_NULL  # CACHE_NULL, BLOOM_FILTER, RATE_LIMIT
      # 空值缓存时间（秒）
      null-value-ttl: 300
      # 布隆过滤器配置
      bloom-filter:
        enabled: true
        expected-insertions: 1000000
        false-positive-rate: 0.01
      # 限流配置
      rate-limit:
        enabled: true
        max-requests: 100
        time-window: 60
```

### 3. 布隆过滤器实现
```java
@Component
public class BloomFilterCachePenetrationProtection implements CachePenetrationProtection {
    
    private BloomFilter<String> bloomFilter;
    
    @PostConstruct
    public void init() {
        // 初始化布隆过滤器
        bloomFilter = BloomFilter.create(
            Funnels.stringFunnel(Charset.defaultCharset()),
            1000000,  // 预期插入数量
            0.01      // 误判率
        );
        
        // 预加载已知数据
        loadKnownData();
    }
    
    @Override
    public boolean mightContain(String key) {
        return bloomFilter.mightContain(key);
    }
    
    @Override
    public void put(String key) {
        bloomFilter.put(key);
    }
    
    private void loadKnownData() {
        // 从数据库加载已知的键值
        List<String> knownKeys = loadKnownKeysFromDatabase();
        for (String key : knownKeys) {
            bloomFilter.put(key);
        }
    }
}
```

### 4. 空值缓存策略
```java
@Service
public class UserServiceWithPenetrationProtection {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private CacheManager cacheManager;
    
    public User getUserById(Long id) {
        String cacheKey = "user:" + id;
        Cache cache = cacheManager.getCache("user");
        
        // 先从缓存获取
        Cache.ValueWrapper wrapper = cache.get(cacheKey);
        if (wrapper != null) {
            Object value = wrapper.get();
            if (value instanceof NullValue) {
                // 空值缓存，直接返回null
                return null;
            }
            return (User) value;
        }
        
        // 检查布隆过滤器
        if (!bloomFilter.mightContain(cacheKey)) {
            // 数据肯定不存在，直接返回null
            return null;
        }
        
        // 从数据库查询
        User user = userMapper.selectById(id);
        
        if (user != null) {
            // 缓存用户数据
            cache.put(cacheKey, user);
        } else {
            // 缓存空值，防止缓存穿透
            cache.put(cacheKey, NullValue.INSTANCE);
        }
        
        return user;
    }
}
```

## 🌨️ 缓存雪崩防护

### 1. 功能说明
缓存雪崩是指缓存中大量数据同时过期，导致大量请求直接打到数据库，造成数据库压力过大。

### 2. 防护策略
```yaml
synapse:
  cache:
    avalanche-protection:
      enabled: true
      # 过期时间随机化
      random-expiration:
        enabled: true
        # 随机范围（秒）
        random-range: 300
      # 缓存重建策略
      rebuild-strategy: ASYNC  # ASYNC, SYNC, SEMAPHORE
      # 重建并发控制
      rebuild-concurrency: 10
      # 熔断器配置
      circuit-breaker:
        enabled: true
        failure-threshold: 5
        recovery-timeout: 60000
```

### 3. 过期时间随机化
```java
@Component
public class RandomExpirationCacheManager extends AbstractCacheManager {
    
    @Override
    protected Cache createCache(String name) {
        return new RandomExpirationCache(name);
    }
    
    private static class RandomExpirationCache implements Cache {
        
        private final String name;
        private final Random random = new Random();
        
        public RandomExpirationCache(String name) {
            this.name = name;
        }
        
        @Override
        public void put(Object key, Object value) {
            // 计算随机过期时间
            long baseTtl = getBaseTtl();
            long randomTtl = baseTtl + random.nextInt(300); // 随机增加0-300秒
            
            // 使用随机过期时间存储
            putWithExpiration(key, value, randomTtl);
        }
        
        private long getBaseTtl() {
            // 从配置获取基础过期时间
            return 3600; // 1小时
        }
    }
}
```

### 4. 异步缓存重建
```java
@Component
public class AsyncCacheRebuilder {
    
    @Autowired
    private CacheManager cacheManager;
    
    @Async
    public void rebuildCache(String cacheName, String key) {
        try {
            log.info("开始异步重建缓存: {} - {}", cacheName, key);
            
            // 获取缓存数据
            Object data = loadDataFromDatabase(key);
            
            // 重建缓存
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null && data != null) {
                cache.put(key, data);
                log.info("缓存重建成功: {} - {}", cacheName, key);
            }
            
        } catch (Exception e) {
            log.error("缓存重建失败: {} - {}", cacheName, key, e);
            // 记录失败次数，触发熔断器
            recordFailure(cacheName, key);
        }
    }
    
    private void recordFailure(String cacheName, String key) {
        // 记录失败次数，达到阈值时触发熔断器
        String failureKey = cacheName + ":" + key + ":failures";
        // 实现失败计数逻辑
    }
}
```

## 🔄 缓存降级

### 1. 功能说明
缓存降级是指当缓存系统出现问题时，系统能够自动降级到备用方案，保证服务的可用性。

### 2. 降级策略
```yaml
synapse:
  cache:
    fallback:
      enabled: true
      # 降级策略
      strategy: GRADUAL  # GRADUAL, IMMEDIATE, SELECTIVE
      # 降级阈值
      threshold:
        error-rate: 0.1
        response-time: 1000
      # 备用缓存
      backup:
        enabled: true
        type: LOCAL  # LOCAL, REDIS_CLUSTER, DATABASE
        # 本地缓存配置
        local:
          maximum-size: 1000
          expire-after-write: 30m
```

### 3. 降级实现
```java
@Component
public class CacheFallbackManager {
    
    @Autowired
    private CacheManager primaryCacheManager;
    
    @Autowired
    private CacheManager backupCacheManager;
    
    private final AtomicBoolean isDegraded = new AtomicBoolean(false);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final AtomicLong totalCount = new AtomicLong(0);
    
    public Object get(String cacheName, String key) {
        try {
            // 优先使用主缓存
            if (!isDegraded.get()) {
                Object value = getFromPrimaryCache(cacheName, key);
                if (value != null) {
                    recordSuccess();
                    return value;
                }
            }
            
            // 降级到备用缓存
            Object value = getFromBackupCache(cacheName, key);
            if (value != null) {
                return value;
            }
            
            // 从数据库加载
            return loadFromDatabase(cacheName, key);
            
        } catch (Exception e) {
            recordError();
            // 触发降级检查
            checkDegradation();
            // 从备用缓存获取
            return getFromBackupCache(cacheName, key);
        }
    }
    
    private void checkDegradation() {
        long errors = errorCount.get();
        long total = totalCount.get();
        
        if (total > 100) { // 至少100次请求后才开始计算
            double errorRate = (double) errors / total;
            
            if (errorRate > 0.1) { // 错误率超过10%
                isDegraded.set(true);
                log.warn("缓存系统降级，错误率: {}", errorRate);
            }
        }
    }
    
    private void recordSuccess() {
        totalCount.incrementAndGet();
    }
    
    private void recordError() {
        errorCount.incrementAndGet();
        totalCount.incrementAndGet();
    }
}
```

## 📊 缓存监控

### 1. 监控指标
```java
@Component
public class CacheMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    
    public CacheMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    public void recordCacheHit(String cacheName) {
        Counter.builder("cache.hits")
            .tag("cache", cacheName)
            .register(meterRegistry)
            .increment();
    }
    
    public void recordCacheMiss(String cacheName) {
        Counter.builder("cache.misses")
            .tag("cache", cacheName)
            .register(meterRegistry)
            .increment();
    }
    
    public void recordCacheOperation(String cacheName, String operation, long duration) {
        Timer.builder("cache.operations")
            .tag("cache", cacheName)
            .tag("operation", operation)
            .register(meterRegistry)
            .record(duration, TimeUnit.MILLISECONDS);
    }
    
    public void recordCacheSize(String cacheName, long size) {
        Gauge.builder("cache.size")
            .tag("cache", cacheName)
            .register(meterRegistry, size);
    }
}
```

### 2. 健康检查
```java
@Component
public class CacheHealthIndicator implements HealthIndicator {
    
    @Autowired
    private CacheManager cacheManager;
    
    @Override
    public Health health() {
        try {
            // 检查缓存系统状态
            boolean isHealthy = checkCacheHealth();
            
            if (isHealthy) {
                return Health.up()
                    .withDetail("status", "UP")
                    .withDetail("message", "缓存系统运行正常")
                    .build();
            } else {
                return Health.down()
                    .withDetail("status", "DOWN")
                    .withDetail("message", "缓存系统异常")
                    .build();
            }
            
        } catch (Exception e) {
            return Health.down()
                .withDetail("status", "DOWN")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
    
    private boolean checkCacheHealth() {
        // 实现缓存健康检查逻辑
        // 检查连接、性能等指标
        return true;
    }
}
```

## 🔧 自定义扩展

### 1. 自定义缓存策略
```java
@Component
public class CustomCacheStrategy implements CacheStrategy {
    
    @Override
    public String getStrategyName() {
        return "CUSTOM_STRATEGY";
    }
    
    @Override
    public boolean shouldCache(String key, Object value) {
        // 自定义缓存判断逻辑
        return value != null && !(value instanceof String) || 
               ((String) value).length() > 10;
    }
    
    @Override
    public long getExpirationTime(String key, Object value) {
        // 自定义过期时间逻辑
        if (value instanceof User) {
            User user = (User) value;
            if ("VIP".equals(user.getUserType())) {
                return 7200; // VIP用户缓存2小时
            }
        }
        return 3600; // 默认1小时
    }
}
```

### 2. 自定义序列化器
```java
@Component
public class CustomCacheSerializer implements CacheSerializer {
    
    @Override
    public byte[] serialize(Object object) throws SerializationException {
        if (object == null) {
            return null;
        }
        
        try {
            // 使用自定义序列化逻辑
            return JsonUtils.toJsonBytes(object);
        } catch (Exception e) {
            throw new SerializationException("序列化失败", e);
        }
    }
    
    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        
        try {
            // 使用自定义反序列化逻辑
            return JsonUtils.fromJsonBytes(bytes, Object.class);
        } catch (Exception e) {
            throw new SerializationException("反序列化失败", e);
        }
    }
}
```

## 📝 最佳实践

### 1. 缓存预热
- 在系统启动时预热热点数据
- 使用渐进式预热避免系统压力
- 监控预热效果和性能影响

### 2. 穿透防护
- 合理使用空值缓存
- 实现布隆过滤器减少误判
- 设置合理的限流策略

### 3. 雪崩防护
- 使用随机过期时间
- 实现异步缓存重建
- 配置熔断器保护系统

### 4. 降级策略
- 实现多级降级方案
- 监控系统状态自动降级
- 提供手动降级开关

### 5. 监控告警
- 设置合理的监控指标
- 配置告警阈值和通知
- 定期分析缓存性能

## 🐛 常见问题

### 1. 预热数据过多
**问题**：预热数据量过大导致系统启动缓慢
**解决方案**：
- 限制预热数据量
- 使用渐进式预热
- 优化预热策略

### 2. 布隆过滤器误判
**问题**：布隆过滤器误判导致数据丢失
**解决方案**：
- 调整布隆过滤器参数
- 实现布隆过滤器重建
- 使用多重验证机制

### 3. 降级策略失效
**问题**：降级策略无法正常工作
**解决方案**：
- 检查降级配置
- 验证备用系统状态
- 测试降级流程

## 📚 相关文档

- [Synapse Cache 基础使用](README.md)
- [缓存注解使用指南](CACHE_ANNOTATIONS_USAGE.md)
- [分布式锁优化](DISTRIBUTED_LOCK_OPTIMIZATION.md)
- [优化总结](OPTIMIZATION_SUMMARY.md)

---

*最后更新时间：2025年08月11日 12:41:56* 