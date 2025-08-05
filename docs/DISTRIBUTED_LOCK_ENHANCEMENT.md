# 分布式锁增强功能使用指南

## 概述

Synapse Framework 的分布式锁模块提供了强大的锁功能增强，包括读写锁、公平锁、死锁检测和性能监控等功能。本文档详细介绍这些功能的使用方法。

## 功能特性

### 1. 读写锁 (ReadWriteLockService)

读写锁允许多个线程同时读取数据，但写操作是排他的。这提高了并发性能，特别适合读多写少的场景。

#### 基本用法

```java
@Autowired
private ReadWriteLockService readWriteLockService;

// 获取读锁
String readLockValue = readWriteLockService.tryReadLock("user", "123", 10);
if (readLockValue != null) {
    try {
        // 执行读操作
        User user = userService.getUser("123");
    } finally {
        readWriteLockService.releaseReadLock("user", "123");
    }
}

// 获取写锁
String writeLockValue = readWriteLockService.tryWriteLock("user", "123", 10);
if (writeLockValue != null) {
    try {
        // 执行写操作
        userService.updateUser("123", userData);
    } finally {
        readWriteLockService.releaseWriteLock("user", "123", writeLockValue);
    }
}
```

#### 便捷方法

```java
// 使用读锁执行操作
String result = readWriteLockService.executeWithReadLock("user", "123", () -> {
    return userService.getUser("123");
});

// 使用写锁执行操作
String result = readWriteLockService.executeWithWriteLock("user", "123", () -> {
    return userService.updateUser("123", userData);
});
```

### 2. 公平锁 (FairLockService)

公平锁确保按照请求顺序获取锁，避免饥饿问题。

#### 基本用法

```java
@Autowired
private FairLockService fairLockService;

// 尝试获取公平锁
String lockValue = fairLockService.tryFairLock("order", "456", 10);
if (lockValue != null) {
    try {
        // 执行操作
        orderService.processOrder("456");
    } finally {
        fairLockService.releaseFairLock("order", "456", lockValue);
    }
}

// 等待获取公平锁
String lockValue = fairLockService.fairLock("order", "456", 10, 30);
if (lockValue != null) {
    try {
        // 执行操作
        orderService.processOrder("456");
    } finally {
        fairLockService.releaseFairLock("order", "456", lockValue);
    }
}
```

#### 便捷方法

```java
// 使用公平锁执行操作
String result = fairLockService.executeWithFairLock("order", "456", () -> {
    return orderService.processOrder("456");
});
```

#### 获取队列信息

```java
// 获取等待队列长度
long queueLength = fairLockService.getQueueLength("order", "456");
System.out.println("等待队列长度: " + queueLength);
```

### 3. 统一锁管理器 (LockManager)

LockManager 提供了统一的 API，整合了所有锁功能，并集成了性能监控和死锁检测。

#### 基本用法

```java
@Autowired
private LockManager lockManager;

// 使用可重入锁
String result = lockManager.executeWithLock("user", "123", () -> {
    return userService.getUser("123");
}, LockManager.LockType.REENTRANT);

// 使用读写锁
String result = lockManager.executeWithReadLock("user", "123", () -> {
    return userService.getUser("123");
});

String result = lockManager.executeWithWriteLock("user", "123", () -> {
    return userService.updateUser("123", userData);
});

// 使用公平锁
String result = lockManager.executeWithLock("order", "456", () -> {
    return orderService.processOrder("456");
}, LockManager.LockType.FAIR);
```

#### 手动控制锁

```java
// 尝试获取锁
String lockValue = lockManager.tryLock("user", "123", 10, LockManager.LockType.REENTRANT);
if (lockValue != null) {
    try {
        // 执行操作
        userService.updateUser("123", userData);
    } finally {
        lockManager.unlock("user", "123", lockValue, LockManager.LockType.REENTRANT);
    }
}

// 等待获取锁
String lockValue = lockManager.lock("user", "123", 10, 30, LockManager.LockType.REENTRANT);
if (lockValue != null) {
    try {
        // 执行操作
        userService.updateUser("123", userData);
    } finally {
        lockManager.unlock("user", "123", lockValue, LockManager.LockType.REENTRANT);
    }
}
```

