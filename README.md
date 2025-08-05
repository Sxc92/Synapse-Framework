# Synapse Framework

一个基于 Spring Boot 的企业级微服务框架，专注于简化开发流程、提高代码质量和系统性能。

## 📚 文档

详细的文档请查看 [docs/](docs/) 目录：

- **[📖 框架概述](docs/README.md)** - 框架介绍、模块架构、技术栈
- **[🏗️ 架构设计](docs/ARCHITECTURE.md)** - 整体架构、设计模式、数据流
- **[📋 使用指南](docs/USAGE_GUIDE.md)** - 详细的使用教程和代码示例
- **[🔍 文档索引](docs/INDEX.md)** - 快速导航和问题解决

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
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your_db
    username: your_username
    password: your_password
    
  redis:
    host: localhost
    port: 6379

sa-token:
  token-name: Authorization
  timeout: 2592000
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
@AutoRepository
public interface UserRepository extends BaseRepository<User> {
    
    @QueryCondition
    List<User> findByUsername(String username);
}
```

### 5. 创建 Controller

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping
    public Result<User> createUser(@RequestBody User user) {
        return Result.success(userService.createUser(user));
    }
}
```

## 📦 模块说明

| 模块 | 描述 | 主要功能 |
|------|------|----------|
| **synapse-core** | 核心模块 | 统一响应、异常处理、工具类 |
| **synapse-databases** | 数据库模块 | 增强的 Repository、查询构建器 |
| **synapse-security** | 安全模块 | Sa-Token 认证、权限控制 |
| **synapse-cache** | 缓存模块 | Redis 缓存、分布式锁 |
| **synapse-events** | 事件模块 | 事件驱动、异步处理 |
| **synapse-bom** | 依赖管理 | 版本统一、依赖管理 |

## 🎯 核心特性

- ✅ **模块化设计** - 高内聚、低耦合的模块架构
- ✅ **注解驱动** - 通过注解简化开发
- ✅ **统一响应** - 标准化的 API 响应格式
- ✅ **智能分页** - 自动分页和结果封装
- ✅ **权限控制** - 细粒度的权限管理
- ✅ **缓存支持** - 多级缓存和分布式锁
- ✅ **事件驱动** - 异步事件处理机制
- ✅ **动态数据源** - 多数据源支持

## 🔧 技术栈

- **Spring Boot 3.x** - 应用框架
- **Spring WebFlux** - 响应式 Web 框架
- **MyBatis-Plus** - ORM 框架
- **Sa-Token** - 认证框架
- **Redis** - 缓存服务
- **MySQL** - 数据库

## 📖 更多信息

- **[📚 完整文档](docs/)** - 查看详细的使用文档
- **[🏗️ 架构设计](docs/ARCHITECTURE.md)** - 了解框架架构
- **[📋 使用指南](docs/USAGE_GUIDE.md)** - 详细的使用教程
- **[🔍 常见问题](docs/USAGE_GUIDE.md#常见问题)** - 问题解决方案

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

本项目采用 MIT 许可证。

---

*Synapse Framework - 让开发更简单* 