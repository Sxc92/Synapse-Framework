# Synapse Cache 模块

## 📋 概述

Synapse Cache 是一个高性能的缓存和分布式锁解决方案，提供了完整的缓存注解支持、多级缓存策略和分布式锁功能。该模块采用延迟初始化和自动释放机制，确保资源的高效利用。

## 🚀 核心功能

### 1. 缓存注解系统
- **@Cacheable**: 方法结果缓存
- **@CachePut**: 缓存数据更新
- **@CacheEvict**: 缓存数据删除
- **@Caching**: 组合缓存操作

### 2. 分布式锁服务
- **延迟初始化**: 按需分配资源
- **自动释放**: 智能资源管理
- **性能监控**: 实时性能指标
- **死锁检测**: 自动死锁识别

### 3. 多级缓存策略
- **LOCAL_ONLY**: 仅本地缓存
- **REDIS_ONLY**: 仅Redis缓存
- **LOCAL_AND_REDIS**: 本地+Redis缓存
- **REDIS_SYNC_TO_LOCAL**: Redis同步到本地

## 📚 文档索引

### 使用指南
- [缓存注解使用指南](CACHE_ANNOTATIONS_USAGE.md) - 详细的缓存注解使用方法
- [分布式锁优化文档](DISTRIBUTED_LOCK_OPTIMIZATION.md) - 分布式锁功能详解

### 技术文档
- [优化工作总结](OPTIMIZATION_SUMMARY.md) - 模块优化历程和成果总结

## 🔧 快速开始

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
    lock:
      auto-release:
        enabled: true
        threshold: 300000      # 5分钟
        check-interval: 60000  # 1分钟
      monitor:
        enabled: true
        granularity: 1000      # 1秒
```

### 3. 使用缓存注解

```java
@Service
public class UserService {
    
    @Cacheable(key = "user:#id", expireSeconds = 3600)
    public User getUserById(Long id) {
        return userRepository.findById(id);
    }
}
```

### 4. 使用分布式锁

```java
@Autowired
private LockManager lockManager;

String lockValue = lockManager.tryLock("resource", "key", 10);
try {
    // 执行业务逻辑
} finally {
    lockManager.releaseLock("resource", "key", lockValue);
}
```

## 🎯 主要特性

### 性能优化
- **延迟初始化**: 启动时不占用资源
- **自动释放**: 空闲时自动回收资源
- **智能缓存**: 多级缓存策略优化

### 运维友好
- **配置灵活**: 支持多种配置选项
- **监控完善**: 详细的性能指标
- **日志优化**: 合理的日志级别

### 开发便利
- **注解驱动**: 简单的注解使用方式
- **API简洁**: 清晰的接口设计
- **异常处理**: 完善的异常处理机制

## 🔍 配置选项

### 分布式锁配置
```yaml
synapse:
  cache:
    lock:
      auto-release:
        enabled: true          # 启用自动释放
        threshold: 300000      # 释放阈值（毫秒）
        check-interval: 60000  # 检查间隔（毫秒）
      monitor:
        enabled: true          # 启用监控
        granularity: 1000      # 监控粒度（毫秒）
        deadlock-detection: true  # 死锁检测
```

### 缓存策略配置
```yaml
synapse:
  cache:
    strategy:
      default: LOCAL_AND_REDIS  # 默认缓存策略
      local:
        max-size: 10000         # 本地缓存最大大小
        expire-seconds: 1800    # 本地缓存过期时间
      redis:
        expire-seconds: 3600    # Redis缓存过期时间
```

## 🧪 测试

运行测试用例验证功能：

```bash
# 运行所有测试
mvn test

# 运行特定测试
mvn test -Dtest=CacheAnnotationTest
mvn test -Dtest=LockManagerTest
```

## 📊 性能指标

### 缓存性能
- **本地缓存**: 纳秒级响应
- **Redis缓存**: 毫秒级响应
- **命中率**: 支持实时监控

### 分布式锁性能
- **获取锁**: 平均 < 10ms
- **释放锁**: 平均 < 5ms
- **死锁检测**: 5秒内识别

## 🚨 注意事项

### 1. 缓存使用
- 合理设置过期时间
- 避免缓存雪崩
- 注意缓存一致性

### 2. 分布式锁
- 设置合理的超时时间
- 确保锁的释放
- 避免死锁情况

### 3. 配置建议
- 生产环境使用较长的阈值
- 开发环境使用较短的检查间隔
- 根据业务需求调整缓存策略

## 🤝 贡献

欢迎提交 Issue 和 Pull Request 来改进 Synapse Cache 模块。

## 📄 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。 