# Synapse Core - 异常处理模块

## 📖 模块概述

异常处理模块是 Synapse Core 的核心组件之一，提供了统一的异常处理体系，支持动态异常创建、自动国际化、框架级异常捕获等功能。该模块通过反射机制实现了高度的灵活性和扩展性。

## 🎯 核心功能

### 1. 统一异常体系
- **BaseException**：所有自定义异常的基类，提供统一的异常结构和错误码支持
- **BusinessException**：业务逻辑异常
- **IAMException**：身份认证和权限异常
- **I18nException**：国际化异常，支持多语言
- **SecurityException**：安全相关异常
- **AssertException**：断言异常，用于参数校验和业务断言
- **RateLimitException**：限流异常

### 2. 动态异常创建
- **反射机制**：通过异常类和错误代码动态创建异常
- **自动国际化**：I18nException自动使用当前语言环境
- **构造函数缓存**：提高异常创建性能
- **类型安全**：泛型支持，确保编译时类型检查

### 3. 异常工具类
- **ExceptionUtils**：提供便捷的异常创建和抛出方法
- **条件异常抛出**：支持条件判断后抛出异常
- **空值检查**：支持空值检查并抛出指定异常
- **异常包装**：支持异常链和原因传递

### 4. 框架级异常处理
- **全局异常处理器**：自动捕获和处理各种异常
- **异常转换**：将异常转换为统一的错误响应
- **异常日志**：结构化的异常日志记录
- **HTTP状态码映射**：异常到HTTP状态码的自动映射

## 🏗️ 架构设计

### 模块结构
```
exception/
├── BaseException.java           # 异常基类
├── BusinessException.java       # 业务异常
├── IAMException.java            # IAM异常
├── I18nException.java          # 国际化异常
├── SecurityException.java       # 安全异常
├── AssertException.java         # 断言异常
├── RateLimitException.java      # 限流异常
├── enums/
│   ├── ErrorCode.java          # 错误代码枚举
│   └── BaseErrCode.java        # 基础错误代码
├── creator/
│   ├── ExceptionCreator.java   # 异常创建器接口
│   └── ReflectionExceptionCreator.java # 反射异常创建器
└── handler/
    ├── GlobalExceptionHandler.java    # 全局异常处理器
    └── WebMvcGlobalExceptionHandler.java # WebMVC异常处理器
```

### 核心接口

#### ExceptionCreator
```java
public interface ExceptionCreator {
    <T extends Exception> T createException(Class<T> exceptionClass, ErrorCode errorCode, Object... args) throws Exception;
    <T extends Exception> T createException(Class<T> exceptionClass, ErrorCode errorCode, Throwable cause, Object... args) throws Exception;
}
```

#### ReflectionExceptionCreator
- 基于反射的异常创建器实现
- 支持构造函数缓存
- 自动检测I18nException类型
- 自动使用当前语言环境

## 🚀 快速开始

### 1. 基本异常创建

```java
import com.indigo.core.exception.enums.ErrorCode;
import com.indigo.core.exception.BusinessException;
import com.indigo.core.exception.IAMException;
import com.indigo.core.exception.I18nException;

// 创建业务异常
BusinessException businessEx = ExceptionUtils.createException(
    BusinessException.class, 
    ErrorCode.BUSINESS_ERROR, 
    "用户ID", "12345"
);

// 创建IAM异常
IAMException iamEx = ExceptionUtils.createException(
    IAMException.class, 
    ErrorCode.USER_NOT_FOUND, 
    "用户名", "admin"
);

// 创建I18n异常（自动使用当前语言环境）
I18nException i18nEx = ExceptionUtils.createException(
    I18nException.class, 
    ErrorCode.PERMISSION_DENIED, 
    "功能", "用户管理"
);
```

### 2. 带原因的异常创建

```java
try {
    // 一些可能抛出异常的操作
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

### 3. 条件异常抛出

```java
// 如果条件为真，抛出指定类型的异常
ExceptionUtils.throwIf(
    user == null, 
    BusinessException.class, 
    ErrorCode.USER_NOT_FOUND, 
    "用户ID", userId
);

