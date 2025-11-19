# Synapse Framework API 参考文档

> 完整的 API 参考，包含所有模块的接口、类和方法说明

## 📚 模块概览

### Core 模块
- **配置管理** - 统一的配置管理接口
- **异常处理** - 标准化的异常体系
- **工具类** - 常用工具方法集合
- **国际化** - 多语言支持

### Databases 模块
- **BaseRepository** - 增强的数据访问接口
- **DTO 体系** - 查询和分页数据传输对象
- **动态数据源** - 多数据源管理和切换
- **MyBatis-Plus 集成** - ORM 框架增强

### Security 模块
- **认证授权** - 用户认证和权限控制（自研 TokenService）
- **Token 管理** - Token 生成、验证、续期、撤销
- **滑动过期** - Token 自动续期机制
- **安全拦截器** - 请求安全过滤
- **权限注解** - 声明式权限控制（@RequireLogin、@RequirePermission、@RequireRole）

### Cache 模块
- **缓存管理** - 统一的缓存接口
- **Redis 集成** - 分布式缓存支持
- **缓存注解** - 声明式缓存操作
- **分布式锁** - 并发控制支持

### Events 模块
- **事件发布** - 异步事件处理
- **事务事件** - 事务相关事件管理
- **事件监听器** - 事件响应处理

### I18n 模块
- **消息解析** - 国际化消息解析
- **多语言支持** - 动态语言切换
- **错误消息国际化** - 异常消息多语言

## 🗄️ Databases 模块 API

### BaseRepository 接口

#### 基础 CRUD 方法

```java
public interface BaseRepository<T, M extends BaseMapper<T>> extends IService<T> {
    
    // 分页查询 - 支持条件查询，自动映射到 VO
    <V extends BaseVO> PageResult<V> pageWithDTO(PageDTO pageDTO, Class<V> voClass);
    
    // 列表查询 - 支持条件查询，自动映射到 VO
    <V extends BaseVO> List<V> listWithDTO(QueryDTO queryDTO, Class<V> voClass);
    
    // 单条查询 - 支持条件查询，自动映射到 VO
    <V extends BaseVO> V getOneWithDTO(QueryDTO queryDTO, Class<V> voClass);
    
    // 多表关联分页查询
    <V extends BaseVO> PageResult<V> pageWithJoin(JoinPageDTO joinPageDTO, Class<V> voClass);
    
    // 多表关联列表查询
    <V extends BaseVO> List<V> listWithJoin(JoinPageDTO joinPageDTO, Class<V> voClass);
}
```

#### 方法说明

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `pageWithDTO` | `PageDTO`, `Class<V>` | `PageResult<V>` | 单表条件分页查询，自动映射到 VO |
| `listWithDTO` | `QueryDTO`, `Class<V>` | `List<V>` | 单表条件列表查询，自动映射到 VO |
| `getOneWithDTO` | `QueryDTO`, `Class<V>` | `V` | 单表条件单条查询，自动映射到 VO |
| `pageWithJoin` | `JoinPageDTO`, `Class<V>` | `PageResult<V>` | 多表关联分页查询，自动映射到 VO |
| `listWithJoin` | `JoinPageDTO`, `Class<V>` | `List<V>` | 多表关联列表查询，自动映射到 VO |

**注意**：`BaseRepository` 是一个接口，需要使用 `@Repository` 注解标记，框架会自动生成代理实现。

### DTO 体系

#### PageDTO - 基础分页 DTO

```java
public class PageDTO {
    private Integer pageNo = 1;           // 页码
    private Integer pageSize = 10;        // 页大小
    private List<OrderBy> orderByList;    // 排序列表
    
    // 分页相关方法
    public boolean needPagination();      // 是否需要分页
    public boolean needOrderBy();         // 是否需要排序
    public long getOffset();              // 获取偏移量
}
```

#### JoinPageDTO - 多表关联分页 DTO

```java
public class JoinPageDTO extends PageDTO {
    private List<TableJoin> tableJoins;   // 表关联配置
    private JoinType joinType;            // 关联类型
    private String joinCondition;         // 关联条件
    private List<String> selectFields;    // 选择字段
}
```

#### TableJoin - 表关联配置

```java
public class TableJoin {
    private String tableName;             // 关联表名
    private String alias;                 // 表别名
    private JoinType joinType;            // 关联类型
    private String joinCondition;         // 关联条件
    private List<String> selectFields;    // 选择字段
}
```

#### JoinType - 关联类型枚举

```java
public enum JoinType {
    INNER,      // 内连接
    LEFT,       // 左连接
    RIGHT,      // 右连接
    FULL        // 全连接
}
```

### 配置类

#### MybatisPlusProperties

