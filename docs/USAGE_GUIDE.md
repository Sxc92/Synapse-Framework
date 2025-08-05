# Synapse Framework 使用指南

## 快速开始

### 1. 环境准备

#### 必需环境
- **JDK 17+** - Java 开发环境
- **Maven 3.6+** - 构建工具
- **MySQL 8.0+** - 数据库
- **Redis 6.0+** - 缓存服务

#### 可选环境
- **Docker** - 容器化部署
- **IDE** - IntelliJ IDEA 或 Eclipse

### 2. 项目创建

#### 使用 Spring Initializr
1. 访问 [Spring Initializr](https://start.spring.io/)
2. 选择以下配置：
   - **Project**: Maven
   - **Language**: Java
   - **Spring Boot**: 3.2.x
   - **Java**: 17
   - **Packaging**: Jar

#### 手动创建
```xml
<!-- pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.example</groupId>
    <artifactId>my-application</artifactId>
    <version>1.0.0</version>
    
    <properties>
        <java.version>17</java.version>
        <spring-boot.version>3.2.0</spring-boot.version>
    </properties>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
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
        <!-- Spring Boot Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        
        <!-- Synapse Framework -->
        <dependency>
            <groupId>com.indigo</groupId>
            <artifactId>synapse-core</artifactId>
        </dependency>
        <dependency>
            <groupId>com.indigo</groupId>
            <artifactId>synapse-databases</artifactId>
        </dependency>
        <dependency>
            <groupId>com.indigo</groupId>
            <artifactId>synapse-security</artifactId>
        </dependency>
        <dependency>
            <groupId>com.indigo</groupId>
            <artifactId>synapse-cache</artifactId>
        </dependency>
    </dependencies>
</project>
```

### 3. 基础配置

#### application.yml 配置
```yaml
spring:
  application:
    name: my-application
  
  # 数据源配置
  datasource:
    url: jdbc:mysql://localhost:3306/my_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
    
  # Redis 配置
  redis:
    host: localhost
    port: 6379
    password: 
    database: 0
    timeout: 3000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
        max-wait: -1ms

# MyBatis-Plus 配置
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
      id-type: ASSIGN_ID

# Sa-Token 配置
sa-token:
  token-name: Authorization
  timeout: 2592000
  activity-timeout: -1
  is-concurrent: true
  is-share: false
  token-style: uuid
  is-log: false
  jwt-secret-key: your-jwt-secret-key
```

#### 启动类配置
```java
@SpringBootApplication
@MapperScan("com.example.mapper")
public class MyApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

## 核心功能使用

### 1. 实体类定义

#### 基础实体类
```java
@Data
@TableName("users")
public class User extends AuditEntity<Long> {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    @TableField("username")
    private String username;
    
    @TableField("email")
    private String email;
    
    @TableField("phone")
    private String phone;
    
    @TableField("status")
    private Integer status;
    
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
```

#### 查询 DTO
```java
@Data
public class UserQueryDTO {
    
    @QueryCondition(operator = "LIKE")
    private String username;
    
    @QueryCondition(operator = "LIKE")
    private String email;
    
    @QueryCondition(operator = "=")
    private Integer status;
    
    @QueryCondition(operator = "BETWEEN")
    private LocalDateTime createTimeStart;
    
    @QueryCondition(operator = "BETWEEN")
    private LocalDateTime createTimeEnd;
}
```

### 2. Repository 层

#### 基础 Repository
```java
@AutoRepository
public interface UserRepository extends BaseRepository<User> {
    
    // 基于注解的查询方法
    @QueryCondition
    List<User> findByUsernameAndStatus(String username, Integer status);
    
    @QueryCondition
    PageResult<User> pageByStatus(Integer status, PageDTO pageDTO);
    
    // 自定义查询方法
    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(String username);
    
    @Select("SELECT COUNT(*) FROM users WHERE status = #{status}")
    Long countByStatus(Integer status);
}
```

#### 自定义 Repository
```java
@AutoRepository
public interface OrderRepository extends BaseRepository<Order> {
    
    @QueryCondition
    PageResult<Order> pageByUserIdAndStatus(Long userId, Integer status, PageDTO pageDTO);
    
    @QueryCondition(operator = "IN")
    List<Order> findByOrderIds(List<Long> orderIds);
    
    // 复杂查询
    @Select("SELECT o.*, u.username FROM orders o " +
            "LEFT JOIN users u ON o.user_id = u.id " +
            "WHERE o.status = #{status}")
    List<OrderVO> findOrdersWithUser(Integer status);
}
```

### 3. Service 层

#### 基础 Service
```java
@AutoService
public interface UserService extends BaseRepository<User> {
    
    // 业务方法
    User createUser(User user);
    
    User updateUser(User user);
    
    void deleteUser(Long id);
    
    User getUserById(Long id);
    
    PageResult<User> getUsersByPage(UserQueryDTO queryDTO, PageDTO pageDTO);
    
    List<User> getUsersByStatus(Integer status);
}
```

#### Service 实现
```java
@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public User createUser(User user) {
        // 业务逻辑验证
        if (userRepository.findByUsername(user.getUsername()) != null) {
            throw new BusinessException("USER_EXISTS", "用户名已存在");
        }
        
        // 设置默认值
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        
        // 保存用户
        return userRepository.save(user);
    }
    
    @Override
    public User updateUser(User user) {
        User existingUser = userRepository.getById(user.getId());
        if (existingUser == null) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        }
        
        // 更新用户信息
        user.setModifyTime(LocalDateTime.now());
        return userRepository.updateById(user);
    }
    
    @Override
    public void deleteUser(Long id) {
        User user = userRepository.getById(id);
        if (user == null) {
            throw new BusinessException("USER_NOT_FOUND", "用户不存在");
        }
        
        userRepository.removeById(id);
    }
    
    @Override
    public User getUserById(Long id) {
        return userRepository.getById(id);
    }
    
    @Override
    public PageResult<User> getUsersByPage(UserQueryDTO queryDTO, PageDTO pageDTO) {
        return userRepository.pageWithCondition(queryDTO, pageDTO);
    }
    
    @Override
    public List<User> getUsersByStatus(Integer status) {
        return userRepository.findByStatus(status);
    }
}
```

### 4. Controller 层

#### RESTful API
```java
@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping
    public Result<User> createUser(@RequestBody @Valid User user) {
        try {
            User created = userService.createUser(user);
            return Result.success(created);
        } catch (BusinessException e) {
            log.warn("创建用户失败: {}", e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        }
    }
    
    @PutMapping("/{id}")
    public Result<User> updateUser(@PathVariable Long id, @RequestBody @Valid User user) {
        try {
            user.setId(id);
            User updated = userService.updateUser(user);
            return Result.success(updated);
        } catch (BusinessException e) {
            log.warn("更新用户失败: {}", e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return Result.success();
        } catch (BusinessException e) {
            log.warn("删除用户失败: {}", e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        }
    }
    
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return Result.error("USER_NOT_FOUND", "用户不存在");
        }
        return Result.success(user);
    }
    
    @GetMapping
    public Result<PageResult<User>> getUsers(UserQueryDTO queryDTO, PageDTO pageDTO) {
        PageResult<User> users = userService.getUsersByPage(queryDTO, pageDTO);
        return Result.success(users);
    }
    
    @GetMapping("/status/{status}")
    public Result<List<User>> getUsersByStatus(@PathVariable Integer status) {
        List<User> users = userService.getUsersByStatus(status);
        return Result.success(users);
    }
}
```

## 安全功能使用

### 1. 认证配置

#### 登录接口
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private TokenManager tokenManager;
    
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        // 验证用户名密码
        User user = userService.authenticate(request.getUsername(), request.getPassword());
        if (user == null) {
            return Result.error("INVALID_CREDENTIALS", "用户名或密码错误");
        }
        
        // 生成 Token
        String token = tokenManager.login(user.getId());
        
        // 构建响应
        LoginResponse response = LoginResponse.builder()
                .token(token)
                .user(user)
                .build();
        
        return Result.success(response);
    }
    
    @PostMapping("/logout")
    public Result<Void> logout() {
        tokenManager.logout();
        return Result.success();
    }
    
    @GetMapping("/current")
    public Result<User> getCurrentUser() {
        User user = UserContext.getCurrentUser();
        return Result.success(user);
    }
}
```

