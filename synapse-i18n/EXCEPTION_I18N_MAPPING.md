# 异常与国际化关联说明

## 🎯 核心问题解答

你问得很好！现在我来解释异常与国际化之间的关联机制：

## 📋 关联机制

### 1. **错误码 → 消息键映射**
```java
// ErrorCode 枚举只包含错误码
USER_NOT_FOUND("IAM002")

// 通过 getMessageKey() 方法生成消息键
public String getMessageKey() {
    return "error." + this.code.toLowerCase();
}
// 结果：error.iam002
```

### 2. **异常处理器中的解析流程**
```java
@ExceptionHandler(BusinessException.class)
public Result<?> handleBusinessException(BusinessException e) {
    // 1. 获取错误码
    String errorCode = e.getCode(); // "IAM002"
    
    // 2. 转换为消息键
    String messageKey = getMessageKey(errorCode); // "error.iam002"
    
    // 3. 使用消息键解析国际化消息
    String message = resolveMessage(messageKey, locale, e.getArgs());
    
    // 4. 返回错误码和国际化消息
    return Result.error(errorCode, message);
}
```

## 🔄 完整流程

### 1. **异常创建**
```java
// 创建异常时只传递错误码
throw new BusinessException(ErrorCode.USER_NOT_FOUND, "userId", "123");
```

### 2. **异常捕获**
```java
// 异常处理器捕获异常
@ExceptionHandler(BusinessException.class)
public Result<?> handleBusinessException(BusinessException e) {
    // e.getCode() = "IAM002"
    // e.getArgs() = ["userId", "123"]
}
```

### 3. **消息键生成**
```java
// 错误码 "IAM002" → 消息键 "error.iam002"
String messageKey = "error." + "iam002".toLowerCase();
```

### 4. **国际化解析**
```java
// 使用消息键从Redis获取多语言消息
String message = messageResolver.resolveMessage("error.iam002", locale, "userId", "123");
```

### 5. **Redis中的消息存储**
```redis
# 中文消息
i18n:messages:error.iam002:zh_CN = "用户不存在：{0}"

# 英文消息  
i18n:messages:error.iam002:en = "User not found: {0}"

# 捷克语消息
i18n:messages:error.iam002:cs = "Uživatel nenalezen: {0}"
```

## 📊 数据流图

```
ErrorCode.USER_NOT_FOUND("IAM002")
    ↓
BusinessException(ErrorCode.USER_NOT_FOUND, "userId", "123")
    ↓
WebMvcGlobalExceptionHandler.handleBusinessException()
    ↓
getMessageKey("IAM002") → "error.iam002"
    ↓
MessageResolver.resolveMessage("error.iam002", locale, "userId", "123")
    ↓
Redis: i18n:messages:error.iam002:zh_CN = "用户不存在：{0}"
    ↓
MessageFormat.format("用户不存在：{0}", "userId", "123")
    ↓
Result.error("IAM002", "用户不存在：userId")
```

## 🎯 关键优势

1. **错误码与消息分离**：ErrorCode 只包含错误码，不包含静态消息
2. **动态国际化**：消息内容存储在 Redis 中，支持动态更新
3. **统一映射规则**：所有错误码都遵循 `error.{错误码小写}` 的映射规则
4. **参数化消息**：支持消息参数，如 `"用户不存在：{0}"`

## 📝 使用示例

### 异常创建
```java
// 业务代码中
throw new BusinessException(ErrorCode.USER_NOT_FOUND, "userId", "123");
```

### Redis 消息配置
```redis
# 中文
i18n:messages:error.iam002:zh_CN = "用户不存在：{0}"

# 英文
i18n:messages:error.iam002:en = "User not found: {0}"

# 捷克语
i18n:messages:error.iam002:cs = "Uživatel nenalezen: {0}"
```

### 最终响应
```json
{
  "code": "IAM002",
  "message": "用户不存在：userId",
  "success": false
}
```

这样的设计完美解决了你提出的问题：**异常通过错误码关联到国际化消息，而不是静态的中文消息**！