```java
@ConfigurationProperties(prefix = "synapse.datasource.mybatis-plus")
public class MybatisPlusProperties {
    private Configuration configuration;    // MyBatis 配置
    private GlobalConfig globalConfig;     // 全局配置
    private String typeAliasesPackage;     // 类型别名包
    private String mapperLocations;        // Mapper 位置
}
```

#### DynamicDataSourceProperties

```java
@ConfigurationProperties(prefix = "synapse.datasource.dynamic-data-source")
public class DynamicDataSourceProperties {
    private String primary;               // 主数据源
    private boolean strict;               // 严格模式
    private boolean seata;                // Seata 支持
    private boolean p6spy;                // P6Spy 支持
    private Map<String, DataSourceConfig> datasource; // 数据源配置
}
```

## 🔐 Security 模块 API

### AuthenticationService 接口

```java
public interface AuthenticationService {
    // 认证（支持多种认证方式）
    AuthResponse authenticate(AuthRequest request);
    
    // Token 续期
    AuthResponse renewToken(String token);
    
    // 获取当前用户
    UserContext getCurrentUser();
    
    // 登出
    Result<Void> logout();
}
```

### TokenService 接口

```java
public interface TokenService {
    // 生成 Token
    String generateToken(String userId, UserContext userContext, long expiration);
    
    // 验证 Token
    boolean validateToken(String token);
    
    // 续期 Token
    boolean renewToken(String token, long duration);
    
    // 撤销 Token
    void revokeToken(String token);
    
    // 获取用户上下文
    UserContext getUserContext(String token);
    
    // 获取 Token 剩余时间
    long getTokenRemainingTime(String token);
}
```

### PermissionService 接口

```java
public interface PermissionService {
    // 检查登录
    void checkLogin();
    
    // 检查权限
    void checkPermission(String permission);
    void checkPermission(String[] permissions, Logical logical);
    
    // 检查角色
    void checkRole(String role);
    void checkRole(String[] roles, Logical logical);
    
    // 判断是否有权限
    boolean hasPermission(String permission);
    boolean hasRole(String role);
}
```

### UserContext 工具类

```java
public class UserContext {
    // 获取当前用户
    static UserContext getCurrentUser();
    
    // 获取用户信息
    static String getCurrentUserId();
    static String getCurrentAccount();
    static String getCurrentRealName();
    static String getCurrentEmail();
    static String getCurrentMobile();
    static String getCurrentAvatar();
    
    // 获取角色和权限
    static List<String> getCurrentRoles();
    static List<String> getCurrentPermissions();
    
    // 权限和角色检查
    static boolean hasRole(String role);
    static boolean hasPermission(String permission);
}
```

### 安全注解

```java
// 需要登录
@RequireLogin

// 需要角色
@RequireRole("admin")
@RequireRole(value = {"admin", "super_admin"}, logical = Logical.OR)

// 需要权限
@RequirePermission("user:read")
@RequirePermission(value = {"user:read", "user:write"}, logical = Logical.AND)
```

### Ex 异常工具类

```java
public class Ex {
    // 抛出异常
    static void throwEx(ErrorCode errorCode);
    static void throwEx(ErrorCode errorCode, Object... args);
    static void throwEx(ErrorCode errorCode, Throwable cause);
    static void throwEx(ErrorCode errorCode, Throwable cause, Object... args);
    
    // 创建异常（不抛出）
    static SynapseException of(ErrorCode errorCode);
}
```

## 🗃️ Cache 模块 API

### 缓存接口

```java
public interface CacheService {
    // 设置缓存
    void set(String key, Object value, long timeout);
    
    // 获取缓存
    <T> T get(String key, Class<T> clazz);
    
    // 删除缓存
    void delete(String key);
    
    // 清空缓存
    void clear();
}
```

### 缓存注解

```java
// 缓存查询结果
@Cacheable(value = "users", key = "#id")

// 更新缓存
@CachePut(value = "users", key = "#user.id")

// 删除缓存
@CacheEvict(value = "users", key = "#id")

// 条件缓存
@Cacheable(value = "users", condition = "#id > 0")
```

### 分布式锁

```java
public interface DistributedLockService {
    // 获取锁
    boolean tryLock(String key, long timeout);
    
    // 释放锁
    void releaseLock(String key);
    
    // 检查锁状态
    boolean isLocked(String key);
}
```

## 📡 Events 模块 API

### 事件发布

```java
public interface EventPublisher {
    // 发布事件
    void publishEvent(Object event);
    
    // 发布事务事件
    void publishTransactionEvent(Object event);
    
    // 异步发布事件
    void publishEventAsync(Object event);
}
```

### 事件监听器

```java
// 事件监听器注解
@EventListener

// 事务事件监听器
@TransactionalEventListener

// 异步事件监听器
@AsyncEventListener
```

## 🛠️ Core 模块 API

### 配置管理

