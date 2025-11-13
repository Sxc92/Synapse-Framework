# Synapse Cache 模块

## 概述

Synapse Cache 是一个功能强大的缓存模块，支持多种缓存策略和Redis连接模式。

## 特性

- ✅ **多模式Redis支持**: 单机、哨兵、集群模式
- ✅ **二级缓存**: Caffeine 本地缓存 + Redis 分布式缓存，自动降级
- ✅ **缓存失效通知**: 基于 Redis Pub/Sub 的分布式缓存一致性保证
- ✅ **并发控制**: 失效追踪机制防止旧数据写入
- ✅ **本地缓存**: 基于Caffeine的高性能本地缓存
- ✅ **分布式锁**: Redis分布式锁实现
- ✅ **缓存预热**: 支持应用启动时的缓存预热
- ✅ **穿透防护**: 布隆过滤器和限流防护
- ✅ **异常处理**: 完善的异常处理和降级策略
- ✅ **健康检查**: 缓存服务健康状态监控
- ✅ **连接池**: 可配置的连接池管理
- ✅ **用户会话管理**: 完整的用户会话、权限、角色管理

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

### 4. 使用用户会话服务

```java
@Service
public class AuthService {
    
    @Autowired
    private UserSessionService userSessionService;
    
    // 存储用户会话
    public void login(String token, UserContext userContext) {
        userSessionService.storeUserSession(token, userContext, 3600); // 1小时过期
    }
    
    // 获取用户会话（自动使用二级缓存）
    public UserContext getUserContext(String token) {
        return userSessionService.getUserSession(token);
    }
    
    // 获取用户权限（自动使用二级缓存）
    public List<String> getUserPermissions(String token) {
        return userSessionService.getUserPermissions(token);
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

### 二级缓存配置

```yaml
synapse:
  cache:
    # 二级缓存自动启用（如果 CaffeineCacheManager 可用）
    # 读取顺序：Caffeine 本地缓存 → Redis 分布式缓存
    # 写入顺序：Redis（主存储）→ Caffeine（本地缓存）
    local-cache:
      maximum-size: 10000        # 本地缓存最大条目数
      expire-after-write: "PT5M" # 本地缓存过期时间（通常为 Redis 的 1/10）
    
    # 缓存失效通知（自动启用，基于 Redis Pub/Sub）
    # 当某个节点更新缓存时，自动通知其他节点清除本地缓存
    # 确保分布式环境下缓存一致性
    
    # 会话缓存预热配置（应用重启后自动从 Redis 加载活跃会话到本地缓存）
    session-warmup:
      enabled: true              # 是否启用会话缓存预热
      max-count: 1000            # 最多预热的会话数量
      min-ttl-seconds: 300       # 最小 TTL（秒），只预热剩余时间 > 5 分钟的会话
      batch-size: 50             # 每批预热的会话数量
      thread-pool-size: 4        # 预热线程池大小
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

## 核心功能

### 二级缓存机制

Synapse Cache 实现了高性能的二级缓存架构：

#### 工作原理

```
读取流程：
  1. 优先从 Caffeine 本地缓存读取（毫秒级延迟）
  2. 如果本地缓存未命中，从 Redis 读取（网络延迟）
  3. 从 Redis 读取后，自动回填到本地缓存

写入流程：
  1. 写入 Redis（主存储，持久化）
  2. 写入 Caffeine 本地缓存（快速访问）
  3. 发布失效通知（通知其他节点清除本地缓存）

失效通知：
  1. 节点 A 更新缓存 → 发布失效事件（Redis Pub/Sub）
  2. 节点 B、C 收到事件 → 清除本地缓存
  3. 下次读取时从 Redis 获取最新数据
```

#### 性能优势

- **减少 Redis 查询**: 本地缓存命中时无需访问 Redis
- **降低网络延迟**: 本地缓存读取延迟 < 1ms
- **提升并发性能**: 减少 Redis 压力，支持更高并发
- **自动降级**: 本地缓存不可用时自动降级到 Redis

#### 并发控制

- **失效追踪**: 记录失效时间戳，防止写入旧数据
- **时间戳比较**: 写入前检查数据时间戳，确保数据新鲜度
- **线程安全**: 使用 `ConcurrentHashMap` 保证线程安全

