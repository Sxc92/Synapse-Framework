# Synapse Cache 模块

## 概述

Synapse Cache 是一个功能强大的缓存模块，支持多种缓存策略、Redis连接模式和高级分布式锁功能。该模块提供了统一的缓存抽象、智能的缓存策略管理以及企业级的分布式锁解决方案。

## 主要特性

- ✅ **多模式Redis支持**: 单机、哨兵、集群模式
- ✅ **本地缓存**: 基于Caffeine的高性能本地缓存
- ✅ **统一分布式锁**: 可重入锁、读写锁、公平锁的统一管理
- ✅ **分布式死锁检测**: 基于Redis的全局死锁检测和协调处理
- ✅ **缓存预热**: 支持应用启动时的缓存预热
- ✅ **穿透防护**: 布隆过滤器和限流防护
- ✅ **异常处理**: 完善的异常处理和降级策略
- ✅ **健康检查**: 缓存服务健康状态监控
- ✅ **连接池**: 可配置的连接池管理
- ✅ **性能监控**: 锁性能监控和统计

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
      # 死锁检测配置
      deadlock:
        # 分布式死锁检测配置
        distributed:
          # 是否启用分布式死锁检测
          enabled: true
          # 状态同步间隔(毫秒)
          sync-interval: 5000  # 5秒
          # 全局检测间隔(毫秒)
          global-detection-interval: 10000  # 10秒
          # 节点超时时间(毫秒)
          node-timeout: 30000  # 30秒
          # 最大节点数量
          max-nodes: 10
          # Redis键前缀
          redis-prefix: "deadlock:global"
          # 是否启用调试日志
          debug: false
          # 心跳间隔(毫秒)
          heartbeat-interval: 1000  # 1秒
          # 清理间隔(毫秒)
          cleanup-interval: 30000  # 30秒
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

## 分布式锁使用

### LockManager 统一入口

`LockManager` 是分布式锁的统一入口，支持多种锁类型：

```java
@Service
public class OrderService {
    
    @Autowired
    private LockManager lockManager;
    
    public void processOrder(String orderId) {
        String lockKey = "order:process:" + orderId;
        
        // 1. 简单加锁（可重入锁）
        String lockValue = lockManager.tryLock("order", orderId, 30);
        if (lockValue != null) {
            try {
                // 处理订单逻辑
                processOrderLogic(orderId);
            } finally {
                lockManager.unlock("order", orderId, lockValue, LockManager.LockType.REENTRANT);
            }
        }
        
        // 2. 读写锁
        String readLockValue = lockManager.tryReadLock("data", orderId, 10);
        if (readLockValue != null) {
            try {
                // 读取数据
                return readData(orderId);
            } finally {
                lockManager.releaseReadLock("data", orderId);
            }
        }
        
        // 3. 便捷执行方法
        lockManager.executeWithLock("order", orderId, () -> {
            // 业务逻辑
            return processOrderLogic(orderId);
        });
    }
}
```

### 锁类型说明

#### 1. 可重入锁 (REENTRANT)
- 支持同一线程多次获取同一把锁
- 适用于需要递归调用的场景

```java
// 获取可重入锁
String lockValue = lockManager.tryLock("resource", "key", 30);

// 等待获取锁
String lockValue = lockManager.lock("resource", "key", 30, 60, LockManager.LockType.REENTRANT);
```

#### 2. 读写锁 (READ_WRITE)
- 支持多个读锁或一个写锁
- 读锁之间不互斥，写锁与读锁/写锁互斥

```java
// 获取读锁
String readLockValue = lockManager.tryReadLock("data", "key", 10);

// 获取写锁
String writeLockValue = lockManager.tryWriteLock("data", "key", 10);
```

#### 3. 公平锁 (FAIR)
- 按照请求顺序获取锁
- 避免锁饥饿问题

```java
// 获取公平锁
String lockValue = lockManager.tryLock("resource", "key", 30, LockManager.LockType.FAIR);
```

### 分布式死锁检测

#### 功能特性
- **混合检测策略**: 本地检测 + 全局协调检测
- **状态同步**: 定期同步本地状态到Redis全局图
- **分布式算法**: 支持跨节点的死锁检测
- **智能处理**: 本地死锁立即处理，全局死锁协调处理
- **容错机制**: 单点故障不影响整体检测

#### 使用示例

