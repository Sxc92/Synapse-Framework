# Synapse Cache 模块

## 概述

Synapse Cache 是一个功能强大的缓存模块，支持多种缓存策略和Redis连接模式。

## 特性

- ✅ **多模式Redis支持**: 单机、哨兵、集群模式
- ✅ **本地缓存**: 基于Caffeine的高性能本地缓存
- ✅ **分布式锁**: Redis分布式锁实现
- ✅ **缓存预热**: 支持应用启动时的缓存预热
- ✅ **穿透防护**: 布隆过滤器和限流防护
- ✅ **异常处理**: 完善的异常处理和降级策略
- ✅ **健康检查**: 缓存服务健康状态监控
- ✅ **连接池**: 可配置的连接池管理

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.indigo</groupId>
    <artifactId>synapse-cache</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 基础配置

```yaml
synapse:
  cache:
    enabled: true
    default-strategy: "LOCAL_AND_REDIS"
    
    redis:
      enabled: true
      key-prefix: "synapse"
      default-expire: "PT1H"
      
      connection:
        host: localhost
        port: 6379
        database: 0
        password: your-password
        timeout: "PT2S"
      
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

### 3. 使用缓存注解

```java
@Service
public class UserService {
    
    @Cacheable(value = "users", key = "#id")
    public User getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    @CacheEvict(value = "users", key = "#user.id")
    public void updateUser(User user) {
        userRepository.save(user);
    }
}
```

## 配置详解

### Redis连接模式

#### 单机模式
```yaml
synapse:
  cache:
    redis:
      connection:
        host: localhost
        port: 6379
        database: 0
        password: your-password
```

#### 哨兵模式
```yaml
synapse:
  cache:
    redis:
      sentinel:
        enabled: true
        nodes:
          - "sentinel1:26379"
          - "sentinel2:26379"
        master: "mymaster"
        password: sentinel-password
```

#### 集群模式
```yaml
synapse:
  cache:
    redis:
      cluster:
        enabled: true
        nodes:
          - "cluster-node1:6379"
          - "cluster-node2:6379"
        max-redirects: 5
        refresh-period: "PT30S"
```

### 连接池配置

```yaml
synapse:
  cache:
    redis:
      pool:
        max-active: 16      # 最大活跃连接数
        max-idle: 8         # 最大空闲连接数
        min-idle: 2         # 最小空闲连接数
        max-wait: "PT5S"    # 最大等待时间
        test-while-idle: true
        test-on-borrow: false
        test-on-return: false
```

### 本地缓存配置

```yaml
synapse:
  cache:
    local-cache:
      maximum-size: 10000
      expire-after-write: "PT30M"
      expire-after-access: "PT10M"
      refresh-after-write: "PT5M"
      enable-stats: true
```

### 分布式锁配置

```yaml
synapse:
  cache:
    lock:
      enabled: true
      key-prefix: "synapse:lock"
      default-timeout: 30
      retry-interval: 100
      max-retries: 3
      auto-release:
        enabled: true
        check-interval: 60000
        core-service-threshold: 1800000
        business-cache-threshold: 900000
        temporary-threshold: 300000
```

### 缓存预热配置

```yaml
synapse:
  cache:
    warmup:
      enabled: true
      thread-pool-size: 5
      timeout: "PT5M"
      batch-size: 100
      interval: "PT1H"
      data-sources:
        user: "userService"
        product: "productService"
```

### 穿透防护配置

```yaml
synapse:
  cache:
    penetration-protection:
      enabled: true
      null-value-expire: "PT5M"
      bloom-filter:
        enabled: false
        size: 1000000
        false-positive-rate: 0.01
      rate-limit:
        enabled: true
        requests-per-second: 100
        burst-requests: 200
```

## 环境配置

### 开发环境
```yaml
spring:
  config:
    activate:
      on-profile: dev
  logging:
    level:
      com.indigo.cache: DEBUG
      org.springframework.data.redis: DEBUG
