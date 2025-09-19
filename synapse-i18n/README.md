# Synapse I18n Module

## 概述

Synapse I18n 模块提供统一的国际化消息解析能力，支持从 Redis 动态获取多语言消息，为 Synapse 框架提供国际化支持。

## 特性

- 🌍 **多语言支持**：支持动态添加新语言（如捷克语、法语等）
- ⚡ **高性能**：基于 Redis 缓存，毫秒级响应
- 🔧 **易集成**：Spring Boot 自动配置，开箱即用
- 📦 **模块化**：独立模块，职责清晰
- 🎯 **统一接口**：提供统一的 I18nMessageResolver 接口

## 架构设计

```
synapse-core -> synapse-i18n -> synapse-cache -> Redis
```

### 模块职责

- **synapse-i18n**：提供国际化消息解析接口
- **synapse-core**：异常处理逻辑，调用 i18n 模块获取消息
- **meta-data-service**：管理数据库中的国际化消息，同步到 Redis

## 核心组件

### 1. I18nMessageResolver

国际化消息解析器接口，提供统一的消息解析能力：

```java
public interface I18nMessageResolver {
    String resolveMessage(String code, Locale locale, Object... args);
    String resolveMessage(String code, Object... args);
    boolean isLocaleSupported(Locale locale);
    List<Locale> getSupportedLocales();
}
```

### 2. RedisI18nMessageResolver

基于 Redis 的消息解析器实现：

- 从 Redis 获取消息模板
- 支持消息参数格式化
- 提供语言支持检查
- 异常处理和降级策略

### 3. I18nCache

Redis 缓存管理：

- 管理消息模板缓存
- 管理支持的语言列表
- 提供 TTL 支持
- 异常处理和日志记录

## 使用方式

### 1. 依赖配置

在 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>com.indigo</groupId>
    <artifactId>synapse-i18n</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置文件

```yaml
synapse:
  i18n:
    enabled: true
    cache:
      enabled: true
      ttl: 86400  # 24小时
      key-prefix: "i18n:messages:"
    fallback:
      enabled: true
      default-locale: zh_CN
      fallback-locale: en
```

### 3. 业务代码使用

```java
@Service
public class UserService {
    
    @Autowired
    private I18nMessageResolver i18nMessageResolver;
    
    public User getUserById(Long userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            // 抛出业务异常，自动从Redis获取国际化消息
            throw new BusinessException(ErrorCode.IAM002, userId);
        }
        return user;
    }
}
```

### 4. 异常处理器集成

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @Autowired
    private I18nMessageResolver i18nMessageResolver;
    
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        Locale locale = LocaleContextHolder.getCurrentLocale();
        String message = i18nMessageResolver.resolveMessage(e.getCode(), locale, e.getArgs());
        return Result.error(e.getCode(), message);
    }
}
```

## Redis 数据结构

### 消息模板存储

```
Key: i18n:messages:{errorCode}:{locale}
Value: {messageTemplate}
TTL: 24小时

示例：
i18n:messages:IAM002:zh_CN -> "用户不存在"
i18n:messages:IAM002:en -> "User not found"
i18n:messages:IAM002:cs -> "Uživatel nenalezen"
```

### 支持语言列表

```
Key: i18n:locales
Value: zh_CN,en,cs,fr
TTL: 24小时
```

## Meta 服务集成

Meta 服务负责管理数据库中的国际化消息，并同步到 Redis：

```java
@Service
public class I18nRedisSyncService {
    
    public void syncAllMessagesToRedis() {
        // 1. 从数据库获取所有消息
        List<ExceptionMessage> messages = exceptionMessageMapper.findAll();
        
        // 2. 同步到Redis
        for (ExceptionMessage message : messages) {
            String redisKey = buildMessageKey(message.getErrorCode(), message.getLocale());
            redisTemplate.opsForValue().set(redisKey, message.getMessageTemplate(), CACHE_TTL, TimeUnit.SECONDS);
        }
        
        // 3. 更新支持的语言列表
        updateSupportedLocales();
    }
}
```

## 异常响应示例

### 中文请求
```json
{
  "code": "IAM002",
  "message": "用户不存在",
  "data": null,
  "timestamp": "2025-01-27T10:30:00Z"
}
```

### 英文请求
```json
{
  "code": "IAM002", 
  "message": "User not found",
  "data": null,
  "timestamp": "2025-01-27T10:30:00Z"
}
```

### 捷克语请求
```json
{
  "code": "IAM002",
  "message": "Uživatel nenalezen", 
  "data": null,
  "timestamp": "2025-01-27T10:30:00Z"
}
```

## 优势

1. **架构清晰**：synapse-i18n 独立模块，职责明确
2. **层次合理**：core -> i18n -> cache -> Redis，符合架构层次
3. **性能优秀**：Core 模块直接从 Redis 获取消息，无远程调用
4. **动态扩展**：支持运行时添加新语言（如捷克语）
5. **易维护**：模块化设计，便于维护和扩展

## 注意事项

1. 确保 Redis 服务正常运行
2. Meta 服务需要定期同步数据到 Redis
3. 新增语言时需要在数据库中配置消息模板
4. 建议设置合适的 TTL 避免内存占用过多