### 4. 性能监控 (LockPerformanceMonitor)

LockManager 自动集成了性能监控功能，可以监控锁的各种性能指标。

#### 获取性能统计

```java
// 获取特定锁的统计信息
LockPerformanceMonitor.LockStats stats = lockManager.getLockStats("user");
if (stats != null) {
    System.out.println("尝试次数: " + stats.attempts.sum());
    System.out.println("成功次数: " + stats.successes.sum());
    System.out.println("失败次数: " + stats.failures.sum());
    System.out.println("成功率: " + stats.getSuccessRate());
    System.out.println("平均等待时间: " + stats.getAverageWaitTime() + "ms");
    System.out.println("平均持有时间: " + stats.getAverageHoldTime() + "ms");
    System.out.println("慢锁比例: " + stats.getSlowLockRate());
}

// 获取全局统计信息
LockPerformanceMonitor.GlobalStats globalStats = lockManager.getGlobalStats();
System.out.println("全局尝试次数: " + globalStats.totalAttempts);
System.out.println("全局成功次数: " + globalStats.totalSuccesses);
System.out.println("全局成功率: " + globalStats.successRate);
System.out.println("死锁检测次数: " + globalStats.totalDeadlockDetections);
```

#### 重置统计信息

```java
// 重置特定锁的统计信息
lockManager.resetStats("user");

// 重置所有统计信息
lockManager.resetStats(null);
```

### 5. 死锁检测 (DeadlockDetector)

LockManager 自动集成了死锁检测功能，可以检测和预防死锁。

#### 获取死锁状态

```java
// 获取死锁检测状态
Map<String, Object> status = lockManager.getDeadlockStatus();
System.out.println("死锁检测状态: " + status);
```

## 使用场景

### 1. 读写锁场景

适用于读多写少的场景，如：
- 用户信息查询（读多）和更新（写少）
- 商品信息查询（读多）和库存更新（写少）
- 配置信息查询（读多）和配置更新（写少）

```java
@Service
public class UserService {
    
    @Autowired
    private LockManager lockManager;
    
    public User getUser(String userId) {
        return lockManager.executeWithReadLock("user", userId, () -> {
            // 从数据库或缓存读取用户信息
            return userRepository.findById(userId);
        });
    }
    
    public void updateUser(String userId, UserData userData) {
        lockManager.executeWithWriteLock("user", userId, () -> {
            // 更新用户信息
            userRepository.update(userId, userData);
            return null;
        });
    }
}
```

### 2. 公平锁场景

适用于需要保证顺序的场景，如：
- 订单处理（按订单创建顺序处理）
- 任务队列（按任务提交顺序执行）
- 资源分配（按申请顺序分配）

```java
@Service
public class OrderService {
    
    @Autowired
    private LockManager lockManager;
    
    public void processOrder(String orderId) {
        lockManager.executeWithLock("order", orderId, () -> {
            // 按顺序处理订单
            orderRepository.process(orderId);
            return null;
        }, LockManager.LockType.FAIR);
    }
}
```

### 3. 可重入锁场景

适用于需要嵌套锁的场景，如：
- 复杂业务逻辑（多个方法都需要锁）
- 递归操作（递归函数需要锁）
- 事务处理（事务内多个操作需要锁）

```java
@Service
public class ComplexService {
    
    @Autowired
    private LockManager lockManager;
    
    public void complexOperation(String resourceId) {
        lockManager.executeWithLock("resource", resourceId, () -> {
            // 复杂操作，可能调用其他需要锁的方法
            step1(resourceId);
            step2(resourceId);
            step3(resourceId);
            return null;
        }, LockManager.LockType.REENTRANT);
    }
    
    private void step1(String resourceId) {
        // 这个方法也需要锁，但由于是可重入锁，不会死锁
        lockManager.executeWithLock("resource", resourceId, () -> {
            // 执行步骤1
            return null;
        }, LockManager.LockType.REENTRANT);
    }
}
```

