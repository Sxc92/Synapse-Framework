# LockManager 统一分布式锁管理指南

## 概述

`LockManager` 是 Synapse Cache 模块中分布式锁的统一入口，提供了多种锁类型的统一管理和高级功能。它整合了可重入锁、读写锁、公平锁以及分布式死锁检测功能，为开发者提供简洁而强大的分布式锁解决方案。

## 核心特性

- 🔒 **统一API**: 提供一致的锁操作接口，屏蔽底层复杂性
- 🔄 **多种锁类型**: 支持可重入锁、读写锁、公平锁
- 📊 **性能监控**: 集成锁性能监控和统计
- 🔍 **死锁检测**: 集成死锁检测和预防机制
- ⚡ **自动管理**: 自动选择合适的锁类型和超时策略
- 🛡️ **异常处理**: 统一的异常处理和日志记录

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    LockManager                              │
│                (统一入口 - 对外暴露)                          │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │ DistributedLock  │  │ ReadWriteLock    │  │ FairLock     │ │
│  │ Service         │  │ Service          │  │ Service     │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │ DeadlockDetector │  │ Performance     │  │ FastRecovery │ │
│  │ (Local)          │  │ Monitor         │  │ Manager      │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────┐ │
│  │        DistributedDeadlockDetector                      │ │
│  │        (分布式死锁检测)                                  │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## 快速开始

### 1. 依赖注入

```java
@Service
public class OrderService {
    
    @Autowired
    private LockManager lockManager;
    
    // 业务方法...
}
```

### 2. 基础锁操作

```java
public void processOrder(String orderId) {
    String lockValue = lockManager.tryLock("order", orderId, 30);
    if (lockValue != null) {
        try {
            // 业务逻辑
            processOrderLogic(orderId);
        } finally {
            lockManager.unlock("order", orderId, lockValue, LockManager.LockType.REENTRANT);
        }
    } else {
        throw new RuntimeException("获取锁失败");
    }
}
```

## 锁类型详解

### 1. 可重入锁 (REENTRANT)

可重入锁允许同一线程多次获取同一把锁，适用于需要递归调用的场景。

#### 特性
- ✅ 支持同一线程多次获取
- ✅ 自动计数管理
- ✅ 避免死锁风险

#### 使用示例

```java
@Service
public class RecursiveService {
    
    @Autowired
    private LockManager lockManager;
    
    public void recursiveOperation(String resourceId) {
        String lockValue = lockManager.tryLock("recursive", resourceId, 30);
        if (lockValue != null) {
            try {
                // 第一次获取锁成功
                performOperation(resourceId);
            } finally {
                lockManager.unlock("recursive", resourceId, lockValue, LockManager.LockType.REENTRANT);
            }
        }
    }
    
    private void performOperation(String resourceId) {
        // 在同一个线程中再次获取同一把锁
        String lockValue = lockManager.tryLock("recursive", resourceId, 30);
        if (lockValue != null) {
            try {
                // 递归调用或嵌套操作
                nestedOperation(resourceId);
            } finally {
                lockManager.unlock("recursive", resourceId, lockValue, LockManager.LockType.REENTRANT);
            }
        }
    }
}
```

### 2. 读写锁 (READ_WRITE)

读写锁支持多个读锁或一个写锁，读锁之间不互斥，写锁与读锁/写锁互斥。

#### 特性
- ✅ 多个读锁并发
- ✅ 写锁独占访问
- ✅ 提高并发性能

#### 使用示例

```java
@Service
public class DataService {
    
    @Autowired
    private LockManager lockManager;
    
    /**
     * 读取数据 - 使用读锁
     */
    public String readData(String dataId) {
        String lockValue = lockManager.tryReadLock("data", dataId, 10);
        if (lockValue != null) {
            try {
                // 多个线程可以同时读取
                return performRead(dataId);
            } finally {
                lockManager.releaseReadLock("data", dataId);
            }
        }
        return null;
    }
    
    /**
     * 更新数据 - 使用写锁
     */
    public void updateData(String dataId, String newData) {
        String lockValue = lockManager.tryWriteLock("data", dataId, 10);
        if (lockValue != null) {
            try {
                // 独占访问，确保数据一致性
                performUpdate(dataId, newData);
            } finally {
                lockManager.unlock("data", dataId, lockValue, LockManager.LockType.READ_WRITE);
            }
        }
    }
    
    /**
     * 读写锁混合使用
     */
    public void readAndUpdate(String dataId) {
        // 先获取读锁检查数据
        String readLockValue = lockManager.tryReadLock("data", dataId, 5);
        if (readLockValue != null) {
            try {
                String currentData = performRead(dataId);
                if (needsUpdate(currentData)) {
                    // 释放读锁，获取写锁
                    lockManager.releaseReadLock("data", dataId);
                    
                    String writeLockValue = lockManager.tryWriteLock("data", dataId, 10);
                    if (writeLockValue != null) {
                        try {
                            performUpdate(dataId, processData(currentData));
                        } finally {
                            lockManager.unlock("data", dataId, writeLockValue, LockManager.LockType.READ_WRITE);
                        }
                    }
                }
            } finally {
                if (readLockValue != null) {
                    lockManager.releaseReadLock("data", dataId);
                }
            }
        }
    }
}
```

