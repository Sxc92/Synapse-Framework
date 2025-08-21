# Synapse Framework

一个基于 Spring Boot 3.x 的企业级微服务框架，专注于简化开发流程、提高代码质量和系统性能。

## 🚀 核心特性

- ✅ **模块化设计** - 高内聚、低耦合的模块架构
- ✅ **注解驱动** - 通过注解简化开发，减少样板代码
- ✅ **智能数据源** - 自动读写分离，支持多数据库，智能故障转移
- ✅ **统一响应** - 标准化的 API 响应格式
- ✅ **权限控制** - Sa-Token 认证，细粒度权限管理
- ✅ **缓存支持** - Redis 缓存，分布式锁，会话管理
- ✅ **事件驱动** - 异步事件处理机制
- ✅ **性能优化** - 连接池优化，多级缓存，智能路由
- ✅ **配置验证** - 启动时自动验证配置完整性和数据源连接性
- ✅ **健康监控** - 集成 Spring Boot 健康检查，实时监控数据源状态

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

- **Spring Boot 3.2.3** - 应用框架
- **Spring Cloud 2023.0.0** - 微服务生态
- **MyBatis-Plus 3.5.8** - ORM 框架
- **Sa-Token 1.38.0** - 认证框架
- **Redis** - 缓存和会话管理
- **MySQL/PostgreSQL/Oracle** - 多数据库支持
- **Java 17** - 运行环境

## 📦 模块说明

| 模块 | 描述 | 主要功能 |
|------|------|----------|
| **synapse-core** | 核心模块 | 统一响应、异常处理、工具类、国际化 |
| **synapse-databases** | 数据库模块 | 增强 Repository、查询构建器、智能数据源路由、读写分离、负载均衡、故障转移 |
| **synapse-security** | 安全模块 | Sa-Token 认证、权限控制、会话管理 |
| **synapse-cache** | 缓存模块 | Redis 缓存、分布式锁、会话缓存 |
| **synapse-events** | 事件模块 | 事件驱动、异步处理、事务事件 |

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
synapse:
  datasource:
    primary: master
    
    # 读写分离配置
    read-write:
      enabled: true
      read-sources: [slave1, slave2]
      write-sources: [master]
    
    # 负载均衡配置
    load-balance:
      strategy: ROUND_ROBIN
    
    # 故障转移配置
    failover:
      enabled: true
      timeout: 5000
      max-retries: 3
    
    # 数据源配置
    datasources:
      master:
        type: MYSQL
        host: localhost
        port: 3306
        database: synapse_demo
        username: root
        password: 123456
        role: WRITE
        
        pool:
          type: HIKARI
          min-idle: 5
          max-size: 20
          connection-timeout: 30000
          idle-timeout: 600000
          max-lifetime: 1800000
          connection-test-query: SELECT 1
          leak-detection-threshold: 60000
          
      slave1:
        type: MYSQL
        host: localhost
        port: 3307
        database: synapse_demo
        username: root
        password: 123456
        role: READ
        
        pool:
          type: HIKARI
          min-idle: 5
          max-size: 15
          
      slave2:
        type: MYSQL
        host: localhost
        port: 3308
        database: synapse_demo
        username: root
        password: 123456
        role: READ
        
        pool:
          type: HIKARI
          min-idle: 5
          max-size: 15
```

### 3. 创建实体和 Repository

```java
@Data
@TableName("sys_user")
public class User extends AuditEntity<Long> {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String username;
    
    private String email;
}

@AutoRepository
public interface UserRepository extends BaseRepository<User> {
    
    @QueryCondition
    List<User> findByUsername(String username);
}
```

### 4. 创建 Controller

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

## 🔧 开发环境要求

- **JDK 17+**
- **Maven 3.6+**
- **MySQL 8.0+** 或 **PostgreSQL 12+**
- **Redis 6.0+**

## 🆕 最新优化特性

### **配置优化**
- ✅ **统一配置入口** - 消除双配置问题，所有配置集中在 `synapse.datasource` 下
- ✅ **智能配置验证** - 启动时自动检查配置完整性和数据源连接性
- ✅ **配置模板化** - 提供开发、生产、高并发等场景的配置模板

### **数据源路由优化**
- ✅ **智能读写分离** - 根据SQL类型自动选择读/写数据源
- ✅ **多种负载均衡策略** - 支持轮询、权重、随机等策略
- ✅ **智能故障转移** - 支持多种故障转移策略，自动健康检查
- ✅ **性能优化** - 减少对象创建，优化路由逻辑

### **监控和运维**
- ✅ **健康检查集成** - 集成 Spring Boot 健康检查机制
- ✅ **实时状态监控** - 监控数据源健康状态和性能指标
- ✅ **故障告警** - 自动检测和报告数据源故障

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