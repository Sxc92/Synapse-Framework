# ErrorCode 接口扩展方案

## 🎯 设计目标

将 `ErrorCode` 改为接口，支持业务模块扩展自定义错误码，而不需要修改核心包。

## 📋 架构设计

### 1. **ErrorCode 接口**
```java
public interface ErrorCode {
    String getCode();
    default String getMessageKey() {
        return "error." + getCode().toLowerCase();
    }
}
```

### 2. **StandardErrorCode 枚举**
```java
public enum StandardErrorCode implements ErrorCode {
    BASE_SUCCESS("200"),
    BASE_ERROR("SYS000"),
    USER_NOT_FOUND("IAM002"),
    // ... 其他标准错误码
}
```

### 3. **业务模块自定义错误码**
```java
public enum BusinessErrorCode implements ErrorCode {
    ORDER_NOT_FOUND("ORD001"),
    PAYMENT_FAILED("PAY001"),
    INVENTORY_INSUFFICIENT("INV001"),
    // ... 其他业务错误码
}
```

## 🔧 使用方式

### 1. **框架内置错误码**
```java
// 使用标准错误码
Ex.throwEx(StandardErrorCode.USER_NOT_FOUND, "userId", "123");
Ex.throwEx(StandardErrorCode.PERMISSION_DENIED, "resource", "user");
```

### 2. **业务模块自定义错误码**
```java
// 使用业务错误码
Ex.throwEx(BusinessErrorCode.ORDER_NOT_FOUND, "orderId", "456");
Ex.throwEx(BusinessErrorCode.PAYMENT_FAILED, "amount", "100.00");
Ex.throwEx(BusinessErrorCode.INVENTORY_INSUFFICIENT, "productId", "789");
```

### 3. **动态错误码**
```java
// 也可以创建动态错误码
ErrorCode customError = new ErrorCode() {
    @Override
    public String getCode() {
        return "CUSTOM001";
    }
};
Ex.throwEx(customError, "param", "value");
```

## 🎯 优势

### 1. **扩展性**
- ✅ 业务模块可以定义自己的错误码
- ✅ 不需要修改核心包
- ✅ 支持无限扩展

### 2. **一致性**
- ✅ 所有错误码都实现相同的接口
- ✅ 统一的错误处理机制
- ✅ 统一的国际化支持

### 3. **灵活性**
- ✅ 支持枚举、类、匿名类等多种实现方式
- ✅ 支持动态错误码
- ✅ 支持错误码分组管理

## 📊 Redis 消息存储

```redis
# 标准错误码消息
i18n:messages:error.sys000:zh_CN = "系统基础错误"
i18n:messages:error.iam002:zh_CN = "用户不存在：{0}"

# 业务错误码消息
i18n:messages:error.ord001:zh_CN = "订单不存在：{0}"
i18n:messages:error.pay001:zh_CN = "支付失败：{0}"
i18n:messages:error.inv001:zh_CN = "库存不足：{0}"
```

## 🔄 完整流程

```
业务代码
    ↓
Ex.throwEx(BusinessErrorCode.ORDER_NOT_FOUND, "orderId", "456")
    ↓
SynapseException(ErrorCode, args...)
    ↓
WebMvcGlobalExceptionHandler.handleSynapseException()
    ↓
getMessageKey("ORD001") → "error.ord001"
    ↓
MessageResolver.resolveMessage("error.ord001", locale, "orderId", "456")
    ↓
Redis: i18n:messages:error.ord001:zh_CN = "订单不存在：{0}"
    ↓
Result.error("ORD001", "订单不存在：orderId")
```

## 📝 最佳实践

### 1. **错误码命名规范**
- 系统级：`SYS001`, `SYS002`...
- 业务级：`BUS001`, `BUS002`...
- 订单相关：`ORD001`, `ORD002`...
- 支付相关：`PAY001`, `PAY002`...

### 2. **错误码分组**
```java
// 按模块分组
public enum OrderErrorCode implements ErrorCode { ... }
public enum PaymentErrorCode implements ErrorCode { ... }
public enum InventoryErrorCode implements ErrorCode { ... }
```

### 3. **国际化消息管理**
- 在 `meta-data-service` 中管理所有错误码的国际化消息
- 支持动态添加新语言
- 支持消息模板参数化

这样的设计完美解决了业务模块扩展错误码的需求！