### 3. 公平锁 (FAIR)

公平锁按照请求顺序获取锁，避免锁饥饿问题。

#### 特性
- ✅ 按请求顺序获取锁
- ✅ 避免锁饥饿
- ✅ 保证公平性

#### 使用示例

```java
@Service
public class FairResourceService {
    
    @Autowired
    private LockManager lockManager;
    
    public void accessResource(String resourceId) {
        // 使用公平锁确保按顺序访问
        String lockValue = lockManager.tryLock("resource", resourceId, 30, LockManager.LockType.FAIR);
        if (lockValue != null) {
            try {
                // 按请求顺序处理资源
                processResource(resourceId);
            } finally {
                lockManager.unlock("resource", resourceId, lockValue, LockManager.LockType.FAIR);
            }
        }
    }
}
```

## 高级功能

### 1. 便捷执行方法

LockManager 提供了便捷的执行方法，自动处理锁的获取和释放。

```java
@Service
public class ConvenientService {
    
    @Autowired
    private LockManager lockManager;
    
    /**
     * 便捷执行 - 自动处理锁
     */
    public String processWithLock(String resourceId) {
        return lockManager.executeWithLock("resource", resourceId, () -> {
            // 业务逻辑
            return performOperation(resourceId);
        });
    }
    
    /**
     * 读锁便捷执行
     */
    public String readWithLock(String resourceId) {
        return lockManager.executeWithReadLock("data", resourceId, () -> {
            return performRead(resourceId);
        });
    }
    
    /**
     * 写锁便捷执行
     */
    public void updateWithLock(String resourceId, String data) {
        lockManager.executeWithWriteLock("data", resourceId, () -> {
            performUpdate(resourceId, data);
            return null;
        });
    }
}
```

### 2. 等待锁获取

支持等待获取锁，避免立即失败。

```java
@Service
public class WaitLockService {
    
    @Autowired
    private LockManager lockManager;
    
    public void processWithWait(String resourceId) {
        // 等待最多60秒获取锁，锁超时30秒
        String lockValue = lockManager.lock("resource", resourceId, 30, 60, LockManager.LockType.REENTRANT);
        if (lockValue != null) {
            try {
                // 业务逻辑
                processResource(resourceId);
            } finally {
                lockManager.unlock("resource", resourceId, lockValue, LockManager.LockType.REENTRANT);
            }
        } else {
            throw new RuntimeException("等待获取锁超时");
        }
    }
}
```

### 3. 性能监控

LockManager 集成了性能监控功能，可以获取锁的使用统计。

```java
@Service
public class PerformanceMonitorService {
    
    @Autowired
    private LockManager lockManager;
    
    /**
     * 获取锁性能统计
     */
    public void logPerformanceStats() {
        Map<String, Object> stats = lockManager.getPerformanceStatistics();
        log.info("锁性能统计: {}", stats);
        
        // 统计信息包括：
        // - 锁获取次数
        // - 锁获取成功率
        // - 平均获取时间
        // - 锁持有时间
        // - 死锁检测次数
    }
    
    /**
     * 获取分布式死锁检测状态
     */
    public void logDeadlockStatus() {
        if (lockManager.isDistributedDeadlockEnabled()) {
            Map<String, Object> status = lockManager.getDistributedDeadlockStatus();
            log.info("分布式死锁检测状态: {}", status);
        }
    }
}
```

### 4. 分布式死锁检测

LockManager 集成了分布式死锁检测功能。

