# Synapse Cache 注解使用指南

## 📋 概述

Synapse Cache 提供了完整的缓存注解支持，包括 `@Cacheable`、`@CachePut`、`@CacheEvict` 和 `@Caching` 注解，帮助开发者轻松实现方法级别的缓存操作。

## 🎯 注解列表

### 1. @Cacheable - 缓存注解

用于缓存方法的返回值，如果缓存中存在数据则直接返回，不执行方法。

```java
@Cacheable(key = "user:#id", expireSeconds = 3600)
public User getUserById(Long id) {
    return userRepository.findById(id);
}
```

**参数说明：**
- `module`: 缓存模块名（可选，默认使用类名）
- `key`: 缓存键，支持 SpEL 表达式
- `expireSeconds`: 过期时间（秒），默认 3600
- `strategy`: 缓存策略，默认 LOCAL_AND_REDIS
- `disableOnException`: 异常时是否禁用缓存，默认 true
- `condition`: 缓存条件，支持 SpEL 表达式

### 2. @CachePut - 缓存更新注解

用于更新缓存数据，无论方法是否返回 null 都会更新缓存。

```java
@CachePut(key = "user:#id", expireSeconds = 3600)
public User updateUser(Long id, String name) {
    User user = userRepository.findById(id);
    user.setName(name);
    return userRepository.save(user);
}
```

**参数说明：**
- `module`: 缓存模块名（可选，默认使用类名）
- `key`: 缓存键，支持 SpEL 表达式
- `expireSeconds`: 过期时间（秒），默认 3600
- `strategy`: 缓存策略，默认 LOCAL_AND_REDIS
- `disableOnException`: 异常时是否禁用缓存，默认 true
- `condition`: 缓存条件，支持 SpEL 表达式
- `beforeInvocation`: 是否在方法执行前更新缓存，默认 false

### 3. @CacheEvict - 缓存删除注解

用于删除缓存数据。

```java
@CacheEvict(key = "user:#id")
public void deleteUser(Long id) {
    userRepository.deleteById(id);
}
```

**参数说明：**
- `module`: 缓存模块名（可选，默认使用类名）
- `key`: 缓存键，支持 SpEL 表达式
- `allEntries`: 是否清除模块下所有缓存，默认 false
- `strategy`: 缓存策略，默认 LOCAL_AND_REDIS
- `beforeInvocation`: 是否在方法执行前清除缓存，默认 false
- `disableOnException`: 异常时是否禁用缓存操作，默认 true
- `condition`: 删除条件，支持 SpEL 表达式

### 4. @Caching - 组合注解

用于在一个方法上组合多个缓存操作。

```java
@Caching(
    put = @CachePut(key = "user:#id", expireSeconds = 3600),
    evict = @CacheEvict(key = "userList", allEntries = true)
)
public User updateUserWithCaching(Long id, String name) {
    User user = userRepository.findById(id);
    user.setName(name);
    return userRepository.save(user);
}
```

**参数说明：**
- `cacheable`: Cacheable 操作数组
- `put`: CachePut 操作数组
- `evict`: CacheEvict 操作数组

## 🔧 缓存策略

### CacheStrategy 枚举

```java
public enum CacheStrategy {
    /**
     * 只使用本地缓存，适用于频繁读取但更新不频繁的数据
     */
    LOCAL_ONLY,
    
    /**
     * 只使用Redis缓存，适用于分布式一致性要求高的数据
     */
    REDIS_ONLY,
    
    /**
     * 同时使用本地缓存和Redis缓存，本地缓存优先，适用于读取频繁但偶尔更新的数据
     */
    LOCAL_AND_REDIS,
    
    /**
     * Redis同步到本地，Redis变更后本地自动同步，适用于较长有效期的数据
     */
    REDIS_SYNC_TO_LOCAL
}
```

## 📝 使用示例

### 1. 基础缓存操作

```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    // 缓存用户信息
    @Cacheable(key = "user:#id", expireSeconds = 3600)
    public User getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    // 更新用户信息并更新缓存
    @CachePut(key = "user:#id", expireSeconds = 3600)
    public User updateUser(Long id, String name) {
        User user = userRepository.findById(id);
        user.setName(name);
        return userRepository.save(user);
    }
    
    // 删除用户并清除缓存
    @CacheEvict(key = "user:#id")
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
```

### 2. 条件缓存

