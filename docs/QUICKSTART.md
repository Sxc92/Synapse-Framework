# Synapse Framework 快速开始指南

> 5分钟快速上手 Synapse Framework，体验企业级开发框架的强大功能！

## 🚀 快速开始

### 1. 环境准备
- **JDK 17+** - Java 开发环境
- **Maven 3.6+** - 构建工具
- **MySQL 8.0+** - 数据库
- **Redis 6.0+** - 缓存服务（可选）

### 2. 添加依赖

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

### 3. 基础配置

详细配置请参考 [配置指南](CONFIGURATION.md)，这里提供基础配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/test_db
    username: root
    password: your_password
    
  redis:
    host: localhost
    port: 6379

sa-token:
  token-name: Authorization
  timeout: 2592000
```

### 4. 创建实体

```java
@Data
@TableName("sys_user")
public class User extends AuditEntity<Long> {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String username;
    private String email;
    private Integer status;
}
```

### 5. 创建 Repository

```java
@AutoRepository
public interface UserRepository extends BaseRepository<User> {
    
    @QueryCondition
    List<User> findByUsername(String username);
}
```

### 6. 创建 Service

```java
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    public User createUser(User user) {
        return userRepository.save(user);
    }
    
    public User getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
```

### 7. 创建 Controller

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
    
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }
    
    @GetMapping
    public Result<List<User>> getAllUsers() {
        return Result.success(userService.getAllUsers());
    }
}
```

## 🎯 框架特性

- **注解驱动** - 通过注解简化开发，减少样板代码
- **智能数据源** - 自动读写分离，支持多数据库
- **统一响应** - 标准化的 API 响应格式
- **权限控制** - Sa-Token 认证，细粒度权限管理
- **缓存支持** - Redis 缓存，分布式锁，会话管理

## 🔧 高级功能

### 缓存使用
```java
@Service
public class UserService {
    
    @Cacheable(value = "users", key = "#id")
    public User getUserById(Long id) {
        return userRepository.findById(id);
    }
}
```

### 权限控制
```java
@RestController
@RequestMapping("/api/admin")
@SaCheckLogin
public class AdminController {
    
    @GetMapping("/users")
    @SaCheckPermission("user:list")
    public Result<List<User>> getUsers() {
        return Result.success(userService.getAllUsers());
    }
}
```

## 🚀 下一步

1. 阅读 [架构设计](ARCHITECTURE.md) 深入理解框架
2. 查看 [模块文档](MODULES/) 了解更多特性
3. 参考 [API参考](API_REFERENCE.md) 完整接口文档
4. 参与社区贡献

恭喜你成功上手 Synapse Framework！🎉

## 📚 相关文档

- [架构设计](ARCHITECTURE.md) - 框架架构详解
- [模块文档](MODULES/) - 各模块详细文档
- [API参考](API_REFERENCE.md) - 完整的API接口文档
- [配置指南](CONFIGURATION.md) - 详细配置说明
