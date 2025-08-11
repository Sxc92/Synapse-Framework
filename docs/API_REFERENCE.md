# Synapse Framework API 参考文档

## 📚 概述

本文档提供了 Synapse Framework 各模块的 API 接口参考，包括核心功能、配置选项、注解说明等。开发者可以通过本文档快速了解框架提供的各种功能和接口。

## 🏗️ 核心模块 API

### Synapse Core 模块

#### 配置管理 API
- **ConfigurationManager**: 统一的配置管理接口，支持多环境配置、配置验证、配置热更新
- **ConfigurationSource**: 自定义配置源接口，支持从外部系统加载配置
- **ConfigurationValidator**: 配置验证器接口，提供配置项的自动验证

#### 异常处理 API
- **BusinessException**: 业务异常基类，包含错误码、错误消息、错误详情
- **SystemException**: 系统异常基类，用于系统级别的异常处理
- **ExceptionHandler**: 异常处理器接口，支持自定义异常处理逻辑
- **ErrorResponse**: 统一错误响应格式，包含错误码、消息、时间戳等

#### 国际化 API
- **MessageProvider**: 消息提供者接口，支持多语言消息获取
- **MessageManager**: 消息管理器，负责消息的加载、缓存、更新
- **LocaleResolver**: 语言解析器，支持多种语言切换策略

#### 上下文管理 API
- **ContextManager**: 上下文管理器，管理请求、用户、业务上下文
- **RequestContext**: 请求上下文，包含请求ID、时间戳、用户信息等
- **UserContext**: 用户上下文，包含用户ID、角色、权限等
- **BusinessContext**: 业务上下文，包含业务ID、业务类型等

### Synapse Databases 模块

#### 数据源管理 API
- **DynamicDataSourceManager**: 动态数据源管理器，支持运行时数据源切换
- **DataSourceRouter**: 数据源路由器，根据SQL类型自动选择读写数据源
- **ConnectionPoolManager**: 连接池管理器，管理多种连接池类型

#### MyBatis-Plus 集成 API
- **BaseMapper**: 基础映射器接口，提供通用CRUD操作
- **BaseService**: 基础服务接口，封装常用业务操作
- **PaginationInterceptor**: 分页拦截器，支持多种分页方式

#### 事务管理 API
- **TransactionManager**: 事务管理器，支持分布式事务
- **TransactionTemplate**: 事务模板，简化事务操作
- **TransactionInterceptor**: 事务拦截器，自动事务管理

### Synapse Cache 模块

#### 缓存管理 API
- **CacheManager**: 缓存管理器，统一管理多种缓存实现
- **Cache**: 缓存接口，定义缓存的基本操作
- **CacheStatistics**: 缓存统计接口，提供缓存性能指标

#### 分布式锁 API
- **DistributedLockManager**: 分布式锁管理器，支持多种锁策略
- **LockProvider**: 锁提供者接口，支持Redis、Zookeeper等实现
- **LockStrategy**: 锁策略接口，支持可重入锁、公平锁等

#### 缓存注解 API
- **@Cacheable**: 缓存查询结果，支持条件缓存和键生成策略
- **@CachePut**: 更新缓存，支持条件更新和键生成策略
- **@CacheEvict**: 删除缓存，支持条件删除和批量删除
- **@DistributedLock**: 分布式锁注解，支持超时和重试配置

### Synapse Events 模块

#### 事件发布 API
- **EventPublisher**: 事件发布器，支持同步和异步发布
- **EventDispatcher**: 事件分发器，负责事件的路由和分发
- **EventRouter**: 事件路由器，支持条件路由和优先级路由

#### 事件订阅 API
- **EventSubscriber**: 事件订阅器接口，定义事件处理方法
- **EventListener**: 事件监听器注解，支持条件订阅和异步处理
- **EventFilter**: 事件过滤器接口，支持自定义过滤逻辑

#### 事件存储 API
- **EventStore**: 事件存储接口，支持事件持久化和查询
- **EventRepository**: 事件仓库，提供事件CRUD操作
- **EventQuery**: 事件查询接口，支持复杂查询条件

### Synapse Security 模块

#### 认证 API
- **AuthenticationManager**: 认证管理器，支持多种认证方式
- **LoginService**: 登录服务，处理用户登录逻辑
- **TokenManager**: Token管理器，管理JWT和Session Token

#### 权限控制 API
- **PermissionManager**: 权限管理器，验证用户权限
- **RoleManager**: 角色管理器，管理用户角色和权限
- **AccessControl**: 访问控制接口，实现细粒度权限控制

#### 安全防护 API
- **XssFilter**: XSS防护过滤器，防止跨站脚本攻击
- **CsrfProtection**: CSRF防护，防止跨站请求伪造
- **SecurityInterceptor**: 安全拦截器，统一安全验证