### 缓存失效通知机制

#### 问题场景

在分布式环境下，多台机器可能存在缓存不一致问题：

```
机器 A: 更新权限 → 更新本地缓存 + Redis
机器 B: 本地缓存还是旧的 → 不会读取 Redis 最新数据
```

#### 解决方案

基于 Redis Pub/Sub 的失效通知机制：

1. **发布失效事件**: 更新缓存时自动发布失效事件
2. **订阅失效事件**: 所有节点订阅失效事件
3. **清除本地缓存**: 收到事件后立即清除本地缓存
4. **失效追踪**: 记录失效时间戳，防止写入旧数据

#### 工作流程

```
机器 A（更新权限）:
  1. 更新 Redis 缓存
  2. 更新本地 Caffeine 缓存
  3. 发布失效事件 → Redis Pub/Sub 频道

机器 B、C（接收通知）:
  1. 订阅 Redis Pub/Sub 事件
  2. 收到失效事件
  3. 记录失效时间戳
  4. 清除本地 Caffeine 缓存

机器 B、C（读取数据）:
  1. 从 Redis 读取数据（时间戳 T1）
  2. 检查失效时间戳 T2
  3. 如果 T1 < T2：跳过写入（数据已失效）
  4. 如果 T1 >= T2：写入本地缓存
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

### 3. 用户会话管理（自动使用二级缓存）

```java
@Service
public class AuthService {
    
    @Autowired
    private UserSessionService userSessionService;
    
    // 登录：存储用户会话和权限
    public void login(String token, UserContext userContext, List<String> permissions) {
        // 存储用户会话（自动写入 Redis + Caffeine，并发布失效通知）
        userSessionService.storeUserSession(token, userContext, 3600);
        
        // 存储用户权限（自动写入 Redis + Caffeine，并发布失效通知）
        userSessionService.storeUserPermissions(token, permissions, 3600);
    }
    
    // 获取用户会话（自动使用二级缓存：Caffeine → Redis）
    public UserContext getUserContext(String token) {
        return userSessionService.getUserSession(token);
    }
    
    // 获取用户权限（自动使用二级缓存）
    public List<String> getUserPermissions(String token) {
        return userSessionService.getUserPermissions(token);
    }
    
    // 更新权限（自动通知其他节点）
    public void updatePermissions(String token, List<String> newPermissions) {
        // 更新权限（自动发布失效通知，其他节点会清除本地缓存）
        userSessionService.storeUserPermissions(token, newPermissions, 3600);
    }
    
    // 登出：删除会话和权限
    public void logout(String token) {
        // 删除会话（自动发布失效通知）
        userSessionService.removeUserSession(token);
    }
}
```

### 4. 会话缓存预热（自动）

应用重启后，`SessionCacheWarmupService` 会自动从 Redis 加载活跃的用户会话和权限数据到 Caffeine 本地缓存：

```yaml
synapse:
  cache:
    session-warmup:
      enabled: true              # 启用预热（默认开启）
      max-count: 1000            # 最多预热 1000 个会话
      min-ttl-seconds: 300       # 只预热剩余时间 > 5 分钟的会话
      batch-size: 50             # 每批预热 50 个