// 带原因的条件异常抛出
ExceptionUtils.throwIf(
    connection == null, 
    IAMException.class, 
    ErrorCode.SYSTEM_ERROR, 
    connectionException, 
    "数据库连接失败"
);
```

### 4. 空值检查

```java
// 如果对象为null，抛出指定类型的异常
User user = ExceptionUtils.requireNonNull(
    userService.findById(userId), 
    BusinessException.class, 
    ErrorCode.USER_NOT_FOUND, 
    "用户ID", userId
);
```

## 🔧 高级功能

### 1. 自定义异常类型

```java
// 自定义异常类
public class CustomBusinessException extends BusinessException {
    public CustomBusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }
    
    public CustomBusinessException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, cause, args);
    }
}

// 使用自定义异常
CustomBusinessException customEx = ExceptionUtils.createException(
    CustomBusinessException.class, 
    ErrorCode.BUSINESS_ERROR, 
    "自定义业务错误"
);
```

### 2. 自定义异常创建器

```java
public class CustomExceptionCreator implements ExceptionCreator {
    @Override
    public <T extends Exception> T createException(Class<T> exceptionClass, ErrorCode errorCode, Object... args) throws Exception {
        // 自定义创建逻辑
        return exceptionClass.getConstructor(ErrorCode.class, Object[].class)
                           .newInstance(errorCode, args);
    }
    
    @Override
    public <T extends Exception> T createException(Class<T> exceptionClass, ErrorCode errorCode, Throwable cause, Object... args) throws Exception {
        // 自定义创建逻辑
        return exceptionClass.getConstructor(ErrorCode.class, Throwable.class, Object[].class)
                           .newInstance(errorCode, cause, args);
    }
}

// 设置自定义创建器
ExceptionUtils.setExceptionCreator(new CustomExceptionCreator());
```

### 3. 自定义异常处理器

```java
@Component
public class CustomExceptionHandler {
    
