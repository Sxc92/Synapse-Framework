# Synapse Framework 整体架构文档

## 概述

Synapse Framework 是一个基于 Spring Boot 的企业级微服务框架，专注于简化开发流程、提高代码质量和系统性能。框架采用模块化设计，每个模块都有明确的职责和边界。

## 架构设计原则

### 1. 模块化设计
- **高内聚，低耦合** - 每个模块职责单一，模块间依赖清晰
- **可插拔** - 模块可以独立使用或组合使用
- **可扩展** - 支持自定义模块和功能扩展

### 2. 约定优于配置
- **零配置启动** - 大部分功能开箱即用
- **智能默认值** - 提供合理的默认配置
- **配置覆盖** - 支持自定义配置覆盖默认值

### 3. 不重复造轮子
- **充分利用现有生态** - 基于 MyBatis-Plus、Sa-Token 等成熟框架
- **专注业务逻辑** - 让开发者专注于业务代码本身
- **减少样板代码** - 通过注解和自动配置减少重复代码

## 模块架构

```
synapse-framework/
├── synapse-core/          # 核心模块 - 基础组件和工具
├── synapse-databases/     # 数据库模块 - 数据访问层
├── synapse-security/      # 安全模块 - 认证授权
├── synapse-cache/         # 缓存模块 - 缓存管理
├── synapse-events/        # 事件模块 - 事件驱动
├── synapse-bom/           # 依赖管理 - 版本统一
└── docs/                  # 文档目录
```

## 模块详解

### 1. synapse-core (核心模块)

**职责**：提供框架的基础组件和通用工具

**主要功能**：
- ✅ **统一响应格式** - `Result<T>` 统一 API 响应
- ✅ **全局异常处理** - `GlobalExceptionHandler` 统一异常处理
- ✅ **国际化支持** - 多语言消息管理
- ✅ **上下文管理** - `UserContext` 用户上下文
- ✅ **工具类库** - 常用工具类和辅助方法

**核心组件**：
```java
// 统一响应格式
Result<T> - 标准化的 API 响应结构

// 全局异常处理
GlobalExceptionHandler - WebFlux 环境下的异常处理器

// 用户上下文
UserContext - ThreadLocal 用户上下文管理

// 异常体系
BusinessException - 业务异常
IAMException - 身份认证异常
I18nException - 国际化异常
```

### 2. synapse-databases (数据库模块)

**职责**：提供数据访问层的增强功能

**主要功能**：
- ✅ **增强的 Repository** - `BaseRepository` 扩展 MyBatis-Plus
- ✅ **注解驱动查询** - `@QueryCondition` 自动构建查询条件
- ✅ **智能分页** - 统一的分页参数和结果格式
- ✅ **动态数据源** - 多数据源支持和自动切换
- ✅ **审计字段** - 自动填充创建/修改信息

**核心组件**：
```java
// 基础仓库接口
BaseRepository<T> - 扩展 IService 的增强接口

// 查询条件构建器
QueryConditionBuilder - 基于注解的查询条件构建
LambdaQueryBuilder - Lambda 表达式查询构建

// 增强查询构建器
EnhancedQueryBuilder - 复杂查询支持（聚合、分组、关联）

// 代理拦截器
SqlMethodInterceptor - 方法调用拦截和路由

// 实体基类
BaseEntity - 基础实体类
CreatedEntity - 包含创建信息的实体
AuditEntity - 包含审计信息的实体
```

**注解支持**：
```java
@QueryCondition - 查询条件注解
@AutoService - 自动生成 Service 实现
@AutoRepository - 自动生成 Repository 实现
```

### 3. synapse-security (安全模块)

**职责**：提供认证授权和安全控制

**主要功能**：
- ✅ **Sa-Token 集成** - 基于 Sa-Token 的认证授权
- ✅ **注解权限控制** - `@SaCheckLogin`、`@SaCheckPermission` 等
- ✅ **用户会话管理** - 用户会话的创建、验证和管理
- ✅ **权限策略** - 灵活的权限验证策略
- ✅ **Token 续期** - 自动 Token 续期机制

