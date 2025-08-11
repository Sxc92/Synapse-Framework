# Synapse Framework 开发笔记

## 📋 概述

本文档记录了 Synapse Framework 开发过程中的重要重构、功能增强和架构优化记录。包括设计思路、实现细节、问题解决和最佳实践。

---

## 🔄 会话管理模块重构 (2025年)

### 重构背景

会话管理模块在初始设计中存在以下问题：
- `RedisService` 包含业务逻辑（token/session管理）
- `UserSessionService` 与 `SessionManager` 功能重复
- 跨层调用问题：`UserSessionService` 直接依赖 `CacheService`
- 多个类承担相似职责，缺乏统一的接口设计

### 重构方案

#### 1. 类职责重新定义

**RedisService**
- **职责**：纯基础设施服务，提供Redis操作能力
- **功能**：基础的Redis操作（get、set、expire等）
- **移除**：所有业务逻辑方法（token、session管理）

**UserSessionService**
- **职责**：门面模式（Facade Pattern），协调各个管理器
- **功能**：对外提供统一的会话管理接口
- **依赖**：只依赖管理器接口，不直接操作基础设施

**SessionManager**
- **职责**：会话管理核心接口
- **功能**：定义所有会话相关的操作方法
- **扩展**：新增token管理和session数据管理方法

**DefaultSessionManager**
- **职责**：SessionManager的具体实现
- **功能**：实现所有会话管理逻辑
- **依赖**：直接使用CacheService和RedisService进行底层操作

#### 2. 重构后的类图

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ UserSessionService │    │  SessionManager  │    │ DefaultSession  │
│                 │    │                 │    │    Manager     │
│ • 门面模式      │    │ • 接口定义      │    │ • 具体实现      │
│ • 协调管理     │────▶│ • 方法声明      │◀───│ • 业务逻辑      │
│ • 对外接口     │    │ • 契约约束      │    │ • 底层操作      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ PermissionManager│    │StatisticsManager │    │   CacheService  │
│                 │    │                 │    │                 │
│ • 权限管理      │    │ • 统计管理      │    │ • 缓存服务      │
│ • 权限验证      │    │ • 数据统计      │    │ • 服务包装      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                       │
                                                       ▼
                                              ┌─────────────────┐
                                              │   RedisService  │
                                              │                 │
                                              │ • Redis操作     │
                                              │ • 基础设施     │
                                              └─────────────────┘
```

#### 3. 重构实现细节

**SessionManager接口扩展**
```java
public interface SessionManager {
    // 原有会话管理方法
    void createSession(String userId, long expireSeconds);
    void extendSession(String userId, long expireSeconds);
    boolean isSessionValid(String userId);
    void removeSession(String userId);
    
    // 新增token管理方法
    void storeToken(String token, String userId, long expireSeconds);
    String validateToken(String token);
    boolean refreshToken(String token, long expireSeconds);
    void removeToken(String token);
    boolean tokenExists(String token);
    long getTokenTtl(String token);
    
    // 新增session数据管理方法
    void storeUserSessionData(String userId, Object sessionData, long expireSeconds);
    <T> T getUserSessionData(String userId, Class<T> clazz);
    void removeUserSessionData(String userId);
}
```

**DefaultSessionManager实现**
```java
@Service
public class DefaultSessionManager implements SessionManager {
    
    @Override
    public void storeToken(String token, String userId, long expireSeconds) {
        String tokenKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "token", token);
        cacheService.getRedisService().set(tokenKey, userId, expireSeconds);
        log.info("Stored token: {} for user: {}", token, userId);
    }
    
    @Override
    public String validateToken(String token) {
        String tokenKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "token", token);
        Object result = cacheService.getRedisService().get(tokenKey);
        return result != null ? result.toString() : null;
    }
    
    // ... 其他方法实现
}
```

**UserSessionService简化**
```java
@Service
public class UserSessionService {
    private final SessionManager sessionManager;
    private final PermissionManager permissionManager;
    private final StatisticsManager statisticsManager;
    
    // 所有方法都委托给相应的管理器
    public void storeToken(String token, String userId, long expireSeconds) {
        sessionManager.storeToken(token, userId, expireSeconds);
    }
    
    public String validateToken(String token) {
        return sessionManager.validateToken(token);
    }
    