```java
@Service
public class DeadlockDetectionService {
    
    @Autowired
    private LockManager lockManager;
    
    public void demonstrateDeadlockDetection() {
        // 获取分布式死锁检测状态
        Map<String, Object> status = lockManager.getDistributedDeadlockStatus();
        log.info("分布式死锁检测状态: {}", status);
        
        // 手动触发全局死锁检测
        List<Set<String>> cycles = lockManager.detectGlobalDeadlocks();
        if (!cycles.isEmpty()) {
            log.warn("检测到死锁环: {}", cycles);
        }
        
        // 同步本地状态到全局
        lockManager.syncLocalStateToGlobal();
        
        // 启用/禁用全局检测
        lockManager.setGlobalDetectionEnabled(true);
    }
}
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

### 2. 高级分布式锁使用

```java
@Service
public class InventoryService {
    
    @Autowired
    private LockManager lockManager;
    
    public void updateInventory(String productId, int quantity) {
        // 使用读写锁优化性能
        String lockKey = "inventory:" + productId;
        
        // 先获取读锁检查库存
        String readLockValue = lockManager.tryReadLock("inventory", productId, 5);
        if (readLockValue != null) {
            try {
                int currentStock = getCurrentStock(productId);
                if (currentStock >= quantity) {
                    // 释放读锁，获取写锁
                    lockManager.releaseReadLock("inventory", productId);
                    
                    String writeLockValue = lockManager.tryWriteLock("inventory", productId, 10);
                    if (writeLockValue != null) {
                        try {
                            // 更新库存
                            updateStock(productId, currentStock - quantity);
                        } finally {
                            lockManager.unlock("inventory", productId, writeLockValue, LockManager.LockType.READ_WRITE);
                        }
                    }
                }
            } finally {
                if (readLockValue != null) {
                    lockManager.releaseReadLock("inventory", productId);
                }
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

### 4. 性能监控

```java
@Component
public class LockPerformanceService {
    
    @Autowired
    private LockManager lockManager;
    
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void logPerformanceStats() {
        // 获取锁性能统计
        Map<String, Object> stats = lockManager.getPerformanceStatistics();
        log.info("锁性能统计: {}", stats);
        
        // 获取分布式死锁检测状态
        if (lockManager.isDistributedDeadlockEnabled()) {
            Map<String, Object> deadlockStatus = lockManager.getDistributedDeadlockStatus();
            log.info("分布式死锁检测状态: {}", deadlockStatus);
        }
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

## 最佳实践

### 1. 锁使用最佳实践

- **选择合适的锁类型**: 根据业务场景选择可重入锁、读写锁或公平锁
- **设置合理的超时时间**: 避免长时间等待和死锁
- **及时释放锁**: 使用try-finally确保锁的释放
- **避免嵌套锁**: 减少死锁风险

### 2. 缓存键设计

- 使用有意义的键名：`user:profile:123`
- 避免键名过长：使用缩写和编码
- 保持键名一致性：遵循命名规范

### 3. 缓存策略选择

- **热点数据**：使用本地缓存（Caffeine）
- **共享数据**：使用分布式缓存（Redis）
- **大对象**：考虑压缩和序列化策略

### 4. 分布式死锁检测

- **启用分布式检测**: 在集群环境中启用分布式死锁检测
- **合理配置参数**: 根据网络延迟调整同步间隔
- **监控检测状态**: 定期检查死锁检测状态和性能

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

4. **死锁检测问题**
   - 检查Redis连接是否正常
   - 验证分布式死锁检测配置
   - 查看死锁检测日志

### 日志配置

```yaml
logging:
  level:
    com.indigo.cache: DEBUG
    com.indigo.cache.extension.lock: DEBUG
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

3. **锁性能优化**
   - 使用读写锁提高并发性能
   - 避免长时间持有锁
   - 启用锁性能监控

4. **序列化优化**
   - 使用高效的序列化器（如Jackson）
   - 避免序列化大对象
   - 考虑压缩选项

## 版本历史

- **1.0.0**: 初始版本，支持基础缓存功能
- **1.1.0**: 添加分布式锁和缓存预热
- **1.2.0**: 支持Redis集群和哨兵模式
- **1.3.0**: 增强异常处理和健康检查
- **1.4.0**: 新增统一LockManager和分布式死锁检测

## 贡献

欢迎提交Issue和Pull Request来改进这个模块！

## 许可证

本项目采用MIT许可证。