**核心组件**：
```java
// 认证策略
SaTokenAuthenticationStrategy - Sa-Token 认证策略
OAuth2AuthenticationStrategy - OAuth2 认证策略

// 权限管理
PermissionManager - 权限管理器（实现 StpInterface）
TokenManager - Token 管理器

// 用户上下文拦截器
UserContextInterceptor - 用户上下文设置拦截器

// 会话服务
UserSessionService - 用户会话管理服务
```

**注解支持**：
```java
@SaCheckLogin - 登录检查
@SaCheckPermission - 权限检查
@SaCheckRole - 角色检查
@SaCheckSafe - 安全检查
```

### 4. synapse-cache (缓存模块)

**职责**：提供缓存管理和会话存储

**主要功能**：
- ✅ **Redis 集成** - 基于 Redis 的缓存实现
- ✅ **会话存储** - 用户会话的 Redis 存储
- ✅ **缓存策略** - 灵活的缓存策略配置
- ✅ **分布式锁** - 基于 Redis 的分布式锁
- ✅ **缓存注解** - 声明式缓存支持

**核心组件**：
```java
// 会话管理
UserSessionService - 用户会话服务
DefaultSessionManager - 默认会话管理器

// 缓存配置
RedisConfig - Redis 配置
CacheConfig - 缓存配置

// 分布式锁
DistributedLock - 分布式锁接口
RedisDistributedLock - Redis 分布式锁实现
```

### 5. synapse-events (事件模块)

**职责**：提供事件驱动架构支持

**主要功能**：
- ✅ **事件发布订阅** - 基于 Spring Events 的事件机制
- ✅ **事务事件** - 事务相关的事件处理
- ✅ **异步事件** - 异步事件处理支持
- ✅ **事件重试** - 事件处理失败重试机制
- ✅ **事件监控** - 事件处理的监控和统计

**核心组件**：
```java
// 事件发布器
UnifiedPublisher - 统一事件发布器
ReliablePublisher - 可靠事件发布器

// 事件消费者
ReliableConsumer - 可靠事件消费者
EventConsumer - 事件消费者接口

// 事件配置
EventConfig - 事件配置
TransactionEventConfig - 事务事件配置
```

### 6. synapse-bom (依赖管理)

**职责**：统一管理所有模块的依赖版本

**主要功能**：
- ✅ **版本统一** - 统一所有依赖的版本号
- ✅ **依赖管理** - 集中管理第三方依赖
- ✅ **版本兼容** - 确保各模块版本兼容性
- ✅ **简化配置** - 简化项目的依赖配置

## 技术栈

### 核心框架
- **Spring Boot 3.x** - 应用框架
- **Spring WebFlux** - 响应式 Web 框架
- **Spring Security** - 安全框架

### 数据访问
- **MyBatis-Plus** - ORM 框架
- **Dynamic Datasource** - 动态数据源
- **Redis** - 缓存和会话存储

### 安全认证
- **Sa-Token** - 轻量级认证框架
- **JWT** - JSON Web Token

### 其他工具
- **Lombok** - 代码生成工具
- **Jackson** - JSON 处理
- **SLF4J** - 日志框架

## 快速开始

### 1. 添加依赖

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.indigo</groupId>
            <artifactId>synapse-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- 核心模块 -->
    <dependency>
        <groupId>com.indigo</groupId>
        <artifactId>synapse-core</artifactId>
    </dependency>
    
    <!-- 数据库模块 -->
    <dependency>
        <groupId>com.indigo</groupId>
        <artifactId>synapse-databases</artifactId>
    </dependency>
    
    <!-- 安全模块 -->
    <dependency>
        <groupId>com.indigo</groupId>
        <artifactId>synapse-security</artifactId>
    </dependency>
    
    <!-- 缓存模块 -->
    <dependency>
        <groupId>com.indigo</groupId>
        <artifactId>synapse-cache</artifactId>
    </dependency>
</dependencies>
```

### 2. 基础配置

```yaml
# application.yml
spring:
  application:
    name: your-application
  
  # 数据源配置
  datasource:
    url: jdbc:mysql://localhost:3306/your_db
    username: your_username
    password: your_password
    
  # Redis 配置
  redis:
    host: localhost
    port: 6379
    
