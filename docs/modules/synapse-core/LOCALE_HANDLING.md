# Gateway架构下的语言环境处理方案

## 📖 概述

在Spring Gateway架构中，语言环境处理方案已经简化，专注于从Gateway传递的请求头获取语言环境信息：

1. **LocaleContextHolder** - 语言环境上下文持有者（简化版）
2. **I18nConfig** - 国际化配置（移除拦截器）

## 🎯 核心功能

### 1. LocaleContextHolder（Gateway优化版）
- **Gateway优先**：优先从Gateway传递的请求头获取语言环境
- **ThreadLocal管理**：支持设置和清理语言环境
- **格式解析**：支持多种语言环境格式（zh_CN, zh-CN, zh等）
- **支持检查**：检查语言环境是否被支持

### 2. I18nConfig（简化版）
- **消息源配置**：配置国际化消息源
- **语言环境解析器**：配置Spring LocaleResolver
- **移除拦截器**：不再需要自定义拦截器

## 🚀 使用方式

### 1. 自动语言环境处理

```java
// Gateway会在请求头中传递语言环境，业务服务直接使用
I18nException ex = ExceptionUtils.createException(
    I18nException.class, 
    ErrorCode.PERMISSION_DENIED, 
    "功能", "用户管理"
);
```

### 2. 手动设置语言环境

```java
// 手动设置语言环境
LocaleContextHolder.setCurrentLocale(Locale.SIMPLIFIED_CHINESE);

// 获取当前语言环境
Locale currentLocale = LocaleContextHolder.getCurrentLocale();

// 清理语言环境
LocaleContextHolder.clearCurrentLocale();
```

### 3. Gateway传递语言环境

Gateway可以通过以下请求头传递语言环境：

```http
# 方式1：自定义请求头（推荐）
X-Locale: zh_CN

# 方式2：语言环境请求头
X-Language: zh_CN

# 方式3：标准Accept-Language头
Accept-Language: zh-CN,zh;q=0.9,en;q=0.8
```

## 🔧 配置说明

### 1. 简化国际化配置

```java
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class I18nConfig implements WebMvcConfigurer {
    
    // 语言环境解析器
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
        localeResolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        return localeResolver;
    }
    
    // 消息源配置
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(true);
        messageSource.setCacheSeconds(-1);
        return messageSource;
    }
    
    // 注册Spring标准语言切换拦截器（可选）
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/error", "/actuator/**", "/static/**", "/favicon.ico");
    }
}
```

### 2. 支持的语言环境

```java
// 默认支持的语言环境
Locale[] supportedLocales = LocaleContextHolder.getSupportedLocales();
// 包括：zh_CN, zh_TW, en, en_US

// 检查语言环境是否支持
boolean isSupported = LocaleContextHolder.isSupportedLocale(Locale.SIMPLIFIED_CHINESE);
```

## 📊 语言环境获取优先级

```
1. ThreadLocal (最高优先级)
   ↓
2. Gateway传递的请求头 (主要方式)
   - X-Locale
   - X-Language
   - Accept-Language
   ↓
3. Spring LocaleResolver (备用方式)
   ↓
4. 系统默认语言环境 (最低优先级)
```

## 🌐 国际化异常示例

### 1. 自动语言环境

```java
// 请求头：X-Locale: zh_CN
I18nException ex = ExceptionUtils.createException(
    I18nException.class, 
    ErrorCode.PERMISSION_DENIED, 
    "功能", "用户管理"
);
// 异常消息：权限不足：缺少功能权限 - 功能: 用户管理

// 请求头：X-Locale: en_US
I18nException ex = ExceptionUtils.createException(
    I18nException.class, 
    ErrorCode.PERMISSION_DENIED, 
    "function", "user management"
);
// 异常消息：Permission denied: missing function permission - function: user management
```

### 2. 手动指定语言环境

```java
// 手动指定语言环境
I18nException ex = new I18nException(
    ErrorCode.PERMISSION_DENIED, 
    Locale.SIMPLIFIED_CHINESE, 
    "功能", "用户管理"
);
```

## 🔒 安全考虑

### 1. ThreadLocal管理
- **自动清理**：拦截器自动清理ThreadLocal，避免内存泄漏
- **异常安全**：即使发生异常也会清理ThreadLocal

### 2. 语言环境验证
- **格式验证**：验证语言环境格式是否正确
- **支持检查**：检查语言环境是否被支持
- **异常处理**：解析失败时使用默认语言环境

## 📝 最佳实践

### 1. 使用建议
- **优先使用自动处理**：让拦截器自动处理语言环境
- **避免手动设置**：除非必要，不要手动设置语言环境
- **及时清理**：使用完毕后及时清理ThreadLocal

### 2. 错误处理
- **优雅降级**：语言环境获取失败时使用默认语言环境
- **日志记录**：记录语言环境设置和清理的日志
- **异常处理**：完善的异常处理机制

## 🐛 常见问题

### 1. 语言环境不生效
**问题**：设置的语言环境没有生效
**解决方案**：
- 检查拦截器是否正确注册
- 检查请求头格式是否正确
- 检查ThreadLocal是否正确设置

### 2. 内存泄漏
**问题**：ThreadLocal没有清理导致内存泄漏
**解决方案**：
- 确保拦截器的afterCompletion方法被调用
- 手动调用clearCurrentLocale()方法
- 检查异常处理逻辑

### 3. 语言环境解析失败
**问题**：语言环境字符串解析失败
**解决方案**：
- 检查语言环境格式是否正确
- 使用标准的语言环境格式（如zh_CN）
- 查看日志中的错误信息

## 📚 相关文档

- [异常处理模块文档](EXCEPTION_HANDLING.md)
- [Synapse Core 模块文档](README.md)
- [Spring Web MVC 拦截器](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-handlermapping-interceptor)

---

*最后更新时间：2025年01月27日*
