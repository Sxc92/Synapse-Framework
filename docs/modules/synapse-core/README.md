# Synapse Core 模块

## 📖 模块概述

Synapse Core 是 Synapse Framework 的核心基础模块，提供了框架的基础设施和通用功能。该模块包含了框架的核心配置、异常处理、工具类、国际化支持等基础功能，为其他模块提供统一的底层支持。

## 🎯 核心功能

### 1. 基础配置管理
- **配置加载**：统一的配置加载和管理机制
- **环境配置**：支持多环境配置切换
- **配置验证**：配置项的自动验证和错误提示

### 2. 异常处理体系
- **统一异常**：标准化的异常定义和分类
- **异常转换**：自动异常转换和错误码映射
- **异常日志**：结构化的异常日志记录

### 3. 工具类集合
- **字符串工具**：字符串处理和验证工具
- **日期工具**：日期时间处理和格式化
- **加密工具**：常用加密算法和哈希函数
- **反射工具**：反射操作的便捷方法

### 4. 国际化支持
- **多语言支持**：支持中英文等多种语言
- **消息管理**：统一的消息管理和配置
- **动态切换**：运行时语言切换支持

### 5. 上下文管理
- **请求上下文**：请求级别的上下文信息管理
- **用户上下文**：用户身份和权限信息管理
- **业务上下文**：业务相关的上下文数据管理

## 🏗️ 架构设计

### 模块结构
```
synapse-core/
├── config/          # 配置管理
├── constants/       # 常量定义
├── context/         # 上下文管理
├── entity/          # 基础实体
├── exception/       # 异常处理
└── utils/           # 工具类
```

### 核心接口
- **ConfigurationManager**：配置管理器接口
- **ContextManager**：上下文管理器接口
- **ExceptionHandler**：异常处理器接口
- **MessageProvider**：消息提供者接口

## 🚀 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.indigo</groupId>
    <artifactId>synapse-core</artifactId>
    <version>${synapse.version}</version>
</dependency>
```

### 2. 基础配置

```yaml
# application.yml
synapse:
  core:
    # 国际化配置
    i18n:
      default-locale: zh_CN
      supported-locales: zh_CN,en_US
      message-basename: i18n/messages
    
    # 异常处理配置
    exception:
      enable-global-handler: true
      log-level: ERROR
      include-stack-trace: false
    
    # 上下文配置
    context:
      enable-request-context: true
      enable-user-context: true
      context-timeout: 30000
```

### 3. 使用示例

#### 配置管理
```java
@Component
public class AppConfig {
    
    @Autowired
    private ConfigurationManager configManager;
    
    public void loadConfig() {
        // 获取配置值
        String appName = configManager.getString("app.name");
        Integer port = configManager.getInteger("server.port", 8080);
        
        // 检查配置是否存在
        if (configManager.hasProperty("database.url")) {
            String dbUrl = configManager.getString("database.url");
        }
    }
}
```

#### 异常处理
```java
@Service
public class UserService {
    
    public User findUser(Long id) {
        try {
            // 业务逻辑
            return userRepository.findById(id);
        } catch (Exception e) {
            // 抛出标准异常
            throw new BusinessException("USER_NOT_FOUND", "用户不存在: " + id);
        }
    }
}
```

#### 国际化使用
```java
@RestController
public class MessageController {
    
    @Autowired
    private MessageProvider messageProvider;
    
    @GetMapping("/message")
    public String getMessage(@RequestParam String key) {
        // 获取当前语言的消息
        return messageProvider.getMessage(key);
        
        // 获取指定语言的消息
        return messageProvider.getMessage(key, Locale.ENGLISH);
        
        // 带参数的消息
        return messageProvider.getMessage("welcome.user", new Object[]{"张三"});
    }
}
```

#### 上下文管理
```java
@Service
public class BusinessService {
    
    public void processBusiness() {
        // 获取请求上下文
        RequestContext requestContext = ContextManager.getRequestContext();
        String requestId = requestContext.getRequestId();
        
        // 获取用户上下文
        UserContext userContext = ContextManager.getUserContext();
        Long userId = userContext.getUserId();
        
        // 设置业务上下文
        BusinessContext businessContext = new BusinessContext();
        businessContext.setBusinessId("ORDER_001");
        ContextManager.setBusinessContext(businessContext);
    }
}
```

## 🔧 高级功能

### 1. 自定义配置源
```java
@Component
public class CustomConfigurationSource implements ConfigurationSource {
    
