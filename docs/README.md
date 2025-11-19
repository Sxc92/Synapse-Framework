# Synapse Framework

一个基于 Spring Boot 3.x 的企业级微服务框架，专注于简化开发流程、提高代码质量和系统性能。

## 🚀 核心特性

- ✅ **模块化设计** - 高内聚、低耦合的模块架构，按需引入
- ✅ **统一响应** - 标准化的 API 响应格式 `Result<T>`
- ✅ **异常处理** - 统一的异常处理机制（`Ex.throwEx()`），支持国际化错误消息
- ✅ **智能分页** - 自动分页和结果封装，支持聚合查询
- ✅ **认证授权** - 基于 Token 的认证（自研 TokenService），支持滑动过期、自动续期
- ✅ **权限控制** - 基于注解的细粒度权限管理（@RequireLogin、@RequirePermission、@RequireRole）
- ✅ **二级缓存** - Caffeine 本地缓存 + Redis 分布式缓存，自动降级
- ✅ **动态数据源** - 智能数据源路由，自动读写分离，支持故障转移
- ✅ **事件驱动** - 异步事件处理机制，支持同步/异步事件
- ✅ **国际化支持** - 完整的 i18n 支持，错误消息多语言
- ✅ **VO 映射** - 自动字段映射（VoMapper），支持数据库字段到 VO 的转换
- ✅ **审计字段** - 自动填充创建时间、修改时间、用户信息等

## 📚 快速导航

| 文档 | 描述 | 适用场景 |
|------|------|----------|
| **[🚀 快速开始](QUICKSTART.md)** | 5分钟搭建项目 | 新用户入门 |
| **[🏗️ 架构设计](ARCHITECTURE.md)** | 整体架构和设计模式 | 架构师、技术决策 |
| **[📖 API 参考](API_REFERENCE.md)** | 详细 API 文档 | 开发人员 |
| **[⚙️ 配置指南](CONFIGURATION.md)** | 配置参数说明 | 运维人员 |
| **[🔧 模块文档](MODULES/)** | 各模块详细说明 | 模块开发 |
| **[📋 配置模板](CONFIGURATION_TEMPLATES.md)** | 常用配置模板 | 快速配置 |

## 🎯 技术栈

- **Spring Boot 3.x** - 应用框架
- **MyBatis-Plus** - ORM 框架
- **Redis** - 缓存服务（支持单机、哨兵、集群模式）
- **Caffeine** - 本地缓存
- **MySQL/PostgreSQL/Oracle** - 多数据库支持
- **HikariCP/Druid** - 连接池支持
- **Java 17+** - JDK 版本要求

## 📦 模块说明

| 模块 | 描述 | 主要功能 |
|------|------|----------|
| **synapse-core** | 核心模块 | 统一响应（Result）、异常处理（Ex）、工具类、国际化支持 |
| **synapse-databases** | 数据库模块 | BaseRepository、动态数据源、VO 映射、查询构建器、审计字段 |
| **synapse-security** | 安全模块 | Token 认证（自研 TokenService）、权限控制、滑动过期、自动续期、Gateway 签名验证 |
| **synapse-cache** | 缓存模块 | 二级缓存（Caffeine + Redis）、分布式锁、用户会话管理、缓存预热 |
| **synapse-events** | 事件模块 | 事件驱动、同步/异步事件、事件发布订阅 |
| **synapse-i18n** | 国际化模块 | 多语言支持、错误消息国际化、消息资源管理 |

## 🚀 快速开始

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
# 数据源配置
synapse:
  datasource:
    dynamic-data-source:
      primary: master
      datasource:
        master:
          type: MYSQL
          host: localhost
          port: 3306
          database: your_db
          username: your_username
          password: your_password
          pool-type: HIKARI

# Redis 配置
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: your_password

# 安全配置
synapse:
  security:
    enabled: true
    mode: STRICT  # STRICT(严格)、PERMISSIVE(宽松)、DISABLED(关闭)
    token:
      timeout: 7200  # Token 过期时间（秒），默认 2 小时
      enable-sliding-expiration: true  # 启用滑动过期（自动刷新）
      refresh-threshold: 600  # 刷新阈值（秒），当 token 剩余时间少于 10 分钟时自动续期
      renewal-duration: 7200  # 续期时长（秒），刷新时将过期时间延长到 2 小时
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
}
```

### 4. 创建 Repository

```java
@Repository
public interface UserRepository extends BaseRepository<User> {
    
    // 使用 @QueryCondition 自动构建查询条件
    @QueryCondition
    List<UserVO> findByUsername(String username);
    
    // 分页查询，自动映射到 VO
    PageResult<UserVO> pageUsers(UserPageDTO pageDTO);
}
```

### 5. 创建 Service

```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public PageResult<UserVO> getUsers(UserPageDTO pageDTO) {
        return userRepository.pageUsers(pageDTO);
    }
    
    public UserVO createUser(CreateUserDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        userRepository.save(user);
        return VoMapper.toVO(user, UserVO.class);
    }
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
    @RequirePermission("user:create")
    public Result<UserVO> createUser(@RequestBody @Valid CreateUserDTO dto) {
        UserVO user = userService.createUser(dto);
        return Result.success(user);
    }
    
    @GetMapping("/page")
    @RequireLogin
    public Result<PageResult<UserVO>> getUsers(UserPageDTO pageDTO) {
        PageResult<UserVO> result = userService.getUsers(pageDTO);
        return Result.success(result);
    }
}
```

## 🔧 开发环境要求

- **JDK 17+**
- **Maven 3.6+**
- **MySQL 8.0+** 或 **PostgreSQL 12+**
- **Redis 6.0+**

## 💡 主要功能亮点

### 认证授权
- 🔐 **Token 认证**：基于 UUID 的 Token 生成，支持滑动过期和自动续期
- 🛡️ **权限控制**：基于注解的权限验证（@RequireLogin、@RequirePermission、@RequireRole）
- 🔑 **用户会话**：完整的用户会话管理，支持权限、角色、菜单、资源的缓存
- 🔄 **自动续期**：Token 剩余时间少于阈值时自动续期，支持菜单和资源的同步续期

### 数据访问
- 📊 **BaseRepository**：强大的 Repository 接口，支持 VO 映射、多表关联查询
- 🔍 **查询构建器**：增强的查询构建器，支持聚合查询、性能监控
- 🗄️ **动态数据源**：支持多数据源动态切换，自动读写分离
- 🎯 **VO 映射**：自动字段映射，支持 @FieldMapping 注解

### 缓存管理
- ⚡ **二级缓存**：Caffeine 本地缓存 + Redis 分布式缓存，自动降级
- 🔒 **分布式锁**：基于 Redis 的分布式锁实现
- 📦 **缓存预热**：支持应用启动时的缓存预热
- 🔔 **缓存失效通知**：基于 Redis Pub/Sub 的分布式缓存一致性保证

### 异常处理
- 🚨 **统一异常**：使用 `Ex.throwEx()` 统一异常处理
- 🌍 **国际化**：错误消息支持多语言
- 📝 **错误码体系**：标准化的错误码定义和管理

## 📖 更多信息

- **[📚 完整文档](docs/)** - 查看详细的使用文档
- **[🏗️ 架构设计](ARCHITECTURE.md)** - 了解框架架构
- **[🚀 快速开始](QUICKSTART.md)** - 详细的使用教程
- **[🔧 模块文档](MODULES/)** - 各模块详细说明
- **[📋 配置模板](CONFIGURATION_TEMPLATES.md)** - 常用配置模板

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

本项目采用 MIT 许可证。

---

*Synapse Framework - 让开发更简单* 