#### 权限控制
```java
@RestController
@RequestMapping("/api/admin")
@SaCheckLogin
public class AdminController {
    
    @GetMapping("/users")
    @SaCheckPermission("user:list")
    public Result<PageResult<User>> getUsers(PageDTO pageDTO) {
        // 管理员查看用户列表
        return Result.success(userService.getUsersByPage(pageDTO));
    }
    
    @PostMapping("/users")
    @SaCheckPermission("user:create")
    public Result<User> createUser(@RequestBody @Valid User user) {
        return Result.success(userService.createUser(user));
    }
    
    @PutMapping("/users/{id}")
    @SaCheckPermission("user:update")
    public Result<User> updateUser(@PathVariable Long id, @RequestBody @Valid User user) {
        user.setId(id);
        return Result.success(userService.updateUser(user));
    }
    
    @DeleteMapping("/users/{id}")
    @SaCheckPermission("user:delete")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }
}
```

### 2. 权限管理

#### 权限配置
```java
@Component
public class PermissionManager implements StpInterface {
    
    @Autowired
    private UserService userService;
    
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 根据用户ID获取权限列表
        User user = userService.getUserById(Long.valueOf(loginId.toString()));
        if (user == null) {
            return Collections.emptyList();
        }
        
        // 这里可以从数据库或缓存中获取用户权限
        return getUserPermissions(user.getId());
    }
    
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 根据用户ID获取角色列表
        User user = userService.getUserById(Long.valueOf(loginId.toString()));
        if (user == null) {
            return Collections.emptyList();
        }
        
        // 这里可以从数据库或缓存中获取用户角色
        return getUserRoles(user.getId());
    }
    
    private List<String> getUserPermissions(Long userId) {
        // 实现从数据库获取用户权限的逻辑
        return Arrays.asList("user:read", "user:create", "user:update");
    }
    
    private List<String> getUserRoles(Long userId) {
        // 实现从数据库获取用户角色的逻辑
        return Arrays.asList("user", "admin");
    }
}
```

