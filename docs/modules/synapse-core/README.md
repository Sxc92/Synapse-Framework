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
- **动态异常创建**：通过反射机制动态创建异常实例
- **自动国际化**：I18nException自动使用当前语言环境
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

### 6. 线程池管理
- **差异化线程池**：针对不同任务类型的专用线程池配置
- **虚拟线程支持**：JDK17虚拟线程，适用于IO密集型任务
- **结构化并发**：使用StructuredTaskScope管理并发任务
- **智能任务路由**：根据任务特性自动选择最佳执行策略

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
- **ThreadUtils**：现代线程工具类，支持虚拟线程和结构化并发

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
    
    # 线程池配置
    thread-pool:
      # IO密集型任务线程池
      io:
        core-pool-size: 50
        max-pool-size: 200
        queue-capacity: 1000
      
      # CPU密集型任务线程池
      cpu:
        core-pool-size: 8
        max-pool-size: 16
        queue-capacity: 100
      
      # 通用任务线程池
      common:
        core-pool-size: 20
        max-pool-size: 100
        queue-capacity: 500
      
      # 监控任务线程池
      monitor:
        core-pool-size: 5
        max-pool-size: 20
        queue-capacity: 200
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
        // 使用动态异常创建
        User user = ExceptionUtils.requireNonNull(
            userRepository.findById(id), 
            BusinessException.class, 
            ErrorCode.USER_NOT_FOUND, 
            "用户ID", id
        );
        
        // 条件异常抛出
        ExceptionUtils.throwIf(
            user.isDisabled(), 
            BusinessException.class, 
            ErrorCode.USER_DISABLED, 
            "用户ID", id
        );
        
        return user;
    }
    
    public void updateUser(User user) {
        try {
            // 业务逻辑
            userRepository.save(user);
        } catch (Exception e) {
            // 包装原始异常
            ExceptionUtils.throwIf(true, BusinessException.class, ErrorCode.OPERATION_FAILED, e, "更新用户失败");
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

#### 线程池使用
```java
@Service
public class TaskService {
    
    @Autowired
    private ThreadUtils threadUtils;
    
    public void processTasks() {
        // IO密集型任务（网络请求、文件操作）
        threadUtils.executeIoTask(() -> {
            // 执行网络请求或文件操作
            downloadFile("https://example.com/file.txt");
        });
        
        // CPU密集型任务（复杂计算）
        threadUtils.executeCpuTask(() -> {
            // 执行复杂计算
            processLargeDataset();
        });
        
        // 通用任务（智能选择线程池）
        threadUtils.executeCommonTask(() -> {
            // 执行一般业务逻辑
            processBusinessLogic();
        });
        
        // 监控任务（低优先级）
        threadUtils.executeMonitorTask(() -> {
            // 执行健康检查
            performHealthCheck();
        });
    }
    
    // 异步任务处理
    public CompletableFuture<String> processAsync() {
        return threadUtils.supplyAsync(() -> {
            // 异步处理逻辑
            return "处理完成";
        });
    }
    
    // 批量任务处理
    public CompletableFuture<Void> processBatchTasks() {
        List<Callable<String>> tasks = Arrays.asList(
            () -> "任务1",
            () -> "任务2",
            () -> "任务3"
        );
        
        return threadUtils.executeIoTasks(tasks);
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
public class CustomExceptionHandler {
    
    @ExceptionHandler(CustomBusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleCustomBusinessException(CustomBusinessException e) {
        log.warn("CustomBusinessException: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(I18nException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleI18nException(I18nException e) {
        log.error("I18nException: code={}, message={}", e.getCode(), e.getMessage(), e);
        return Result.error(e.getCode(), e.getMessage());
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

### 4. 线程池配置自定义
```java
@Configuration
public class CustomThreadPoolConfig {
    
    @Bean("customIoThreadPool")
    public ThreadPoolTaskExecutor customIoThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(100);
        executor.setMaxPoolSize(500);
        executor.setQueueCapacity(2000);
        executor.setThreadNamePrefix("custom-io-");
        executor.initialize();
        return executor;
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

### 4. 线程池性能优化
- **虚拟线程**：IO密集型任务使用虚拟线程，提升并发能力
- **结构化并发**：使用StructuredTaskScope管理并发任务生命周期
- **智能路由**：根据任务特性自动选择最佳执行策略
- **性能监控**：任务执行时间监控和性能分析

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
- 使用动态异常创建，提高代码灵活性
- 定义清晰的异常层次结构
- 使用有意义的错误码和消息
- 避免在异常中暴露敏感信息
- 记录足够的异常上下文信息
- 保持异常链，便于问题追踪

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

### 5. 线程池管理
- **任务分类**：根据任务特性选择合适的线程池类型
- **IO任务**：使用虚拟线程处理网络请求、文件操作等
- **CPU任务**：使用平台线程池处理复杂计算、算法处理等
- **监控任务**：使用低优先级线程池，不影响主业务
- **资源管理**：合理配置线程池参数，避免资源浪费

## 🐛 常见问题

### 1. 配置加载失败
**问题**：配置文件无法加载或配置值获取失败
**解决方案**：
- 检查配置文件路径和格式
- 验证配置文件的权限设置
- 检查配置键名是否正确
- 查看启动日志中的错误信息

### 2. 异常创建失败
**问题**：反射创建异常时失败
**解决方案**：
- 确保异常类有正确的构造函数
- 检查异常类是否继承自BaseException
- 验证ErrorCode是否正确
- 检查异常创建器配置

### 3. 国际化异常不生效
**问题**：I18nException没有使用正确的语言环境
**解决方案**：
- 检查语言环境获取逻辑
- 验证MessageUtils配置
- 确认语言包是否完整
- 检查国际化上下文设置

### 4. 上下文数据丢失
**问题**：请求间的上下文数据丢失
**解决方案**：
- 检查上下文的作用域设置
- 验证线程池的配置
- 确认上下文清理的时机
- 检查异步操作的上下文传递

### 4. 线程池性能问题
**问题**：线程池性能不佳或资源浪费
**解决方案**：
- **IO任务阻塞**：使用虚拟线程处理IO密集型任务
- **CPU任务排队**：调整CPU线程池大小和队列容量
- **监控任务影响主业务**：使用独立的监控线程池
- **资源浪费**：根据实际负载调整线程池参数
- **任务类型判断**：实现智能任务路由，自动选择最佳线程池

## 📚 相关文档

- [Synapse Core - 异常处理模块](EXCEPTION_HANDLING.md)
- [Synapse Framework 架构设计](../../ARCHITECTURE.md)
- [Synapse Framework 使用指南](../../QUICKSTART.md)
- [Synapse Framework 配置参考](../../CONFIGURATION.md)

## 🔗 相关链接

- [Spring Boot 配置管理](https://spring.io/projects/spring-boot)
- [Java 国际化指南](https://docs.oracle.com/javase/tutorial/i18n/)
- [Spring 异常处理](https://spring.io/guides/gs/rest-service/)
- [JDK17 虚拟线程指南](https://docs.oracle.com/en/java/javase/17/core/virtual-threads.html)
- [Java 结构化并发](https://openjdk.org/jeps/428)
- [Spring 线程池配置](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#scheduling)

---

*最后更新时间：2025年08月11日 12:41:56* 