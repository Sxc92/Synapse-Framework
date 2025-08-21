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
- **认证授权** - 用户认证和权限控制
- **JWT 支持** - 无状态认证令牌
- **安全拦截器** - 请求安全过滤
- **权限注解** - 声明式权限控制

### Cache 模块
- **缓存管理** - 统一的缓存接口
- **Redis 集成** - 分布式缓存支持
- **缓存注解** - 声明式缓存操作
- **分布式锁** - 并发控制支持

### Events 模块
- **事件发布** - 异步事件处理
- **事务事件** - 事务相关事件管理
- **事件监听器** - 事件响应处理

## 🗄️ Databases 模块 API

### BaseRepository 接口

#### 基础 CRUD 方法

```java
public interface BaseRepository<T, M extends BaseMapper<T>> extends IService<T> {
    
    // 分页查询 - 支持条件查询
    PageResult<T> pageWithCondition(PageDTO pageDTO);
    
    // 列表查询 - 支持条件查询
    List<T> listWithDTO(PageDTO pageDTO);
    
    // 单条查询 - 支持条件查询
    T getOneWithDTO(PageDTO pageDTO);
    
    // 多表关联分页查询
    PageResult<T> pageWithJoin(JoinPageDTO joinPageDTO);
    
    // 多表关联列表查询
    List<T> listWithJoin(JoinPageDTO joinPageDTO);
}
```

#### 方法说明

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `pageWithCondition` | `PageDTO` | `PageResult<T>` | 单表条件分页查询 |
| `listWithDTO` | `PageDTO` | `List<T>` | 单表条件列表查询 |
| `getOneWithDTO` | `PageDTO` | `T` | 单表条件单条查询 |
| `pageWithJoin` | `JoinPageDTO` | `PageResult<T>` | 多表关联分页查询 |
| `listWithJoin` | `JoinPageDTO` | `List<T>` | 多表关联列表查询 |

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

### 认证接口

```java
public interface AuthenticationService {
    // 用户登录
    LoginResult login(LoginRequest request);
    
    // 用户登出
    void logout(String token);
    
    // 刷新令牌
    String refreshToken(String token);
    
    // 验证令牌
    boolean validateToken(String token);
}
```

### 权限接口

```java
public interface PermissionService {
    // 检查用户权限
    boolean hasPermission(String userId, String permission);
    
    // 获取用户角色
    List<String> getUserRoles(String userId);
    
    // 获取角色权限
    List<String> getRolePermissions(String roleId);
}
```

### 安全注解

```java
// 需要认证
@RequiresAuthentication

// 需要角色
@RequiresRoles("admin")

// 需要权限
@RequiresPermissions("user:read")

// 需要登录
@RequiresLogin
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
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    // 分页查询用户
    public PageResult<User> pageUsers(UserQueryDTO queryDTO) {
        return userRepository.pageWithCondition(queryDTO);
    }
    
    // 多表关联查询
    public PageResult<User> pageUsersWithJoin(UserJoinQueryDTO queryDTO) {
        return userRepository.pageWithJoin(queryDTO);
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

### 安全使用示例

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @RequiresAuthentication
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }
    
    @RequiresPermissions("user:write")
    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.saveUser(user);
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