```java
@Service
public class DeadlockDetectionService {
    
    @Autowired
    private LockManager lockManager;
    
    /**
     * 手动触发死锁检测
     */
    public void detectDeadlocks() {
        List<Set<String>> cycles = lockManager.detectGlobalDeadlocks();
        if (!cycles.isEmpty()) {
            log.warn("检测到死锁环: {}", cycles);
            // 处理死锁...
        }
    }
    
    /**
     * 同步本地状态到全局
     */
    public void syncState() {
        lockManager.syncLocalStateToGlobal();
    }
    
    /**
     * 控制分布式检测
     */
    public void controlDetection(boolean enabled) {
        lockManager.setGlobalDetectionEnabled(enabled);
    }
}
```

## 最佳实践

### 1. 锁命名规范

```java
// ✅ 好的命名
lockManager.tryLock("user:profile", userId, 30);
lockManager.tryLock("order:process", orderId, 30);
lockManager.tryLock("inventory:update", productId, 30);

// ❌ 避免的命名
lockManager.tryLock("lock1", "key1", 30);
lockManager.tryLock("temp", "data", 30);
```

### 2. 超时时间设置

```java
// ✅ 合理的超时时间
lockManager.tryLock("quick-operation", key, 5);    // 快速操作
lockManager.tryLock("normal-operation", key, 30);  // 普通操作
lockManager.tryLock("slow-operation", key, 120);   // 慢操作

// ❌ 避免的超时时间
lockManager.tryLock("operation", key, 0);     // 不等待
lockManager.tryLock("operation", key, 3600);  // 过长等待
```

### 3. 异常处理

```java
@Service
public class SafeLockService {
    
    @Autowired
    private LockManager lockManager;
    
    public void safeOperation(String resourceId) {
        String lockValue = null;
        try {
            lockValue = lockManager.tryLock("resource", resourceId, 30);
            if (lockValue != null) {
                // 业务逻辑
                performOperation(resourceId);
            } else {
                log.warn("获取锁失败: {}", resourceId);
                // 降级处理
                fallbackOperation(resourceId);
            }
        } catch (Exception e) {
            log.error("操作异常: {}", resourceId, e);
            throw e;
        } finally {
            if (lockValue != null) {
                try {
                    lockManager.unlock("resource", resourceId, lockValue, LockManager.LockType.REENTRANT);
                } catch (Exception e) {
                    log.error("释放锁异常: {}", resourceId, e);
                }
            }
        }
    }
}
```

### 4. 性能优化

```java
@Service
public class OptimizedLockService {
    
    @Autowired
    private LockManager lockManager;
    
    /**
     * 使用读写锁优化读多写少场景
     */
    public String optimizedRead(String dataId) {
        // 读操作使用读锁，支持并发
        return lockManager.executeWithReadLock("data", dataId, () -> {
            return performRead(dataId);
        });
    }
    
    /**
     * 批量操作优化
     */
    public void batchUpdate(List<String> dataIds) {
        // 按顺序处理，避免死锁
        dataIds.stream()
               .sorted() // 排序避免死锁
               .forEach(dataId -> {
                   lockManager.executeWithWriteLock("data", dataId, () -> {
                       performUpdate(dataId);
                       return null;
                   });
               });
    }
}
```

## 故障排除

### 常见问题

1. **锁获取失败**
   ```java
   // 检查锁是否被其他线程持有
   String lockValue = lockManager.tryLock("resource", "key", 30);
   if (lockValue == null) {
       log.warn("锁获取失败，可能被其他线程持有");
       // 实现重试逻辑或降级处理
   }
   ```

2. **死锁问题**
   ```java
   // 启用分布式死锁检测
   if (lockManager.isDistributedDeadlockEnabled()) {
       List<Set<String>> cycles = lockManager.detectGlobalDeadlocks();
       if (!cycles.isEmpty()) {
           log.error("检测到死锁: {}", cycles);
       }
   }
   ```

3. **性能问题**
   ```java
   // 监控锁性能
   Map<String, Object> stats = lockManager.getPerformanceStatistics();
   log.info("锁性能统计: {}", stats);
   ```

### 调试技巧

```yaml
# 启用详细日志
logging:
  level:
    com.indigo.cache.extension.lock.LockManager: DEBUG
    com.indigo.cache.extension.lock.DistributedDeadlockDetector: DEBUG
```

## 配置参考

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
      deadlock:
        distributed:
          enabled: true
          sync-interval: 5000
          global-detection-interval: 10000
          node-timeout: 30000
          debug: false
```

## 版本历史

- **v1.0.0**: 初始版本，基础锁管理功能
- **v1.1.0**: 添加读写锁和公平锁支持
- **v1.2.0**: 集成性能监控功能
- **v1.3.0**: 添加分布式死锁检测
- **v1.4.0**: 优化API设计和便捷方法