```java
public interface ConfigurationService {
    // 获取配置值
    String getProperty(String key);
    
    // 获取配置值（带默认值）
    String getProperty(String key, String defaultValue);
    
    // 获取配置值（类型转换）
    <T> T getProperty(String key, Class<T> clazz);
}
```

### 异常处理

```java
// 基础异常
public abstract class BaseException extends RuntimeException

// 业务异常
public class BusinessException extends BaseException

// 系统异常
public class SystemException extends BaseException

// 验证异常
public class ValidationException extends BaseException
```

### 工具类

```java
// 字符串工具
public class StringUtils

// 日期工具
public class DateUtils

// 加密工具
public class CryptoUtils

// JSON 工具
public class JsonUtils
```

## 📖 使用示例

### 基础查询示例

```java
@Repository
public interface UserRepository extends BaseRepository<User> {
    
    // 使用 @QueryCondition 自动构建查询条件
    @QueryCondition
    List<UserVO> findByUsername(String username);
    
    // 分页查询，自动映射到 VO
    PageResult<UserVO> pageUsers(UserPageDTO pageDTO);
}

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    // 分页查询用户（自动映射到 VO）
    public PageResult<UserVO> pageUsers(UserPageDTO pageDTO) {
        return userRepository.pageWithDTO(pageDTO, UserVO.class);
    }
    
    // 条件查询（自动映射到 VO）
    public List<UserVO> findUsers(UserQueryDTO queryDTO) {
        return userRepository.listWithDTO(queryDTO, UserVO.class);
    }
}
```

### 缓存使用示例

```java
@Service
public class UserService {
    
    @Cacheable(value = "users", key = "#id")
    public User getUserById(Long id) {
        return userRepository.getById(id);
    }
    
    @CachePut(value = "users", key = "#user.id")
    public User saveUser(User user) {
        return userRepository.save(user);
    }
}
```

### 异常处理示例

```java
@Service
public class UserService {
    
    public UserVO getUser(String id) {
        if (id == null || id.isEmpty()) {
            Ex.throwEx(StandardErrorCode.USER_ID_REQUIRED, "用户ID不能为空");
        }
        
        User user = userRepository.getById(id);
        if (user == null) {
            Ex.throwEx(StandardErrorCode.USER_NOT_FOUND, id);
        }
        
        return VoMapper.toVO(user, UserVO.class);
    }
}
```

### 安全使用示例

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @PostMapping("/login")
    public Result<AuthResponse> login(@RequestBody LoginRequest request) {
        // 构建认证请求
        AuthRequest authRequest = AuthRequest.builder()
            .authType(AuthRequest.AuthType.USERNAME_PASSWORD)
            .usernamePasswordAuth(UsernamePasswordAuth.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .build())
            .userId(user.getId().toString())
            .realName(user.getRealName())
            .email(user.getEmail())
            .mobile(user.getMobile())
            .avatar(user.getAvatar())
            .roles(roles)
            .permissions(permissions)
            .build();
        
        AuthResponse response = authenticationService.authenticate(authRequest);
        return Result.success(response);
    }
    
    @RequireLogin
    @GetMapping("/{id}")
    public Result<UserVO> getUser(@PathVariable String id) {
        UserVO user = userService.getUser(id);
        return Result.success(user);
    }
    
    @RequirePermission("user:read")
    @GetMapping
    public Result<List<UserVO>> getUsers() {
        List<UserVO> users = userService.getUsers();
        return Result.success(users);
    }
    
    @RequireRole("admin")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return Result.success();
    }
}
```

## 🔧 配置参考

### 完整配置示例

```yaml
synapse:
  datasource:
    mybatis-plus:
      configuration:
        map-underscore-to-camel-case: true
        log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
      global-config:
        banner: false
        enable-pagination: true
      type-aliases-package: com.indigo.**.entity
      mapper-locations: "classpath*:mapper/**/*.xml"
    
    dynamic-data-source:
      primary: master
      strict: false
      seata: false
      p6spy: false
      datasource:
        master:
          type: MYSQL
          host: localhost
          port: 3306
          database: synapse_iam
          username: root
          password: your_password
          pool-type: HIKARI
          hikari:
            minimum-idle: 5
            maximum-pool-size: 15
            connection-timeout: 30000

  security:
    jwt:
      secret: your-secret-key
      expiration: 86400000
      header: Authorization
    
  cache:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 3000
```

## 📝 注意事项

1. **配置前缀**: 使用 `synapse` 作为配置前缀
2. **依赖管理**: 通过 `synapse-bom` 管理版本
3. **自动配置**: 大部分功能支持自动配置
4. **向后兼容**: 保持与标准 Spring Boot 配置的兼容性

## 🚀 下一步

- 查看 [使用指南](USAGE_GUIDE.md) 了解详细用法
- 参考 [快速开始](QUICKSTART.md) 快速上手
- 探索 [最佳实践](BEST_PRACTICES.md) 学习最佳实践 