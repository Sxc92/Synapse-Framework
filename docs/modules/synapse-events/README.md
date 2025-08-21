# Synapse Events 模块

## 📖 模块概述

Synapse Events 是 Synapse Framework 的事件驱动模块，提供了完整的事件发布、订阅和处理机制。该模块基于 Spring 的事件系统，扩展了分布式事件、事务事件、异步事件等高级功能，为应用提供松耦合、高可扩展性的事件驱动架构。

## 🎯 核心功能

### 1. 事件发布订阅
- **本地事件**：应用内的事件发布和订阅
- **分布式事件**：跨应用、跨服务的事件传播
- **事件路由**：智能的事件路由和分发机制
- **事件过滤**：基于条件的事件过滤和选择

### 2. 事务事件管理
- **事务绑定**：事件与事务的绑定关系
- **事务回滚**：事务回滚时的事件处理
- **事务传播**：跨事务边界的事件传播
- **事务监控**：事务事件的监控和统计

### 3. 异步事件处理
- **异步执行**：事件的异步处理和执行
- **线程池管理**：可配置的线程池策略
- **重试机制**：事件处理失败的重试策略
- **超时控制**：事件处理的超时控制

### 4. 事件持久化
- **事件存储**：事件的持久化存储
- **事件重放**：历史事件的重新播放
- **事件审计**：事件操作的审计日志
- **事件清理**：过期事件的自动清理

### 5. 事件监控和统计
- **性能监控**：事件处理的性能指标
- **错误统计**：事件处理错误的统计
- **延迟分析**：事件处理延迟的分析
- **吞吐量监控**：事件处理吞吐量的监控

## 🏗️ 架构设计

### 模块结构
```
synapse-events/
├── annotation/      # 事件注解
├── config/          # 配置管理
├── core/            # 核心功能
├── transaction/     # 事务事件
└── utils/           # 工具类
```

### 核心组件
- **EventPublisher**：事件发布器
- **EventSubscriber**：事件订阅器
- **EventDispatcher**：事件分发器
- **EventStore**：事件存储
- **EventMonitor**：事件监控器

### 事件流程
```
事件发布 → 事件路由 → 事件分发 → 事件处理 → 结果回调
    ↓           ↓         ↓         ↓         ↓
  发布器    路由策略   分发策略   处理器    回调处理
```

## 🚀 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.indigo</groupId>
    <artifactId>synapse-events</artifactId>
    <version>${synapse.version}</version>
</dependency>
```

### 2. 基础配置

```yaml
# application.yml
synapse:
  events:
    # 事件配置
    enabled: true
    
    # 异步处理配置
    async:
      enabled: true
      core-pool-size: 5
      max-pool-size: 20
      queue-capacity: 100
      keep-alive-seconds: 60
    
    # 分布式事件配置
    distributed:
      enabled: false
      broker-url: tcp://localhost:61616
      topic-prefix: synapse.events
    
    # 事件存储配置
    storage:
      enabled: true
      type: memory  # memory, redis, database
      retention-days: 30
```

### 3. 使用示例

#### 基础事件发布订阅
```java
// 定义事件
public class UserCreatedEvent extends BaseEvent {
    private Long userId;
    private String username;
    
    public UserCreatedEvent(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }
    
    // getters and setters
}

// 发布事件
@Service
public class UserService {
    
    @Autowired
    private EventPublisher eventPublisher;
    
    public void createUser(User user) {
        // 创建用户
        userRepository.save(user);
        
        // 发布用户创建事件
        UserCreatedEvent event = new UserCreatedEvent(user.getId(), user.getUsername());
        eventPublisher.publish(event);
    }
}

// 订阅事件
@Component
public class UserEventHandler {
    
    @EventListener
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("用户创建事件: userId={}, username={}", 
                event.getUserId(), event.getUsername());
        
        // 处理用户创建后的业务逻辑
        sendWelcomeEmail(event.getUserId());
        createUserProfile(event.getUserId());
    }
}
```

#### 异步事件处理
```java
@Component
public class AsyncEventHandler {
    
    @AsyncEventListener
    public void handleAsyncEvent(AsyncEvent event) {
        // 异步处理事件
        log.info("异步处理事件: {}", event.getEventType());
        
        // 模拟耗时操作
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        log.info("异步事件处理完成: {}", event.getEventType());
    }
}
```

#### 事务事件
```java
@Service
@Transactional
public class OrderService {
    
    @Autowired
    private EventPublisher eventPublisher;
    
    public void createOrder(Order order) {
        // 创建订单
        orderRepository.save(order);
        
        // 发布事务事件（事务提交后才会发布）
        OrderCreatedEvent event = new OrderCreatedEvent(order.getId());
        eventPublisher.publishAfterCommit(event);
        
        // 发布事务事件（事务提交前发布）
        OrderProcessingEvent processingEvent = new OrderProcessingEvent(order.getId());
        eventPublisher.publishBeforeCommit(processingEvent);
    }
}
```

#### 条件事件订阅
```java
@Component
public class ConditionalEventHandler {
    