## 缓存功能使用

### 1. 缓存配置

#### 缓存注解使用
```java
@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Cacheable(value = "users", key = "#id")
    @Override
    public User getUserById(Long id) {
        return userRepository.getById(id);
    }
    
    @CacheEvict(value = "users", key = "#user.id")
    @Override
    public User updateUser(User user) {
        return userRepository.updateById(user);
    }
    
    @CacheEvict(value = "users", key = "#id")
    @Override
    public void deleteUser(Long id) {
        userRepository.removeById(id);
    }
    
    @Cacheable(value = "users", key = "'status:' + #status")
    @Override
    public List<User> getUsersByStatus(Integer status) {
        return userRepository.findByStatus(status);
    }
}
```

#### 分布式锁使用
```java
@Service
public class OrderService {
    
    @Autowired
    private DistributedLock distributedLock;
    
    public void processOrder(Long orderId) {
        String lockKey = "order:process:" + orderId;
        
        try {
            // 获取分布式锁
            if (distributedLock.tryLock(lockKey, 30, TimeUnit.SECONDS)) {
                try {
                    // 处理订单逻辑
                    processOrderLogic(orderId);
                } finally {
                    // 释放锁
                    distributedLock.unlock(lockKey);
                }
            } else {
                throw new BusinessException("ORDER_PROCESSING", "订单正在处理中");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("LOCK_TIMEOUT", "获取锁超时");
        }
    }
    
    private void processOrderLogic(Long orderId) {
        // 订单处理逻辑
    }
}
```

## 事件功能使用

### 1. 事件定义

#### 事件类
```java
@Data
@AllArgsConstructor
public class UserCreatedEvent extends BaseEvent {
    
    private User user;
    
    public UserCreatedEvent(User user) {
        super("USER_CREATED");
        this.user = user;
    }
}

@Data
@AllArgsConstructor
public class UserUpdatedEvent extends BaseEvent {
    
    private User user;
    
    public UserUpdatedEvent(User user) {
        super("USER_UPDATED");
        this.user = user;
    }
}
```

#### 事件发布
```java
@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UnifiedPublisher eventPublisher;
    
    @Override
    public User createUser(User user) {
        // 创建用户
        User created = userRepository.save(user);
        
        // 发布用户创建事件
        eventPublisher.publishEvent(new UserCreatedEvent(created));
        
        return created;
    }
    
    @Override
    public User updateUser(User user) {
        // 更新用户
        User updated = userRepository.updateById(user);
        
        // 发布用户更新事件
        eventPublisher.publishEvent(new UserUpdatedEvent(updated));
        
        return updated;
    }
}
```