    // ... 其他委托方法
}
```

### 重构优势

1. **职责清晰**：每个类都有明确的单一职责
2. **依赖关系合理**：遵循依赖倒置原则
3. **易于扩展**：新增功能只需实现相应接口
4. **便于测试**：可以独立测试每个组件

### 重构后的使用方式

**基本使用**
```java
@Autowired
private UserSessionService userSessionService;

// 存储token
userSessionService.storeToken("token123", "user456", 3600);

// 验证token
String userId = userSessionService.validateToken("token123");

// 创建会话
userSessionService.createSession("user456", 7200);
```

**扩展使用**
```java
// 存储会话数据
Map<String, Object> sessionData = new HashMap<>();
sessionData.put("lastLoginTime", System.currentTimeMillis());
sessionData.put("loginIp", "192.168.1.100");
userSessionService.storeUserSessionData("user456", sessionData, 7200);

// 获取会话数据
Map<String, Object> data = userSessionService.getUserSessionData("user456", Map.class);
```

---

## 🔒 分布式锁模块增强 (2025年)

### 增强背景

原有的分布式锁功能较为基础，缺乏高级特性：
- 只支持基本的加锁和解锁
- 缺乏性能监控和死锁检测
- 没有读写锁和公平锁支持
- 锁管理分散，缺乏统一入口

### 增强方案

#### 1. 新增锁类型

**读写锁 (ReadWriteLockService)**
- 支持读锁和写锁的分离
- 读锁可以并发获取，写锁独占
- 防止写饥饿问题

**公平锁 (FairLockService)**
- 按照请求顺序获取锁
- 避免锁饥饿问题
- 支持优先级设置

#### 2. 锁管理统一

**LockManager 统一入口**
```java
@Service
public class LockManager {
    private final DistributedLockService distributedLockService;
    private final ReadWriteLockService readWriteLockService;
    private final FairLockService fairLockService;
    private final DeadlockDetector deadlockDetector;
    private final LockPerformanceMonitor performanceMonitor;
    
    // 提供统一的锁操作方法
    public <T> T executeWithLock(String lockKey, LockAction<T> action) throws Exception {
        // 统一的锁执行逻辑
    }
    
    public <T> T executeWithReadWriteLock(String lockKey, boolean isWrite, LockAction<T> action) throws Exception {
        // 读写锁执行逻辑
    }
}
```

#### 3. 死锁检测和预防

**DeadlockDetector**
- 检测循环等待的锁依赖
- 自动释放死锁中的锁
- 提供死锁预防建议

**LockPerformanceMonitor**
- 监控锁的获取和释放时间
- 统计锁竞争情况
- 提供性能优化建议

#### 4. 锁操作统一接口

**LockAction 函数式接口**
```java
@FunctionalInterface
public interface LockAction<T> {
    T execute() throws Exception;
}
```

### 使用示例

**基本分布式锁**
```java
@Autowired
private LockManager lockManager;

public void processWithLock(String resourceId) {
    try {
        lockManager.executeWithLock("resource:" + resourceId, () -> {
            // 临界区代码
            return null;
        });
    } catch (Exception e) {
        log.error("Lock execution failed", e);
    }
}
```

**读写锁**
```java
// 读锁
lockManager.executeWithReadWriteLock("data:" + id, false, () -> {
    // 读取操作
    return data;
});

// 写锁
lockManager.executeWithReadWriteLock("data:" + id, true, () -> {
    // 写入操作
    return null;
});
```

---

## 🎯 缓存属性翻译注解设计 (2025年)

### 设计背景

在业务开发中，经常需要将ID字段翻译为对应的名称、描述等信息：
- 用户ID → 用户名、昵称
- 商品ID → 商品名称、价格
- 分类ID → 分类名称、描述

传统方式需要在业务代码中手动查询和设置，代码重复且维护困难。

### 设计方案

#### 1. 注解设计

**扩展现有缓存注解**
```java
@CachePut(
    module = "user", 
    key = "#user.id", 
    translationData = true,
    translationSource = "user", 
    translationFields = {"username", "nickname"}
)
public User saveUser(User user) {
    return userRepository.save(user);
}
```

**新增字段级翻译注解**
```java
public class OrderDTO {
    private Long id;
    private Long userId;
    
    @CacheTranslate(source = "user", key = "#userId", field = "username")
    private String username;
    