## 配置说明

### 1. 锁超时配置

```yaml
# application.yml
synapse:
  cache:
    lock:
      default-timeout: 10  # 默认锁超时时间（秒）
      max-wait-time: 30    # 最大等待时间（秒）
      watchdog-interval: 3000  # 看门狗检查间隔（毫秒）
      renewal-seconds: 10      # 续期时间（秒）
```

### 2. 死锁检测配置

```yaml
# application.yml
synapse:
  cache:
    deadlock:
      detection-interval: 5000  # 死锁检测间隔（毫秒）
      lock-timeout: 30          # 锁超时时间（秒）
      deadlock-timeout: 60      # 死锁检测超时时间（秒）
```

### 3. 性能监控配置

```yaml
# application.yml
synapse:
  cache:
    monitor:
      slow-lock-threshold: 1000     # 慢锁阈值（毫秒）
      deadlock-detection-threshold: 5000  # 死锁检测阈值（毫秒）
```

## 最佳实践

### 1. 锁粒度控制

- 使用细粒度锁，避免粗粒度锁导致的性能问题
- 根据业务场景选择合适的锁类型
- 避免长时间持有锁

```java
// 好的做法：细粒度锁
lockManager.executeWithLock("user", userId, () -> {
    // 只锁定特定用户
    return userService.updateUser(userId, data);
});

// 避免：粗粒度锁
lockManager.executeWithLock("all-users", "global", () -> {
    // 锁定所有用户，影响其他操作
    return userService.updateUser(userId, data);
});
```

### 2. 锁超时设置

- 根据业务操作时间合理设置锁超时时间
- 避免设置过短的超时时间导致频繁失败
- 避免设置过长的超时时间导致死锁风险

```java
// 根据操作复杂度设置超时时间
lockManager.executeWithLock("simple-operation", key, () -> {
    // 简单操作，短超时
    return simpleService.process();
}, LockManager.LockType.REENTRANT);

lockManager.executeWithLock("complex-operation", key, () -> {
    // 复杂操作，长超时
    return complexService.process();
}, LockManager.LockType.REENTRANT);
```

### 3. 异常处理

- 在 finally 块中释放锁
- 处理锁获取失败的情况
- 记录锁操作的异常信息

```java
String lockValue = null;
try {
    lockValue = lockManager.tryLock("resource", key, 10);
    if (lockValue != null) {
        // 执行操作
        return processResource();
    } else {
        // 处理获取锁失败的情况
        throw new RuntimeException("无法获取锁");
    }
} catch (Exception e) {
    log.error("处理资源时发生异常", e);
    throw e;
} finally {
    if (lockValue != null) {
        lockManager.unlock("resource", key, lockValue, LockManager.LockType.REENTRANT);
    }
}
```

### 4. 性能监控

- 定期检查锁性能统计
- 监控慢锁和死锁情况
- 根据监控数据优化锁使用

```java
// 定期检查性能统计
@Scheduled(fixedRate = 60000) // 每分钟检查一次
public void checkLockPerformance() {
    LockPerformanceMonitor.GlobalStats stats = lockManager.getGlobalStats();
    
    if (stats.successRate < 0.8) {
        log.warn("锁成功率过低: {}", stats.successRate);
    }
    
    if (stats.totalDeadlockDetections > 0) {
        log.warn("检测到死锁次数: {}", stats.totalDeadlockDetections);
    }
}
```

## 注意事项

1. **锁的可见性**：确保所有需要同步的操作都使用相同的锁
2. **避免死锁**：避免嵌套获取不同的锁，或使用统一的锁顺序
3. **性能影响**：锁操作会影响性能，合理使用锁
4. **监控告警**：设置锁性能的监控告警，及时发现问题
5. **测试验证**：在测试环境中验证锁的正确性和性能

## 总结

Synapse Framework 的分布式锁增强功能提供了完整的锁解决方案，包括读写锁、公平锁、死锁检测和性能监控。通过合理使用这些功能，可以有效解决分布式环境下的并发控制问题，提高系统的稳定性和性能。 