# Sa-Token 配置
sa-token:
  token-name: Authorization
  timeout: 2592000
  activity-timeout: -1
  is-concurrent: true
  is-share: false
  token-style: uuid
  is-log: false
```

### 3. 创建实体

```java
@Data
@TableName("users")
public class User extends AuditEntity<Long> {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String username;
    
    private String email;
    
    @TableLogic
    private Integer deleted;
}
```

### 4. 创建 Repository

```java
@AutoRepository
public interface UserRepository extends BaseRepository<User> {
    
    @QueryCondition
    List<User> findByUsernameAndEmail(String username, String email);
    
    @QueryCondition
    PageResult<User> pageByStatus(Integer status, PageDTO pageDTO);
}
```

### 5. 创建 Service

```java
@AutoService
public interface UserService extends BaseRepository<User> {
    
    User createUser(User user);
    
    User getUserById(Long id);
    
    PageResult<User> getUsersByPage(PageDTO pageDTO);
}
```

### 6. 创建 Controller

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping
    public Result<User> createUser(@RequestBody User user) {
        User created = userService.createUser(user);
        return Result.success(created);
    }
    
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return Result.success(user);
    }
    
    @GetMapping
    public Result<PageResult<User>> getUsers(PageDTO pageDTO) {
        PageResult<User> users = userService.getUsersByPage(pageDTO);
        return Result.success(users);
    }
}
```

## 最佳实践

### 1. 模块使用建议

- **新项目**：建议从 `synapse-core` 和 `synapse-databases` 开始
- **安全需求**：添加 `synapse-security` 模块
- **缓存需求**：添加 `synapse-cache` 模块
- **事件驱动**：添加 `synapse-events` 模块

### 2. 代码规范

- **统一响应格式**：使用 `Result<T>` 包装所有 API 响应
- **异常处理**：使用框架提供的异常类型
- **注解驱动**：优先使用注解配置，减少代码量
- **日志记录**：使用 SLF4J 进行日志记录

### 3. 性能优化

- **合理使用缓存**：对频繁访问的数据使用缓存
- **分页查询**：大数据量查询使用分页
- **索引优化**：数据库表添加合适的索引
- **连接池配置**：合理配置数据库连接池

### 4. 安全建议

- **权限控制**：使用注解进行细粒度权限控制
- **Token 管理**：合理设置 Token 过期时间
- **数据验证**：对输入数据进行严格验证
- **SQL 注入防护**：使用参数化查询

## 常见问题

### 1. 编译问题

**问题**：`NoClassDefFoundError: SqlMethodInterceptor$1`
**解决**：已修复，使用静态内部类替代匿名内部类

### 2. 配置问题

**问题**：MyBatis-Plus 配置不生效
**解决**：确保添加了 `@MapperScan` 注解

### 3. 权限问题

**问题**：Sa-Token 注解不生效
**解决**：确保添加了 `SaInterceptor` 配置

### 4. 缓存问题

**问题**：Redis 连接失败
**解决**：检查 Redis 配置和网络连接

## 版本历史

### v1.0.0 (当前版本)
- ✅ 核心模块基础功能
- ✅ 数据库模块增强功能
- ✅ 安全模块认证授权
- ✅ 缓存模块会话管理
- ✅ 事件模块事件驱动
- ✅ 依赖管理版本统一

## 贡献指南

### 1. 代码贡献
- Fork 项目
- 创建功能分支
- 提交代码
- 创建 Pull Request

### 2. 文档贡献
- 完善现有文档
- 添加使用示例
- 修复文档错误

### 3. 问题反馈
- 使用 GitHub Issues
- 提供详细的错误信息
- 包含复现步骤

## 许可证

本项目采用 MIT 许可证，详见 [LICENSE](LICENSE) 文件。

## 联系方式

- **项目地址**：[GitHub Repository]
- **问题反馈**：[GitHub Issues]
- **邮箱联系**：[your-email@example.com]

---

*最后更新时间：2025年1月7日* 