    @CacheTranslate(source = "user", key = "#userId", field = "nickname")
    private String nickname;
}
```

**组合注解支持**
```java
@Caching(
    put = {
        @CachePut(module = "user", key = "#user.id", translationData = true)
    },
    translate = {
        @CacheTranslate(source = "user", key = "#userId", field = "username"),
        @CacheTranslate(source = "user", key = "#userId", field = "nickname")
    }
)
public OrderDTO createOrder(Order order) {
    return orderService.create(order);
}
```

#### 2. 核心组件

**CacheTranslationProcessor**
- 自动扫描并处理翻译标记
- 实现翻译逻辑的自动执行
- 支持批量翻译优化

**TranslationCacheManager**
- 管理翻译数据的缓存存储
- 实现翻译缓存的预热机制
- 支持翻译缓存的失效和更新

**CacheTranslationAspect**
- 自动拦截带有翻译注解的方法
- 实现翻译逻辑的自动执行
- 支持翻译结果的自动设置

#### 3. 配置支持

**翻译策略配置**
```yaml
synapse:
  cache:
    translation:
      enabled: true
      strategy: ASYNC  # SYNC, ASYNC, BATCH
      timeout: 5000ms
      retry-count: 3
      data-sources:
        user:
          cache-key: "user:{id}"
          fields: ["username", "nickname", "avatar"]
        product:
          cache-key: "product:{id}"
          fields: ["name", "price", "description"]
```

### 使用流程

1. **标记翻译字段**：在DTO/POJO/VO字段上添加翻译注解
2. **配置翻译源**：在配置文件中定义翻译数据源
3. **自动执行翻译**：框架自动拦截并执行翻译逻辑
4. **缓存翻译结果**：翻译结果自动缓存，提高性能

### 优势分析

1. **代码简洁**：通过注解声明，无需手动编写翻译逻辑
2. **性能优化**：翻译结果自动缓存，避免重复查询
3. **易于维护**：翻译逻辑集中管理，便于修改和扩展
4. **灵活配置**：支持多种翻译策略和数据源配置

---

## 📊 日志优化 (2025年)

### 优化内容

1. **日志级别调整**
   - 将所有 `log.info()` 改为 `log.info()`
   - 移除 `log.isDebugEnabled()` 检查

2. **日志内容优化**
   - 统一日志格式
   - 添加更多有用的调试信息
   - 优化错误日志的详细信息

### 优化原因

- 开发环境中debug日志通常被过滤，导致重要信息丢失
- `isDebugEnabled()` 检查增加了代码复杂度
- 统一的日志级别便于问题排查和监控

---

## 🏗️ 架构优化总结

### 设计原则

1. **单一职责原则**：每个类只负责一个功能领域
2. **依赖倒置原则**：高层模块不依赖低层模块的具体实现
3. **开闭原则**：对扩展开放，对修改关闭
4. **接口隔离原则**：客户端不应该依赖它不需要的接口

### 架构模式

1. **门面模式**：`UserSessionService` 作为会话管理的统一入口
2. **策略模式**：不同的锁类型和缓存策略
3. **模板方法模式**：锁操作的统一流程
4. **工厂模式**：锁服务的创建和管理

### 最佳实践

1. **分层架构**：明确各层的职责和依赖关系
2. **接口设计**：通过接口定义契约，便于扩展和维护
3. **异常处理**：统一的异常处理机制
4. **性能监控**：内置性能监控和优化建议

---

## 🔮 未来规划

### 短期目标 (1-2个月)
- [ ] 完善缓存属性翻译注解功能
- [ ] 优化分布式锁的性能监控
- [ ] 添加更多缓存策略和算法

### 中期目标 (3-6个月)
- [ ] 实现缓存集群支持
- [ ] 添加缓存数据同步机制
- [ ] 支持更多类型的分布式锁

### 长期目标 (6-12个月)
- [ ] 实现智能缓存预热
- [ ] 添加机器学习驱动的缓存优化
- [ ] 支持多云环境的缓存部署

---

## 📝 更新记录

| 日期 | 版本 | 更新内容 | 负责人 |
|------|------|----------|--------|
| 2025-01 | v1.0 | 会话管理模块重构 | 开发团队 |
| 2025-01 | v1.1 | 分布式锁模块增强 | 开发团队 |
| 2025-01 | v1.2 | 缓存属性翻译注解设计 | 开发团队 |
| 2025-01 | v1.3 | 日志优化和架构优化 | 开发团队 | 