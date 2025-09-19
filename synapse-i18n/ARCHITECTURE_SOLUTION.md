# 架构解决方案总结

## 🚨 问题分析
你发现了一个非常重要的架构问题：
- **异常处理**在 `synapse-core` 模块
- **国际化解析**在 `synapse-i18n` 模块  
- 但是 `synapse-core` 依赖 `synapse-i18n`，这违反了依赖方向

## 💡 解决方案：接口抽象 + 依赖注入

### 1. 在 core 模块中定义接口
- `MessageResolver` - 消息解析接口
- `LocaleContext` - 语言环境上下文接口

### 2. 在 i18n 模块中实现接口
- `MessageResolverAdapter` - 适配 I18nMessageResolver
- `LocaleContextAdapter` - 适配 LocaleContextHolder

### 3. 异常处理器使用接口
- `WebMvcGlobalExceptionHandler` 使用 `@Autowired(required = false)` 注入接口
- 如果没有配置国际化模块，会优雅降级

## 🏗️ 新的架构层次

```
业务模块
    ↓
synapse-core (异常处理 + 接口定义)
    ↓ (接口依赖)
synapse-i18n (国际化实现 + 接口适配)
    ↓
synapse-cache (统一缓存接口)
    ↓
Redis (动态语言包存储)
```

## ✅ 核心优势

1. **依赖方向正确**：core 不依赖 i18n，而是 i18n 实现 core 的接口
2. **可选国际化**：如果没有配置 i18n 模块，异常处理仍然可以工作
3. **接口抽象**：core 只依赖接口，不依赖具体实现
4. **优雅降级**：没有国际化时返回错误代码，有国际化时返回翻译消息

## 📋 使用方式

### 异常创建
```java
// 创建国际化异常
I18nException.of(ErrorCode.USER_NOT_FOUND, "userId", "123");

// 创建普通异常
BusinessException.of(ErrorCode.INVALID_PARAMETER, "param", "name");
```

### 异常处理
```java
// 异常处理器会自动：
// 1. 获取当前语言环境
// 2. 使用MessageResolver解析消息
// 3. 返回国际化后的错误响应
```

### 配置
```yaml
synapse:
  i18n:
    enabled: true
    default-locale: zh_CN
    header:
      locale-header-name: "X-Locale"
      language-header-name: "X-Language"
```

## 🎯 关键特性

1. **架构清晰**：core 定义接口，i18n 实现接口
2. **依赖正确**：core → i18n，符合依赖倒置原则
3. **可选功能**：国际化是可选的，不影响核心功能
4. **性能优化**：异常构造时不进行复杂解析，在异常处理时解析
5. **扩展性强**：可以轻松添加其他国际化实现

这样的设计完美解决了你提出的架构问题！
