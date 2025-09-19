# 异常处理模块整理总结

## 📋 整理内容

### ✅ 已删除的异常类
- `JsonException.java` - JSON处理异常（过于具体，不通用）
- `DateTimeException.java` - 日期时间异常（过于具体，不通用）
- `TreeException.java` - 树结构异常（过于具体，不通用）
- `ThreadException.java` - 线程异常（过于具体，不通用）
- `MapException.java` - Map异常（过于具体，不通用）
- `JwtAuthenticationException.java` - JWT认证异常（过于具体，不通用）
- `SpringException.java` - Spring异常（过于具体，不通用）
- `GatewayException.java` - 网关异常（过于具体，不通用）

### ✅ 重构的异常类

#### 1. BaseException（异常基类）
- **简化设计**：统一使用ErrorCode，移除复杂的构造函数
- **统一结构**：所有异常都继承自BaseException
- **标准接口**：提供4个标准构造函数（基本、带原因、自定义消息、自定义消息+原因）

#### 2. 核心异常类
- **BusinessException**：业务逻辑异常
- **IAMException**：身份认证和权限异常
- **I18nException**：国际化异常，支持多语言
- **SecurityException**：安全相关异常
- **AssertException**：断言异常，用于参数校验和业务断言
- **RateLimitException**：限流异常

## 🎯 设计原则

### 1. 统一性
- 所有异常类都继承自BaseException
- 统一的构造函数签名
- 统一的错误码支持

### 2. 简洁性
- 移除不必要的异常类
- 简化构造函数设计
- 减少代码重复

### 3. 扩展性
- 支持动态异常创建
- 支持自定义异常类型
- 支持自动国际化

## 📝 使用示例

### 基本异常创建
```java
// 业务异常
BusinessException ex = ExceptionUtils.createException(
    BusinessException.class, 
    ErrorCode.BUSINESS_ERROR, 
    "用户ID", "12345"
);

// IAM异常
IAMException ex = ExceptionUtils.createException(
    IAMException.class, 
    ErrorCode.USER_NOT_FOUND, 
    "用户名", "admin"
);

// 安全异常
SecurityException ex = ExceptionUtils.createException(
    SecurityException.class, 
    ErrorCode.SECURITY_ERROR, 
    "权限不足"
);

// 断言异常
AssertException ex = ExceptionUtils.createException(
    AssertException.class, 
    ErrorCode.PARAM_ERROR, 
    "参数", "userId"
);

// 限流异常
RateLimitException ex = ExceptionUtils.createException(
    RateLimitException.class, 
    ErrorCode.RATE_LIMIT_EXCEEDED, 
    "请求频率", "100/分钟"
);
```

### 带原因的异常创建
```java
try {
    // 业务逻辑
    someRiskyOperation();
} catch (Exception e) {
    // 包装原始异常
    BusinessException wrappedEx = ExceptionUtils.createException(
        BusinessException.class, 
        ErrorCode.OPERATION_FAILED, 
        e,  // 原始异常作为原因
        "操作参数", "value"
    );
    throw wrappedEx;
}
```

### 条件异常抛出
```java
// 如果条件为真，抛出指定类型的异常
ExceptionUtils.throwIf(
    user == null, 
    BusinessException.class, 
    ErrorCode.USER_NOT_FOUND, 
    "用户ID", userId
);

// 安全检查
ExceptionUtils.throwIf(
    !hasPermission, 
    SecurityException.class, 
    ErrorCode.PERMISSION_DENIED, 
    "功能", "用户管理"
);

// 参数校验
ExceptionUtils.throwIf(
    StringUtils.isBlank(userId), 
    AssertException.class, 
    ErrorCode.PARAM_ERROR, 
    "用户ID不能为空"
);
```

### 空值检查
```java
// 如果对象为null，抛出指定类型的异常
User user = ExceptionUtils.requireNonNull(
    userService.findById(userId), 
    BusinessException.class, 
    ErrorCode.USER_NOT_FOUND, 
    "用户ID", userId
);
```

## 🔧 自定义异常

### 创建自定义异常类
```java
public class CustomBusinessException extends BusinessException {
    public CustomBusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
    
    public CustomBusinessException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, cause, args);
    }
    
    public CustomBusinessException(ErrorCode errorCode, String message, Object... args) {
        super(errorCode, message, args);
    }
    
    public CustomBusinessException(ErrorCode errorCode, String message, Throwable cause, Object... args) {
        super(errorCode, message, cause, args);
    }
}
```

### 使用自定义异常
```java
// 使用自定义异常
CustomBusinessException ex = ExceptionUtils.createException(
    CustomBusinessException.class, 
    ErrorCode.BUSINESS_ERROR, 
    "自定义业务错误"
);
```

## 📊 异常类层次结构

```
BaseException (抽象基类)
├── BusinessException (业务异常)
├── IAMException (身份认证异常)
├── I18nException (国际化异常)
├── SecurityException (安全异常)
├── AssertException (断言异常)
└── RateLimitException (限流异常)
```

## 🎉 整理效果

### 1. 代码简化
- 删除了8个不必要的异常类
- 统一了所有异常类的设计
- 减少了代码重复

### 2. 设计统一
- 所有异常类都使用相同的构造函数签名
- 统一的错误码支持
- 一致的异常处理方式

### 3. 易于使用
- 通过ExceptionUtils统一创建异常
- 支持动态异常创建
- 支持自动国际化

### 4. 易于扩展
- 可以轻松添加新的异常类型
- 支持自定义异常创建器
- 支持框架级异常处理

现在异常处理模块更加简洁、统一、易用，符合框架的设计理念。
