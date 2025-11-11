# Redis Key 序列化问题修复指南

## 问题描述

在 Redis Desktop Manager 等工具中，发现 Redis 的 key 显示为 Java 序列化格式，例如：
```
\xac\xed\x00\x05t\x00\x15deadlock:global:nodes
```

其中 `\xac\xed\x00\x05` 是 Java 序列化的魔数（magic number），表示这是一个 Java 序列化的对象。

## 问题原因

根据截图分析，key 显示为 `\xac\xed\x00\x05t\x00=synapse:user:permissions:4a88e11...`，说明：

1. **序列化器配置问题**：RedisTemplate 的 key 序列化器未正确配置为 `StringRedisSerializer`
2. **Hash Key 序列化问题**：Hash 操作的 key 也可能使用了 Java 序列化器
3. **默认序列化器**：Spring Boot 的默认 RedisTemplate 使用 `JdkSerializationRedisSerializer`，会导致 key 被序列化
4. **Sa-Token 框架使用**：Sa-Token 等框架可能使用了默认的 RedisTemplate，而不是我们配置的 `@Primary` RedisTemplate
5. **对象直接作为 Key**：虽然 key 的内容是字符串（如 `synapse:user:permissions:xxx`），但如果直接传入 String 对象而不是字符串字面量，也可能被序列化

**关键发现**：
- Key 的内容本身是正确的字符串格式（`synapse:user:permissions:...`）
- 但整个 key 被 Java 序列化了（`\xac\xed\x00\x05` 是 Java 序列化魔数）
- 这说明 RedisTemplate 的 key 序列化器配置不正确，或者使用了错误的 RedisTemplate 实例

## 解决方案

### 1. 配置修复（已完成）

在 `RedisConfiguration` 中确保所有 key 都使用 `StringRedisSerializer`：

```java
// Key 序列化器
template.setKeySerializer(new StringRedisSerializer());

// Hash Key 序列化器（重要！）
template.setHashKeySerializer(new StringRedisSerializer());
```

### 2. 验证配置

启动应用后，检查日志输出，确认序列化器配置正确：

```
自定义RedisTemplate Bean 创建成功
  - Key序列化器: StringRedisSerializer
  - Value序列化器: GenericJackson2JsonRedisSerializer
  - HashKey序列化器: StringRedisSerializer
  - HashValue序列化器: GenericJackson2JsonRedisSerializer
```

### 3. 清理旧的序列化 Key

如果 Redis 中已经存在使用 Java 序列化的 key，需要清理：

#### 方法 1：使用 Redis CLI

```bash
# 连接到 Redis
redis-cli

# 查找所有序列化的 key（以 \xac\xed 开头）
KEYS *

# 删除特定的序列化 key
DEL "\xac\xed\x00\x05t\x00\x15deadlock:global:nodes"

# 或者使用模式删除
redis-cli --scan --pattern "*deadlock:global:*" | xargs redis-cli DEL
```

#### 方法 2：使用 Redis Desktop Manager

1. 在 Redis Desktop Manager 中找到序列化的 key
2. 右键点击 key，选择 "Delete"
3. 或者使用批量删除功能

#### 方法 3：编写清理脚本

```java
@Service
public class RedisKeyCleanupService {
    
    @Autowired
    private RedisService redisService;
    
    /**
     * 清理所有序列化的 key
     * 注意：此操作会删除所有以 Java 序列化格式存储的 key
     */
    public void cleanupSerializedKeys() {
        // 使用 SCAN 命令扫描所有 key
        // 检查 key 是否以 Java 序列化魔数开头
        // 如果是，则删除
        
        // 实现逻辑...
    }
}
```

## 预防措施

### 1. 确保配置优先级

`RedisConfiguration` 使用了以下注解确保配置优先级：

```java
@AutoConfigureBefore(RedisAutoConfiguration.class)  // 在 Spring Boot 自动配置之前运行
@Primary  // 标记为主要 Bean
@ConditionalOnMissingBean(name = "redisTemplate")  // 只在没有其他 RedisTemplate 时创建
```