    @ExceptionHandler(CustomBusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleCustomBusinessException(CustomBusinessException e) {
        log.warn("CustomBusinessException: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
}
```

## 📊 错误代码体系

### 错误代码分类

```java
public enum ErrorCode {
    // 系统级错误码 (SYS)
    SYSTEM_ERROR("SYS001", "系统内部错误"),
    PARAM_ERROR("SYS002", "参数错误"),
    UNAUTHORIZED("SYS003", "未授权"),
    FORBIDDEN("SYS004", "禁止访问"),
    NOT_FOUND("SYS005", "资源不存在"),
    
    // 业务级错误码 (BUS)
    BUSINESS_ERROR("BUS001", "业务处理错误"),
    DATA_NOT_FOUND("BUS002", "数据不存在"),
    DATA_ALREADY_EXISTS("BUS003", "数据已存在"),
    DATA_INVALID("BUS004", "数据无效"),
    
    // 安全认证相关错误码 (SEC)
    SECURITY_ERROR("SEC001", "安全错误"),
    NOT_LOGIN("SEC002", "未登录"),
    LOGIN_FAILED("SEC003", "登录失败"),
    TOKEN_INVALID("SEC005", "令牌无效"),
    PERMISSION_DENIED("SEC008", "权限不足"),
    
    // IAM相关错误码 (IAM)
    IAM_ERROR("IAM001", "身份认证错误"),
    USER_NOT_FOUND("IAM002", "用户不存在"),
    USER_ALREADY_EXISTS("IAM003", "用户已存在"),
    USER_INVALID("IAM004", "用户无效"),
    
    // 工作流相关错误码 (WF)
    WORKFLOW_ERROR("WF001", "工作流错误"),
    WORKFLOW_NOT_FOUND("WF002", "工作流不存在"),
    
    // 审计相关错误码 (AUD)
    AUDIT_ERROR("AUD001", "审计错误"),
    AUDIT_NOT_FOUND("AUD002", "审计记录不存在");
}
```

## 🌐 国际化支持

### 1. I18nException自动国际化

```java
// I18nException会自动使用当前语言环境
I18nException i18nEx = ExceptionUtils.createException(
    I18nException.class, 
    ErrorCode.PERMISSION_DENIED, 
    "功能", "用户管理"
);

// 异常消息会根据当前语言环境自动切换
// 中文环境：权限不足：缺少功能权限 - 功能: 用户管理
// 英文环境：Permission denied: missing function permission - function: user management
```

### 2. 语言环境获取策略

```java
// 反射异常创建器中的语言环境获取逻辑
private Locale getCurrentLocale() {
    // 优先从ThreadLocal获取（如果有国际化上下文）
    Locale locale = getLocaleFromContext();
    if (locale != null) {
        return locale;
    }
    
    // 从系统默认语言环境获取
    return Locale.getDefault();
}
```

### 3. 自定义语言环境获取

```java
// 可以根据实际的国际化框架进行调整
private Locale getLocaleFromContext() {
    // 示例：从ThreadLocal获取
    // return LocaleContextHolder.getLocale();
    
    // 示例：从RequestContext获取
    // return RequestContextHolder.getRequestAttributes() != null ? 
    //     ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getLocale() : null;
    
    // 暂时返回null，使用系统默认
    return null;
}
```

## 🔒 安全特性

### 1. 异常信息安全
- **敏感信息过滤**：异常消息中不包含敏感信息
- **错误码标准化**：统一的错误码体系，避免信息泄露
- **异常链保护**：异常链中的敏感信息自动脱敏

### 2. 异常访问控制
- **权限验证**：异常创建和处理的权限控制
- **审计日志**：异常操作的审计日志记录
- **访问限制**：异常信息的访问限制

## 📝 最佳实践

### 1. 异常类型选择
- **BusinessException**：业务逻辑错误
- **IAMException**：系统级错误
- **I18nException**：需要国际化的错误
- **SecurityException**：安全相关错误
- **AssertException**：断言失败

### 2. 错误代码使用
```java
// 推荐：使用预定义的错误代码
ExceptionUtils.throwIf(condition, BusinessException.class, ErrorCode.BUSINESS_ERROR);

// 避免：硬编码错误代码
ExceptionUtils.throwIf(condition, BusinessException.class, "CUSTOM001");
```

### 3. 异常链保持
```java
// 保持异常链，便于问题追踪
try {
    riskyOperation();
} catch (Exception e) {
    ExceptionUtils.throwIf(true, BusinessException.class, ErrorCode.OPERATION_FAILED, e, "操作失败");
}
```

### 4. 性能考虑
```java
// 反射创建器会缓存构造函数，重复创建相同类型的异常性能很好
// 但首次创建会有反射开销，建议在应用启动时预热

// 预热示例
ExceptionUtils.createException(BusinessException.class, ErrorCode.BUSINESS_ERROR);
ExceptionUtils.createException(IAMException.class, ErrorCode.SYSTEM_ERROR);
```

## 🐛 常见问题

### 1. 异常创建失败
**问题**：反射创建异常时失败
**解决方案**：
- 确保异常类有正确的构造函数
- 检查异常类是否继承自BaseException
- 验证ErrorCode是否正确

### 2. 国际化异常不生效
**问题**：I18nException没有使用正确的语言环境
**解决方案**：
- 检查语言环境获取逻辑
- 验证MessageUtils配置
- 确认语言包是否完整

### 3. 异常处理器不生效
**问题**：自定义异常处理器没有被调用
**解决方案**：
- 检查@ExceptionHandler注解
- 验证异常类型匹配
- 确认处理器是否被Spring管理

### 4. 性能问题
**问题**：异常创建性能不佳
**解决方案**：
- 使用构造函数缓存
- 避免频繁创建异常
- 考虑异常对象池

## 📚 相关文档

- [Synapse Core 模块文档](../README.md)
- [Synapse Framework 架构设计](../../ARCHITECTURE.md)
- [Synapse Framework 使用指南](../../QUICKSTART.md)
- [Synapse Framework 配置参考](../../CONFIGURATION.md)

## 🔗 相关链接

- [Spring Boot 异常处理](https://spring.io/guides/gs/rest-service/)
- [Java 反射机制](https://docs.oracle.com/javase/tutorial/reflect/)
- [Java 国际化指南](https://docs.oracle.com/javase/tutorial/i18n/)
- [Spring 异常处理最佳实践](https://spring.io/guides/gs/rest-service/)

---

*最后更新时间：2025年01月27日*