    @Override
    public Map<String, Object> loadConfiguration() {
        Map<String, Object> config = new HashMap<>();
        // 从外部系统加载配置
        config.put("external.api.url", "https://api.example.com");
        return config;
    }
}
```

### 2. 自定义异常处理器
```java
@Component
public class CustomExceptionHandler implements ExceptionHandler {
    
    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof CustomBusinessException;
    }
    
    @Override
    public ErrorResponse handle(Throwable exception) {
        CustomBusinessException ex = (CustomBusinessException) exception;
        return ErrorResponse.builder()
            .code(ex.getErrorCode())
            .message(ex.getMessage())
            .build();
    }
}
```

### 3. 自定义消息提供者
```java
@Component
public class DatabaseMessageProvider implements MessageProvider {
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Override
    public String getMessage(String key, Locale locale) {
        return messageRepository.findByKeyAndLocale(key, locale)
            .map(Message::getContent)
            .orElse(key);
    }
}
```

## 📊 性能特性

### 1. 配置缓存
- **内存缓存**：配置值的内存缓存机制
- **变更监听**：配置变更的自动监听和更新
- **懒加载**：配置的按需加载策略

### 2. 上下文优化
- **线程安全**：线程安全的上下文管理
- **内存优化**：上下文数据的自动清理
- **性能监控**：上下文操作的性能监控

### 3. 异常处理优化
- **异常缓存**：常见异常的缓存机制
- **异步处理**：异常日志的异步记录
- **性能分析**：异常处理的性能分析

## 🔒 安全特性

### 1. 配置安全
- **敏感信息加密**：敏感配置的自动加密
- **访问控制**：配置访问的权限控制
- **审计日志**：配置变更的审计日志

### 2. 上下文安全
- **数据隔离**：不同请求间的数据隔离
- **权限验证**：上下文访问的权限验证
- **数据脱敏**：敏感数据的自动脱敏

## 📝 最佳实践

### 1. 配置管理
- 使用有意义的配置键名
- 为配置项提供默认值
- 使用环境变量覆盖敏感配置
- 定期审查和清理无用配置

### 2. 异常处理
- 定义清晰的异常层次结构
- 使用有意义的错误码和消息
- 避免在异常中暴露敏感信息
- 记录足够的异常上下文信息

### 3. 国际化
- 使用统一的消息键命名规范
- 避免硬编码的文本内容
- 支持消息的参数化
- 提供完整的语言包

### 4. 上下文管理
- 及时清理不再需要的上下文数据
- 避免在上下文中存储大量数据
- 使用合适的上下文作用域
- 监控上下文的内存使用情况

## 🐛 常见问题

### 1. 配置加载失败
**问题**：配置文件无法加载或配置值获取失败
**解决方案**：
- 检查配置文件路径和格式
- 验证配置文件的权限设置
- 检查配置键名是否正确
- 查看启动日志中的错误信息

### 2. 国际化消息缺失
**问题**：某些语言的消息无法显示
**解决方案**：
- 检查消息文件是否存在
- 验证消息键名是否正确
- 确认语言包是否完整
- 检查消息文件的编码格式

### 3. 上下文数据丢失
**问题**：请求间的上下文数据丢失
**解决方案**：
- 检查上下文的作用域设置
- 验证线程池的配置
- 确认上下文清理的时机
- 检查异步操作的上下文传递

## 📚 相关文档

- [Synapse Framework 架构设计](../../ARCHITECTURE.md)
- [Synapse Framework 使用指南](../../USAGE_GUIDE.md)
- [Synapse Framework 开发笔记](../../DEVELOPMENT_NOTES.md)

## 🔗 相关链接

- [Spring Boot 配置管理](https://spring.io/projects/spring-boot)
- [Java 国际化指南](https://docs.oracle.com/javase/tutorial/i18n/)
- [Spring 异常处理](https://spring.io/guides/gs/rest-service/)

---

*最后更新时间：2025年08月11日 12:41:56* 