```

### 生产环境
```yaml
spring:
  config:
    activate:
      on-profile: prod
synapse:
  cache:
    redis:
      sentinel:
        enabled: true
        nodes:
          - "prod-sentinel1:26379"
          - "prod-sentinel2:26379"
      pool:
        max-active: 16
        max-idle: 8
        min-idle: 2
```

### 集群环境
```yaml
spring:
  config:
    activate:
      on-profile: cluster
synapse:
  cache:
    redis:
      cluster:
        enabled: true
        nodes:
          - "cluster-node1:6379"
          - "cluster-node2:6379"
        max-redirects: 10
        refresh-period: "PT15S"
```

## 使用示例

### 1. 基础缓存操作

```java
@Service
public class ProductService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Cacheable(value = "products", key = "#id")
    public Product getProduct(Long id) {
        return productRepository.findById(id);
    }
    
    @CacheEvict(value = "products", key = "#product.id")
    public void updateProduct(Product product) {
        productRepository.save(product);
    }
    
    @CacheEvict(value = "products", allEntries = true)
    public void clearAllProducts() {
        // 清除所有产品缓存
    }
}
```

### 2. 分布式锁使用

```java
@Service
public class OrderService {
    
    @Autowired
    private DistributedLockManager lockManager;
    
    public void processOrder(String orderId) {
        String lockKey = "order:process:" + orderId;
        
        try (AutoReleaseLock lock = lockManager.tryLock(lockKey, Duration.ofSeconds(30))) {
            if (lock.isAcquired()) {
                // 处理订单逻辑
                processOrderLogic(orderId);
            } else {
                throw new RuntimeException("获取锁失败");
            }
        }
    }
}
```

### 3. 缓存预热

```java
@Component
public class CacheWarmupService {
    
    @Autowired
    private CacheWarmupManager warmupManager;
    
    @EventListener(ApplicationReadyEvent.class)
    public void warmupCache() {
        warmupManager.warmup("user", userService::getAllUsers);
        warmupManager.warmup("product", productService::getAllProducts);
    }
}
```

## 监控和健康检查

### 健康检查端点

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,caches
  endpoint:
    health:
      show-details: always
```

### 缓存统计

```java
@Component
public class CacheStatisticsService {
    
    @Autowired
    private StatisticsManager statisticsManager;
    
    public void printCacheStats() {
        Map<String, Object> stats = statisticsManager.getCacheStatistics();
        log.info("Cache statistics: {}", stats);
    }
}
```

## 故障排除

### 常见问题

1. **Redis连接失败**
   - 检查Redis服务是否运行
   - 验证连接配置（主机、端口、密码）
   - 检查网络连接和防火墙设置

2. **序列化错误**
   - 确保缓存对象实现Serializable接口
   - 检查RedisTemplate的序列化器配置
   - 验证Jackson配置

3. **连接池耗尽**
   - 增加连接池大小
   - 检查连接泄漏
   - 优化连接超时设置

### 日志配置

```yaml
logging:
  level:
    com.indigo.cache: DEBUG
    org.springframework.data.redis: DEBUG
    io.lettuce.core: DEBUG
```

## 性能优化建议

1. **连接池调优**
   - 根据并发量调整连接池大小
   - 设置合适的连接超时时间
   - 启用连接测试

2. **缓存策略优化**
   - 合理设置缓存过期时间
   - 使用本地缓存减少网络开销
   - 实现缓存预热机制

3. **序列化优化**
   - 使用高效的序列化器（如Jackson）
   - 避免序列化大对象
   - 考虑压缩选项

## 版本历史

- **1.0.0**: 初始版本，支持基础缓存功能
- **1.1.0**: 添加分布式锁和缓存预热
- **1.2.0**: 支持Redis集群和哨兵模式
- **1.3.0**: 增强异常处理和健康检查

## 贡献

欢迎提交Issue和Pull Request来改进这个模块！

## 许可证

本项目采用MIT许可证。 