```java
@Service
public class UserService {
    
    // 只有当用户不为 null 时才缓存
    @Cacheable(key = "user:#id", condition = "#result != null")
    public User getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    // 只有当用户存在时才更新缓存
    @CachePut(key = "user:#id", condition = "#result != null")
    public User updateUser(Long id, String name) {
        User user = userRepository.findById(id);
        if (user != null) {
            user.setName(name);
            return userRepository.save(user);
        }
        return null;
    }
}
```

### 3. 组合缓存操作

```java
@Service
public class UserService {
    
    // 更新用户信息，同时更新用户缓存和清除用户列表缓存
    @Caching(
        put = @CachePut(key = "user:#id", expireSeconds = 3600),
        evict = @CacheEvict(key = "userList", allEntries = true)
    )
    public User updateUserWithCaching(Long id, String name) {
        User user = userRepository.findById(id);
        user.setName(name);
        return userRepository.save(user);
    }
    
    // 删除用户，同时清除用户缓存和用户列表缓存
    @Caching(
        evict = {
            @CacheEvict(key = "user:#id"),
            @CacheEvict(key = "userList", allEntries = true)
        }
    )
    public void deleteUserWithCaching(Long id) {
        userRepository.deleteById(id);
    }
}
```

### 4. 自定义缓存策略

```java
@Service
public class UserService {
    
    // 使用本地缓存策略，适用于频繁读取的数据
    @Cacheable(
        key = "user:#id", 
        strategy = TwoLevelCacheService.CacheStrategy.LOCAL_ONLY,
        expireSeconds = 1800
    )
    public User getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    // 使用Redis缓存策略，适用于分布式一致性要求高的数据
    @Cacheable(
        key = "user:#id", 
        strategy = TwoLevelCacheService.CacheStrategy.REDIS_ONLY,
        expireSeconds = 7200
    )
    public User getUserByIdForDistributed(Long id) {
        return userRepository.findById(id);
    }
}
```

### 5. 异常处理

```java
@Service
public class UserService {
    
    // 异常时禁用缓存，直接执行方法
    @Cacheable(
        key = "user:#id", 
        disableOnException = true
    )
    public User getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    // 异常时不禁用缓存，可能抛出异常
    @Cacheable(
        key = "user:#id", 
        disableOnException = false
    )
    public User getUserByIdStrict(Long id) {
        return userRepository.findById(id);
    }
}
```

## ⚠️ 注意事项

### 1. SpEL 表达式支持

所有注解都支持 SpEL 表达式，可以使用以下变量：
- `#result`: 方法返回值
- `#root.method`: 当前方法
- `#root.target`: 当前对象
- `#root.args`: 方法参数数组
- `#参数名`: 具体的参数值

### 2. 缓存键生成

缓存键会自动添加模块前缀，格式为：`模块名:键名`

### 3. 异常处理

当方法抛出异常时，根据 `disableOnException` 参数决定是否禁用缓存操作。

### 4. 性能考虑

- 使用 `LOCAL_ONLY` 策略可以获得最佳性能
- 使用 `REDIS_ONLY` 策略可以获得最佳一致性
- 使用 `LOCAL_AND_REDIS` 策略可以平衡性能和一致性

## 🧪 测试

运行测试用例验证注解功能：

```bash
mvn test -Dtest=CacheAnnotationTest
```

## 🔒 分布式锁支持

除了缓存注解，Synapse Cache 还提供了强大的分布式锁功能，支持延迟初始化和自动释放机制。

### 基本用法

```java
@Autowired
private LockManager lockManager;

// 获取分布式锁
String lockValue = lockManager.tryLock("order", "123", 10);
if (lockValue != null) {
    try {
        // 执行业务逻辑
        processOrder("123");
    } finally {
        // 释放锁
        lockManager.releaseLock("order", "123", lockValue);
    }
}
```

### 高级特性

- **延迟初始化**: 服务启动时不占用资源，首次使用时才初始化
- **自动释放**: 长时间未使用时自动释放资源
- **性能监控**: 提供详细的性能指标和死锁检测
- **配置灵活**: 支持多种配置选项和阈值设置

详细用法请参考：
- [分布式锁优化文档](DISTRIBUTED_LOCK_OPTIMIZATION.md)
- [优化工作总结](OPTIMIZATION_SUMMARY.md)

## 📚 更多信息

- [缓存策略详解](CACHE_STRATEGY.md)
- [性能优化指南](PERFORMANCE_GUIDE.md)
- [常见问题解答](FAQ.md)
- [分布式锁优化](DISTRIBUTED_LOCK_OPTIMIZATION.md)
- [优化工作总结](OPTIMIZATION_SUMMARY.md) 