## 🔧 配置 API

### 基础配置
- **synapse.core**: 核心模块配置，包含国际化、异常处理、上下文等配置
- **synapse.datasource**: 数据源配置，包含主从配置、连接池配置等
- **synapse.cache**: 缓存配置，包含缓存类型、策略、分布式锁等配置
- **synapse.events**: 事件配置，包含异步处理、分布式事件等配置
- **synapse.security**: 安全配置，包含认证、权限、防护等配置

### 环境配置
- **application.yml**: 主配置文件，包含所有模块的基础配置
- **application-{profile}.yml**: 环境特定配置，支持dev、test、prod等环境
- **bootstrap.yml**: 启动配置，包含配置中心、注册中心等配置

## 📝 注解参考

### 核心注解
- **@EnableSynapse**: 启用Synapse Framework，自动配置所有模块
- **@SynapseConfiguration**: 自定义配置类注解，支持自定义配置逻辑

### 缓存注解
- **@Cacheable**: 缓存查询结果，支持条件缓存
- **@CachePut**: 更新缓存，支持条件更新
- **@CacheEvict**: 删除缓存，支持条件删除
- **@DistributedLock**: 分布式锁，支持超时和重试

### 安全注解
- **@SaCheckLogin**: 登录验证，检查用户是否已登录
- **@SaCheckPermission**: 权限验证，检查用户是否有指定权限
- **@SaCheckRole**: 角色验证，检查用户是否有指定角色
- **@SaCheckSafe**: 安全验证，检查操作是否安全

### 事件注解
- **@EventListener**: 事件监听，处理指定类型的事件
- **@AsyncEventListener**: 异步事件监听，异步处理事件
- **@TransactionalEventListener**: 事务事件监听，支持事务绑定

## 🚀 扩展 API

### 自定义扩展点
- **ExtensionPoint**: 扩展点接口，定义扩展的标准接口
- **ExtensionRegistry**: 扩展注册器，管理所有扩展实现
- **ExtensionLoader**: 扩展加载器，动态加载扩展实现

### 插件系统
- **PluginManager**: 插件管理器，管理框架插件
- **PluginInterface**: 插件接口，定义插件的标准接口
- **PluginRegistry**: 插件注册器，注册和管理插件

### 监控 API
- **MetricsCollector**: 指标收集器，收集性能指标
- **HealthChecker**: 健康检查器，检查系统健康状态
- **PerformanceMonitor**: 性能监控器，监控系统性能

## 📊 错误码参考

### 系统错误码
- **SYS_001**: 系统内部错误
- **SYS_002**: 配置错误
- **SYS_003**: 网络错误
- **SYS_004**: 超时错误

### 业务错误码
- **BIZ_001**: 业务逻辑错误
- **BIZ_002**: 数据验证错误
- **BIZ_003**: 权限不足
- **BIZ_004**: 资源不存在

### 缓存错误码
- **CACHE_001**: 缓存连接失败
- **CACHE_002**: 缓存操作超时
- **CACHE_003**: 缓存键不存在
- **CACHE_004**: 缓存序列化失败

### 数据库错误码
- **DB_001**: 数据库连接失败
- **DB_002**: SQL执行错误
- **DB_003**: 事务回滚
- **DB_004**: 数据源切换失败

## 🔍 最佳实践

### API 设计原则
- **一致性**: 保持API接口的一致性和可预测性
- **简洁性**: 设计简洁明了的API接口
- **可扩展性**: 支持API的向后兼容和扩展
- **安全性**: 内置安全验证和防护机制

### 性能优化
- **缓存策略**: 合理使用缓存提高性能
- **异步处理**: 使用异步处理提高响应速度
- **批量操作**: 支持批量操作减少网络开销
- **连接池**: 优化连接池配置提高资源利用率

### 监控和调试
- **日志记录**: 完整的操作日志和错误日志
- **性能指标**: 详细的性能监控指标
- **健康检查**: 系统健康状态检查
- **调试工具**: 丰富的调试和诊断工具

## 📚 相关文档

- [Synapse Framework 架构设计](ARCHITECTURE.md)
- [Synapse Framework 使用指南](USAGE_GUIDE.md)
- [Synapse Framework 配置参考](CONFIGURATION_REFERENCE.md)
- [Synapse Framework 开发笔记](DEVELOPMENT_NOTES.md)

## 🔗 相关链接

- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [MyBatis-Plus 官方文档](https://baomidou.com/)
- [Sa-Token 官方文档](https://sa-token.dev33.cn/)
- [Redis 官方文档](https://redis.io/documentation)

---

*最后更新时间：2025年08月11日 12:41:56* 