#### 事件监听
```java
@Component
@Slf4j
public class UserEventListener {
    
    @EventListener
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("用户创建事件: {}", event.getUser().getUsername());
        
        // 发送欢迎邮件
        sendWelcomeEmail(event.getUser());
        
        // 初始化用户数据
        initializeUserData(event.getUser());
    }
    
    @EventListener
    public void handleUserUpdated(UserUpdatedEvent event) {
        log.info("用户更新事件: {}", event.getUser().getUsername());
        
        // 更新缓存
        updateUserCache(event.getUser());
        
        // 记录操作日志
        logUserUpdate(event.getUser());
    }
    
    private void sendWelcomeEmail(User user) {
        // 发送欢迎邮件逻辑
    }
    
    private void initializeUserData(User user) {
        // 初始化用户数据逻辑
    }
    
    private void updateUserCache(User user) {
        // 更新用户缓存逻辑
    }
    
    private void logUserUpdate(User user) {
        // 记录用户更新日志逻辑
    }
}
```

## 高级功能

### 1. 动态数据源

#### 数据源配置
```yaml
# application.yml
spring:
  datasource:
    dynamic:
      primary: master
      strict: false
      datasource:
        master:
          url: jdbc:mysql://localhost:3306/master_db
          username: root
          password: password
          driver-class-name: com.mysql.cj.jdbc.Driver
        slave:
          url: jdbc:mysql://localhost:3306/slave_db
          username: root
          password: password
          driver-class-name: com.mysql.cj.jdbc.Driver
```

#### 数据源切换
```java
@Service
public class UserService {
    
    @DS("master")
    public User createUser(User user) {
        // 在主库创建用户
        return userRepository.save(user);
    }
    
    @DS("slave")
    public List<User> getUsers() {
        // 从从库查询用户列表
        return userRepository.list();
    }
}
```

### 2. 分页查询

#### 分页参数
```java
@Data
public class PageDTO {
    
    @Min(value = 1, message = "页码必须大于0")
    private Integer pageNo = 1;
    
    @Min(value = 1, message = "每页大小必须大于0")
    @Max(value = 1000, message = "每页大小不能超过1000")
    private Integer pageSize = 10;
    
    private String sortField;
    
    private String sortOrder = "ASC";
}
```

#### 分页查询
```java
@GetMapping("/users")
public Result<PageResult<User>> getUsers(UserQueryDTO queryDTO, PageDTO pageDTO) {
    // 分页查询用户
    PageResult<User> users = userService.getUsersByPage(queryDTO, pageDTO);
    
    return Result.success(users);
}
```

### 3. 批量操作

#### 批量插入
```java
@Service
public class UserService {
    
    public List<User> batchCreateUsers(List<User> users) {
        // 批量插入用户
        return userRepository.saveBatch(users);
    }
    
    public boolean batchUpdateUsers(List<User> users) {
        // 批量更新用户
        return userRepository.updateBatchById(users);
    }
    
    public boolean batchDeleteUsers(List<Long> userIds) {
        // 批量删除用户
        return userRepository.removeByIds(userIds);
    }
}
```

## 最佳实践

### 1. 异常处理

#### 统一异常处理
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(ValidationException.class)
    public Result<Void> handleValidationException(ValidationException e) {
        log.warn("参数验证异常: {}", e.getMessage());
        return Result.error("INVALID_PARAMETER", e.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("SYSTEM_ERROR", "系统异常，请稍后重试");
    }
}
```

### 2. 参数验证

#### 实体验证
```java
@Data
public class User {
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20之间")
    private String username;
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
```

### 3. 日志记录

#### 操作日志
```java
@Aspect
@Component
@Slf4j
public class OperationLogAspect {
    
    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint point, OperationLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = point.proceed();
            long endTime = System.currentTimeMillis();
            
            // 记录操作日志
            log.info("操作成功: {}, 耗时: {}ms", operationLog.value(), endTime - startTime);
            
            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            
            // 记录错误日志
            log.error("操作失败: {}, 耗时: {}ms, 错误: {}", 
                operationLog.value(), endTime - startTime, e.getMessage());
            
            throw e;
        }
    }
}
```

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

## 总结

通过本使用指南，你可以快速上手 Synapse Framework，构建高效、安全、可维护的企业级应用。框架提供了丰富的功能和灵活的扩展性，能够满足各种业务需求。

记住以下关键点：
- ✅ **模块化使用**：根据需要选择合适的模块
- ✅ **注解驱动**：充分利用注解简化开发
- ✅ **统一规范**：遵循框架的设计规范
- ✅ **性能优化**：合理使用缓存和分页
- ✅ **安全防护**：正确配置认证和权限

如有问题，请参考文档或提交 Issue。 