```

**预热策略**：
- ✅ **智能过滤**：只预热活跃会话（TTL > 阈值），避免预热即将过期的数据
- ✅ **异步执行**：不阻塞应用启动，后台异步预热
- ✅ **分批处理**：分批预热，控制 Redis 压力
- ✅ **自动排序**：按 TTL 降序排序，优先预热剩余时间长的会话

**预热内容**：
- 用户会话（UserContext）
- 用户权限列表
- 用户角色列表

### 5. 通用缓存预热

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

4. **缓存不一致问题**
   - **现象**: 多台机器缓存数据不一致
   - **原因**: 失效通知未正常工作
   - **解决**: 
     - 检查 Redis Pub/Sub 连接是否正常
     - 查看日志确认失效事件是否发布和接收
     - 验证 `CacheInvalidationService` 是否正常启动
     - 检查网络延迟，确保失效通知及时到达

5. **本地缓存命中率低**
   - **现象**: 本地缓存命中率 < 50%
   - **原因**: 缓存过期时间设置不合理或数据更新频繁
   - **解决**:
     - 适当增加本地缓存过期时间
     - 检查是否有频繁的失效通知
     - 考虑增加本地缓存大小

6. **失效通知延迟**
   - **现象**: 权限更新后，其他节点仍使用旧数据
   - **原因**: Redis Pub/Sub 消息延迟或丢失
   - **解决**:
     - 检查 Redis 网络延迟
     - 验证 Redis Pub/Sub 连接稳定性
     - 考虑降低本地缓存过期时间作为兜底

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

2. **二级缓存优化**
   - **本地缓存大小**: 根据内存情况调整 `maximum-size`（建议 5000-10000）
   - **过期时间策略**: 本地缓存过期时间 = Redis 过期时间的 1/10
   - **缓存命中率**: 监控本地缓存命中率，目标 > 80%
   - **失效通知**: 确保 Redis Pub/Sub 正常工作，保证缓存一致性

3. **缓存策略优化**
   - 合理设置缓存过期时间
   - 使用本地缓存减少网络开销（减少 90%+ 的 Redis 查询）
   - 实现缓存预热机制
   - 避免频繁更新热点数据

4. **序列化优化**
   - 使用高效的序列化器（如Jackson）
   - 避免序列化大对象
   - 考虑压缩选项

5. **分布式缓存一致性**
   - 确保 Redis Pub/Sub 连接稳定
   - 监控失效通知的延迟
   - 定期检查失效追踪器的内存使用
   - 在权限变更频繁的场景，考虑降低本地缓存过期时间

## 架构设计

### 二级缓存架构

```
┌─────────────────────────────────────────────────────────┐
│                   应用层（业务代码）                      │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│              UserSessionService（门面层）                │
│  - 协调 SessionManager、CachePermissionManager          │
└────────────────────┬────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        │                         │
┌───────▼────────┐      ┌────────▼──────────┐
│ SessionManager │      │PermissionManager  │
│  (会话管理)     │      │  (权限管理)        │
└───────┬────────┘      └────────┬──────────┘
        │                        │
        └────────────┬────────────┘
                     │
        ┌────────────▼────────────┐
        │   二级缓存读取策略        │
        │  1. Caffeine 本地缓存    │
        │  2. Redis 分布式缓存     │
        │  3. 自动回填本地缓存      │
        └────────────┬────────────┘
                     │
        ┌────────────▼────────────┐
        │   缓存失效通知机制        │
        │  - Redis Pub/Sub         │
        │  - 失效追踪器             │
        │  - 并发控制               │
        └─────────────────────────┘
```

### 缓存失效通知流程

```
┌──────────┐                    ┌──────────┐
│  机器 A  │                    │  机器 B  │
└────┬─────┘                    └────┬─────┘
     │                                │
     │ 1. 更新 Redis                  │
     ├───────────────────────────────►│
     │                                │
     │ 2. 更新本地缓存                 │
     │                                │
     │ 3. 发布失效事件                 │
     ├───────────┐                    │
     │           │                    │
     │           ▼                    │
     │    Redis Pub/Sub              │
     │           │                    │
     │           ├───────────────────►│
     │                                │
     │                        4. 收到失效事件
     │                                │
     │                        5. 记录失效时间戳
     │                                │
     │                        6. 清除本地缓存
     │                                │
     │                        7. 下次读取时检查失效状态
     │                                │
```

## 版本历史

- **1.0.0**: 初始版本，支持基础缓存功能
- **1.1.0**: 添加分布式锁和缓存预热
- **1.2.0**: 支持Redis集群和哨兵模式
- **1.3.0**: 增强异常处理和健康检查
- **1.4.0**: 
  - ✨ 实现二级缓存机制（Caffeine + Redis）
  - ✨ 添加缓存失效通知机制（基于 Redis Pub/Sub）
  - ✨ 实现并发控制机制（失效追踪器）
  - ✨ 优化用户会话和权限管理性能
  - 🐛 修复分布式环境下缓存一致性问题
  - ✨ **1.4.1**: 添加会话缓存预热机制（应用重启后自动从 Redis 加载活跃会话到本地缓存）

## 贡献

欢迎提交Issue和Pull Request来改进这个模块！

## 许可证

本项目采用MIT许可证。 