### 2. 统一使用 RedisService

所有 Redis 操作都应该通过 `RedisService` 进行，而不是直接使用 `RedisTemplate`：

```java
// ✅ 正确：使用 RedisService
@Autowired
private RedisService redisService;

public void example() {
    redisService.hset("deadlock:global:nodes", "node1", "value1");
}

// ❌ 错误：直接使用 RedisTemplate（可能使用错误的序列化器）
@Autowired
private RedisTemplate<String, Object> redisTemplate;

public void example() {
    redisTemplate.opsForHash().put("deadlock:global:nodes", "node1", "value1");
}
```

### 3. 代码审查检查点

- ✅ 确保所有 Redis 操作都通过 `RedisService`
- ✅ 检查是否有直接使用 `RedisTemplate` 的地方
- ✅ 确认没有使用 `JdkSerializationRedisSerializer`
- ✅ 验证 Hash 操作的 key 也使用字符串格式

## 常见问题

### Q1: 修复后，旧的序列化 key 仍然存在？

**A**: 修复配置只会影响新创建的 key。旧的序列化 key 需要手动清理（见上面的清理方法）。

### Q2: 如何确认配置已生效？

**A**: 检查应用启动日志，确认序列化器配置正确。然后创建一个新的 key，在 Redis Desktop Manager 中查看是否是可读的字符串格式。

### Q3: 是否会影响现有功能？

**A**: 不会。修复后的配置：
- 新创建的 key 使用字符串格式（可读）
- 旧的序列化 key 仍然可以读取（如果序列化器兼容）
- 建议清理旧的序列化 key，避免混淆

### Q4: 分布式锁的 key 是否受影响？

**A**: 是的。分布式锁使用的 key（如 `deadlock:global:nodes`）也会受到影响。修复后，这些 key 将使用字符串格式存储。

### Q5: 为什么修复后 key 仍然被序列化？

**A**: 可能的原因：
1. **应用未重启**：修复配置后需要重启应用才能生效
2. **多个 RedisTemplate Bean**：可能有其他地方创建了 RedisTemplate，使用了默认序列化器
3. **Sa-Token 使用自定义 RedisTemplate**：Sa-Token 可能创建了自己的 RedisTemplate 实例
4. **Spring Boot 自动配置仍然生效**：虽然使用了 `@AutoConfigureBefore`，但在某些情况下可能仍然会创建默认的 RedisTemplate

**诊断方法**：
```java
// 在应用启动后，检查所有 RedisTemplate Bean
@Autowired
private ApplicationContext applicationContext;

@PostConstruct
public void checkRedisTemplates() {
    Map<String, RedisTemplate> templates = applicationContext.getBeansOfType(RedisTemplate.class);
    templates.forEach((name, template) -> {
        log.info("RedisTemplate Bean: {}, Key序列化器: {}", 
            name, template.getKeySerializer().getClass().getSimpleName());
    });
}
```

## 相关文件

- `synapse-framework/synapse-cache/src/main/java/com/indigo/cache/config/RedisConfiguration.java` - Redis 配置类
- `synapse-framework/synapse-cache/src/main/java/com/indigo/cache/infrastructure/RedisService.java` - Redis 服务类
- `synapse-framework/synapse-cache/src/main/java/com/indigo/cache/extension/lock/DistributedDeadlockDetector.java` - 分布式死锁检测器（使用 `deadlock:global:nodes` key）

## 总结

1. ✅ 已修复 `RedisConfiguration`，确保所有 key 使用 `StringRedisSerializer`
2. ✅ 添加了序列化器配置验证和日志输出
3. ⚠️ 需要清理 Redis 中已存在的序列化 key
4. ✅ 新创建的 key 将使用可读的字符串格式

修复后，所有新的 Redis key 都将以可读的字符串格式存储，方便在 Redis Desktop Manager 等工具中查看和管理。

