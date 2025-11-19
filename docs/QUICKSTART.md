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
          database: test_db
          username: root
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
    mode: STRICT
    token:
      timeout: 7200
      enable-sliding-expiration: true
      refresh-threshold: 600
      renewal-duration: 7200
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
@Repository
public interface UserRepository extends BaseRepository<User> {
    
    // 使用 @QueryCondition 自动构建查询条件
    @QueryCondition
    List<UserVO> findByUsername(String username);
    
    // 分页查询，自动映射到 VO
    PageResult<UserVO> pageUsers(UserPageDTO pageDTO);
}
```

### 6. 创建 Service

```java
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    public UserVO createUser(CreateUserDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        userRepository.save(user);
        return VoMapper.toVO(user, UserVO.class);
    }
    
    public UserVO getUserById(Long id) {
        User user = userRepository.getById(id);
        return VoMapper.toVO(user, UserVO.class);
    }
    
    public PageResult<UserVO> getAllUsers(UserPageDTO pageDTO) {
        return userRepository.pageUsers(pageDTO);
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
    @RequirePermission("user:create")
    public Result<UserVO> createUser(@RequestBody @Valid CreateUserDTO dto) {
        UserVO user = userService.createUser(dto);
        return Result.success(user);
    }
    
    @GetMapping("/{id}")
    @RequireLogin
    public Result<UserVO> getUserById(@PathVariable Long id) {
        UserVO user = userService.getUserById(id);
        return Result.success(user);
    }
    
    @GetMapping("/page")
    @RequireLogin
    public Result<PageResult<UserVO>> getAllUsers(UserPageDTO pageDTO) {
        PageResult<UserVO> result = userService.getAllUsers(pageDTO);
        return Result.success(result);
    }
}
```

## 🎯 框架特性

- **注解驱动** - 通过注解简化开发，减少样板代码
- **智能数据源** - 自动读写分离，支持多数据库
- **统一响应** - 标准化的 API 响应格式 `Result<T>`
- **异常处理** - 统一的异常处理机制 `Ex.throwEx()`
- **权限控制** - 基于注解的权限验证（@RequireLogin、@RequirePermission、@RequireRole）
- **Token 认证** - 自研 TokenService，支持滑动过期、自动续期
- **缓存支持** - 二级缓存（Caffeine + Redis），分布式锁，会话管理
- **VO 映射** - 自动字段映射，支持数据库字段到 VO 的转换

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
@RequireLogin
public class AdminController {
    
    @GetMapping("/users")
    @RequirePermission("user:list")
    public Result<PageResult<UserVO>> getUsers(UserPageDTO pageDTO) {
        PageResult<UserVO> result = userService.getAllUsers(pageDTO);
        return Result.success(result);
    }
    
    @GetMapping("/admin-only")
    @RequireRole("admin")
    public Result<String> adminOnly() {
        return Result.success("管理员专用接口");
    }
}
```

### 异常处理
```java
@Service
public class UserService {
    
    public UserVO getUserById(Long id) {
        User user = userRepository.getById(id);
        if (user == null) {
            // 使用 Ex.throwEx() 统一异常处理
            Ex.throwEx(StandardErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        return VoMapper.toVO(user, UserVO.class);
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