    @EventListener(condition = "#event.amount > 1000")
    public void handleLargeOrder(OrderCreatedEvent event) {
        log.info("处理大额订单: orderId={}, amount={}", 
                event.getOrderId(), event.getAmount());
        
        // 大额订单的特殊处理逻辑
        notifyManager(event.getOrderId());
        applyRiskCheck(event.getOrderId());
    }
    
    @EventListener(condition = "#event.userType == 'VIP'")
    public void handleVipUser(UserCreatedEvent event) {
        log.info("处理VIP用户: userId={}", event.getUserId());
        
        // VIP用户的特殊处理
        assignVipBenefits(event.getUserId());
    }
}
```

## 🔧 高级功能

### 1. 自定义事件分发器
```java
@Component
public class CustomEventDispatcher implements EventDispatcher {
    
    @Override
    public void dispatch(Event event, List<EventListener> listeners) {
        // 自定义事件分发逻辑
        for (EventListener listener : listeners) {
            if (canHandle(listener, event)) {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                    handleDispatchError(event, listener, e);
                }
            }
        }
    }
    
    private boolean canHandle(EventListener listener, Event event) {
        // 自定义处理条件判断
        return listener.getEventType().isAssignableFrom(event.getClass());
    }
}
```

### 2. 事件重试机制
```java
@Component
public class RetryableEventHandler {
    
    @EventListener
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void handleWithRetry(RetryableEvent event) {
        log.info("处理可重试事件: {}", event.getEventType());
        
        // 可能失败的业务逻辑
        processEvent(event);
    }
    
    @Recover
    public void recover(RetryableEvent event, Exception e) {
        log.error("事件处理最终失败: {}", event.getEventType(), e);
        
        // 失败后的恢复处理
        sendFailureNotification(event);
        storeFailedEvent(event);
    }
}
```

### 3. 事件监控和统计
```java
@Component
public class EventMonitor {
    
    @EventListener
    public void monitorEvent(Event event) {
        // 记录事件统计信息
        EventStatistics stats = EventStatistics.getInstance();
        stats.recordEvent(event.getClass().getSimpleName());
        
        // 记录事件处理时间
        long startTime = System.currentTimeMillis();
        
        // 事件处理完成后记录耗时
        stats.recordProcessingTime(event.getClass().getSimpleName(), 
                System.currentTimeMillis() - startTime);
    }
}
```

## 📊 性能特性

### 1. 事件缓存
- **事件缓存**：常用事件的缓存机制
- **订阅者缓存**：事件订阅者的缓存
- **路由缓存**：事件路由规则的缓存

### 2. 异步优化
- **线程池优化**：可配置的线程池策略
- **批量处理**：事件的批量处理机制
- **优先级队列**：基于优先级的事件队列

### 3. 内存优化
- **事件池化**：事件对象的对象池
- **内存监控**：事件处理的内存监控
- **垃圾回收**：自动的内存清理机制

## 🔒 安全特性

### 1. 事件安全
- **权限验证**：事件发布的权限控制
- **数据脱敏**：敏感事件的自动脱敏
- **访问控制**：事件订阅的访问控制

### 2. 审计日志
- **操作审计**：事件操作的完整审计
- **变更追踪**：事件数据的变更追踪
- **合规检查**：事件处理的合规性检查

## 📝 最佳实践

### 1. 事件设计
- 使用有意义的命名约定
- 保持事件的不可变性
- 包含足够的上下文信息
- 避免在事件中包含敏感数据

### 2. 事件处理
- 保持事件处理器的轻量级
- 使用异步处理处理耗时操作
- 实现幂等性的事件处理
- 提供适当的错误处理机制

### 3. 性能优化
- 合理配置线程池参数
- 使用事件过滤减少不必要的处理
- 监控事件处理的性能指标
- 定期清理过期的事件数据

### 4. 监控和调试
- 实现完整的事件监控
- 记录详细的事件处理日志
- 提供事件处理的性能分析
- 实现事件处理的健康检查

## 🐛 常见问题

### 1. 事件丢失
**问题**：某些事件没有被处理或丢失
**解决方案**：
- 检查事件订阅者的注册
- 验证事件类型匹配
- 检查事件过滤条件
- 确认事件发布时机

### 2. 事件处理超时
**问题**：事件处理时间过长导致超时
**解决方案**：
- 使用异步事件处理
- 优化事件处理逻辑
- 调整线程池配置
- 实现超时控制机制

### 3. 内存泄漏
**问题**：事件处理导致内存使用持续增长
**解决方案**：
- 及时清理事件数据
- 监控内存使用情况
- 实现事件数据的生命周期管理
- 定期进行垃圾回收

## 📚 相关文档

- [Synapse Framework 架构设计](../../ARCHITECTURE.md)
- [Synapse Framework 使用指南](../../USAGE_GUIDE.md)
- [Synapse Framework 架构设计](../../ARCHITECTURE.md)

## 🔗 相关链接

- [Spring 事件机制](https://spring.io/projects/spring-framework)
- [事件驱动架构](https://martinfowler.com/articles/201701-event-driven.html)
- [异步事件处理](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#context-functionality-events)

---

*最后更新时间：2025年08月11日 12:41:56* 