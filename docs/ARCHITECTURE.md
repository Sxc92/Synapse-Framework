# Synapse Framework 架构设计文档

## 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        Synapse Framework                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │   synapse-  │  │   synapse-  │  │   synapse-  │            │
│  │    core     │  │ databases   │  │  security   │            │
│  │             │  │             │  │             │            │
│  │ • Result    │  │ • BaseRepo  │  │ • TokenSvc  │            │
│  │ • Exception │  │ • Query     │  │ • Auth      │            │
│  │ • Context   │  │ • Entity    │  │ • Session   │            │
│  │ • Utils     │  │ • Proxy     │  │ • Permission│            │
│  │ • I18n      │  │ • VoMapper  │  │ • Renewal   │            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │   synapse-  │  │   synapse-  │  │   synapse-  │            │
│  │    cache    │  │   events    │  │     i18n    │            │
│  │             │  │             │  │             │            │
│  │ • Redis     │  │ • Publisher │  │ • Message   │            │
│  │ • Session   │  │ • Consumer  │  │ • Resolver  │            │
│  │ • Lock      │  │ • Event     │  │ • Locale    │            │
│  │ • Cache     │  │ • Config    │  │             │            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
│                                                                 │
│  ┌─────────────┐                                               │
│  │   synapse-  │                                               │
│  │     bom     │                                               │
│  │             │                                               │
│  │ • Versions  │                                               │
│  │ • Dependencies│                                             │
│  │ • Management│                                               │
│  └─────────────┘                                               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                      │
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │   Web Layer │  │ Service     │  │ Repository  │            │
│  │             │  │ Layer       │  │ Layer       │            │
│  │ • Controller│  │ • Business  │  │ • Data      │            │
│  │ • Filter    │  │ • Logic     │  │ • Access    │            │
│  │ • Interceptor│ │ • Validation│  │ • Query     │            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Infrastructure Layer                         │
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │   Database  │  │    Cache    │  │   Security  │            │
│  │             │  │             │  │             │            │
│  │ • MySQL     │  │ • Redis     │  │ • TokenSvc  │            │
│  │ • MyBatis+  │  │ • Caffeine  │  │ • Permission│            │
│  │ • Dynamic   │  │ • Lock      │  │ • OAuth2    │            │
│  │   DS        │  │             │  │             │            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 模块依赖关系

```
synapse-bom
    │
    ├── synapse-core (基础依赖)
    │   ├── Spring Boot
    │   ├── Spring WebFlux
    │   └── Lombok
    │
    ├── synapse-databases (依赖 core)
    │   ├── MyBatis-Plus
    │   ├── Dynamic Datasource
    │   ├── HikariCP/Druid
    │   └── synapse-core
    │
    ├── synapse-security (依赖 core, cache)
    │   ├── TokenService (自研)
    │   ├── PermissionService (自研)
    │   ├── synapse-core
    │   └── synapse-cache
    │
    ├── synapse-cache (依赖 core)
    │   ├── Spring Data Redis
    │   ├── Caffeine
    │   └── synapse-core
    │
    ├── synapse-events (依赖 core)
    │   ├── Spring Events
    │   └── synapse-core
    │
    └── synapse-i18n (依赖 core, cache)
        ├── I18nMessageResolver
        ├── synapse-core
        └── synapse-cache
```

## 核心设计模式

### 1. 代理模式 (Proxy Pattern)

**应用场景**：`SqlMethodInterceptor`
```java
// 代理 BaseRepository 接口
public class SqlMethodInterceptor implements InvocationHandler {
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        // 拦截方法调用
        if (isBaseRepositoryMethod(method)) {
            return handleBaseRepositoryMethod(proxy, method, args);
        }
        // 委托给实际的实现
        return callMapperMethod(method, args);
    }
}
```

### 2. 策略模式 (Strategy Pattern)

**应用场景**：认证策略
```java
// 认证服务接口
public interface AuthenticationService {
    AuthResponse authenticate(AuthRequest request);
    AuthResponse renewToken(String token);
}

// 默认认证服务实现
public class DefaultAuthenticationService implements AuthenticationService {
    private final TokenService tokenService;
    
    @Override
    public AuthResponse authenticate(AuthRequest request) {
        // 使用自研 TokenService 处理认证
        String token = tokenService.generateToken(userId, userContext, expiration);
        return AuthResponse.of(token, null, expiration);
    }
}
```

### 3. 模板方法模式 (Template Method Pattern)

**应用场景**：查询构建器
```java
// 查询构建器基类
public abstract class AbstractQueryBuilder {
    
    // 模板方法
    public final QueryWrapper<?> buildQuery(Object queryDTO) {
        QueryWrapper<?> wrapper = new QueryWrapper<>();
        
        // 1. 解析注解
        parseAnnotations(queryDTO, wrapper);
        
        // 2. 构建条件
        buildConditions(queryDTO, wrapper);
        
        // 3. 应用排序
        applySorting(queryDTO, wrapper);
        
        return wrapper;
    }
    
    // 抽象方法，由子类实现
    protected abstract void parseAnnotations(Object queryDTO, QueryWrapper<?> wrapper);
    protected abstract void buildConditions(Object queryDTO, QueryWrapper<?> wrapper);
    protected abstract void applySorting(Object queryDTO, QueryWrapper<?> wrapper);
}
```

### 4. 观察者模式 (Observer Pattern)

**应用场景**：事件驱动
```java
// 事件发布器
public class UnifiedPublisher {
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public void publishEvent(BaseEvent event) {
        // 发布事件
        eventPublisher.publishEvent(event);
    }
}

// 事件监听器
@Component
public class UserEventListener {
    
    @EventListener
    public void handleUserCreated(UserCreatedEvent event) {
        // 处理用户创建事件
    }
}
```

### 5. 工厂模式 (Factory Pattern)

**应用场景**：数据源工厂
```java
// 数据源工厂
public class DataSourceFactory {
    
    public static DataSource createDataSource(DataSourceConfig config) {
        switch (config.getType()) {
            case MYSQL:
                return createMySQLDataSource(config);
            case POSTGRESQL:
                return createPostgreSQLDataSource(config);
            default:
                throw new IllegalArgumentException("Unsupported data source type");
        }
    }
}
```

## 数据库模块架构

### 1. 配置架构设计

**配置类层次结构**
```
SynapseDataSourceProperties
├── primary: String                    # 主数据源名称
├── mybatisPlus: MybatisPlus          # MyBatis-Plus配置
│   ├── configuration: Configuration   # MyBatis配置
│   └── globalConfig: GlobalConfig    # 全局配置
├── dynamicDataSource: DynamicDataSource # 动态数据源配置
│   ├── strict: boolean               # 严格模式
│   ├── seata: boolean                # Seata分布式事务
│   ├── p6spy: boolean                # P6Spy监控
│   └── datasource: Map<String, DataSourceConfig> # 数据源映射
└── springDatasource: SpringDatasource # 兼容性配置
    └── dynamic: Dynamic              # 标准Spring Boot格式
        ├── primary: String           # 主数据源名称
        └── datasource: Map<String, SpringDataSourceConfig> # 数据源配置
```

**数据源配置结构**
```
DataSourceConfig
├── type: DatabaseType                 # 数据库类型 (MYSQL, POSTGRESQL, ORACLE, SQLSERVER, H2)
├── host: String                       # 主机地址
├── port: Integer                      # 端口号
├── database: String                   # 数据库名
├── username: String                   # 用户名
├── password: String                   # 密码
├── poolType: PoolType                 # 连接池类型 (HIKARI, DRUID)
├── params: Map<String, String>        # 连接参数
├── hikari: HikariConfig               # HikariCP配置
└── druid: DruidConfig                 # Druid配置
```

### 2. 智能数据源路由架构

**路由机制**
```
Application Request
    │
    ▼
┌─────────────────┐
│ DynamicRouting  │ ← 动态路由数据源
│   DataSource    │
└─────────────────┘
    │
    ▼
┌─────────────────┐
│ AutoDataSource  │ ← 自动数据源拦截器
│  Interceptor    │   (根据SQL类型自动选择)
└─────────────────┘
    │
    ▼
┌─────────────────┐
│ Context Holder  │ ← 数据源上下文持有者
└─────────────────┘
    │
    ▼
┌─────────────────┐
│ Target DataSource│ ← 目标数据源选择
└─────────────────┘
    │
    ├─ master ──┐
    │           ▼
    │  ┌─────────────────┐
    │  │  Master DS      │ ← 主数据源 (写操作)
    │  └─────────────────┘
    │
    ├─ slave1 ──┐
    │           ▼
    │  ┌─────────────────┐
    │  │  Slave1 DS      │ ← 从数据源1 (读操作)
    │  └─────────────────┘
    │
    └─ slave2 ──┐
                ▼
        ┌─────────────────┐
        │  Slave2 DS      │ ← 从数据源2 (读操作)
        └─────────────────┘
```

**智能路由策略**
- **SELECT语句** → 自动路由到从库（轮询负载均衡）
- **INSERT/UPDATE/DELETE语句** → 自动路由到主库
- **编程式切换** → 支持精确控制数据源选择

**自动配置流程**
```
Spring Boot Startup
    │
    ▼
┌─────────────────┐
│ AutoConfiguration│ ← 自动配置类
└─────────────────┘
    │
    ▼
┌─────────────────┐
│ Properties      │ ← 配置属性绑定
│   Binding       │
└─────────────────┘
    │
    ▼
┌─────────────────┐
│ DataSource      │ ← 数据源创建
│   Creation      │
└─────────────────┘
    │
    ▼
┌─────────────────┐
│ Dynamic Routing │ ← 动态路由设置
│   Setup         │
└─────────────────┘
    │
    ▼
┌─────────────────┐
│ MyBatis-Plus   │ ← MyBatis-Plus配置
│   Configuration │
└─────────────────┘
```

### 3. 连接池架构

**HikariCP配置**
```
HikariConfig
├── minimumIdle: Integer               # 最小空闲连接数
├── maximumPoolSize: Integer           # 最大连接池大小
├── idleTimeout: Long                  # 空闲超时时间
├── maxLifetime: Long                  # 最大生命周期
├── connectionTimeout: Long            # 连接超时时间
├── connectionTestQuery: String        # 连接测试查询
├── connectionInitSql: String          # 连接初始化SQL
├── validationTimeout: Long            # 验证超时时间
├── leakDetectionThreshold: Long       # 连接泄漏检测阈值
└── registerMbeans: boolean           # 是否注册MBean
```

**Druid配置**
```
DruidConfig
├── initialSize: Integer                # 初始连接数
├── minIdle: Integer                    # 最小空闲连接数
├── maxActive: Integer                  # 最大活跃连接数
├── maxWait: Long                       # 最大等待时间
├── timeBetweenEvictionRunsMillis: Long # 空闲连接检测间隔
├── minEvictableIdleTimeMillis: Long   # 最小空闲时间
├── maxEvictableIdleTimeMillis: Long   # 最大空闲时间
├── validationQuery: String             # 验证查询
├── testWhileIdle: boolean             # 空闲时测试
├── testOnBorrow: boolean              # 借用时测试
├── testOnReturn: boolean              # 归还时测试
├── poolPreparedStatements: boolean    # 池化预处理语句
├── maxPoolPreparedStatementPerConnectionSize: Integer # 每连接最大预处理语句数
└── filters: String                     # 过滤器配置
```

## 数据流架构

### 1. 请求处理流程

```
HTTP Request
    │
    ▼
┌─────────────────┐
│   Controller    │ ← 接收请求
└─────────────────┘
    │
    ▼
┌─────────────────┐
│   Service       │ ← 业务逻辑
└─────────────────┘
    │
    ▼
┌─────────────────┐
│  Repository     │ ← 数据访问
└─────────────────┘
    │
    ▼
┌─────────────────┐
│   Database      │ ← 数据存储
└─────────────────┘
    │
    ▼
┌─────────────────┐
│   Result<T>     │ ← 统一响应
└─────────────────┘
    │
    ▼
HTTP Response
```

### 2. 认证授权流程

```
HTTP Request (with Token)
    │
    ▼
┌─────────────────┐
│  Auth Filter    │ ← 提取 Token
└─────────────────┘
    │
    ▼
┌─────────────────┐
│ Token Validator │ ← 验证 Token
└─────────────────┘
    │
    ▼
┌─────────────────┐
│ User Context    │ ← 设置用户上下文
└─────────────────┘
    │
    ▼
┌─────────────────┐
│ Permission Check│ ← 权限检查
└─────────────────┘
    │
    ▼
┌─────────────────┐
│   Controller    │ ← 执行业务逻辑
└─────────────────┘
```

### 3. 缓存处理流程

```
Data Access Request
    │
    ▼
┌─────────────────┐
│  Cache Layer    │ ← 检查缓存
└─────────────────┘
    │
    ▼
┌─────────────────┐
│ Cache Hit?      │ ← 缓存命中？
└─────────────────┘
    │
    ├─ Yes ──┐
    │        ▼
    │  ┌─────────────────┐
    │  │ Return Cached   │ ← 返回缓存数据
    │  │     Data        │
    │  └─────────────────┘
    │
    └─ No ───┐
             ▼
    ┌─────────────────┐
    │   Database      │ ← 查询数据库
    └─────────────────┘
             │
             ▼
    ┌─────────────────┐
    │ Update Cache    │ ← 更新缓存
    └─────────────────┘
             │
             ▼
    ┌─────────────────┐
    │ Return Data     │ ← 返回数据
    └─────────────────┘
```

### 4. 会话管理流程

```
User Authentication
    │
    ▼
┌─────────────────┐
│ UserSessionService│ ← 会话管理门面
└─────────────────┘
    │
    ▼
┌─────────────────┐
│ SessionManager  │ ← 会话管理接口
└─────────────────┘
    │
    ▼
┌─────────────────┐
│DefaultSession   │ ← 会话管理实现
│   Manager      │
└─────────────────┘
    │
    ▼
┌─────────────────┐
│  CacheService   │ ← 缓存服务
└─────────────────┘
    │
    ▼
┌─────────────────┐
│  RedisService   │ ← Redis操作
└─────────────────┘
    │
    ▼
┌─────────────────┐
│     Redis       │ ← 数据存储
└─────────────────┘
```

## 扩展点设计

### 1. 自定义查询构建器

```java
// 自定义查询构建器
@Component
public class CustomQueryBuilder extends AbstractQueryBuilder {
    
    @Override
    protected void parseAnnotations(Object queryDTO, QueryWrapper<?> wrapper) {
        // 自定义注解解析逻辑
    }
    
    @Override
    protected void buildConditions(Object queryDTO, QueryWrapper<?> wrapper) {
        // 自定义条件构建逻辑
    }
    
    @Override
    protected void applySorting(Object queryDTO, QueryWrapper<?> wrapper) {
        // 自定义排序逻辑
    }
}
```

### 2. 自定义认证策略

```java
// 自定义认证策略
@Component
public class CustomAuthenticationStrategy implements AuthenticationStrategy {
    
    @Override
    public UserContext authenticate(String token) {
        // 自定义认证逻辑
        return userContext;
    }
}
```

### 3. 自定义事件处理器

```java
// 自定义事件处理器
@Component
public class CustomEventHandler {
    
    @EventListener
    public void handleCustomEvent(CustomEvent event) {
        // 自定义事件处理逻辑
    }
}
```

### 4. 自定义会话管理器

```java
// 自定义会话管理器
@Component
public class CustomSessionManager implements SessionManager {
    
    @Override
    public void createSession(String userId, long expireSeconds) {
        // 自定义会话创建逻辑
    }
    
    @Override
    public void storeToken(String token, String userId, long expireSeconds) {
        // 自定义token存储逻辑
    }
    
    // 实现其他接口方法...
}
```

## 性能优化策略

### 1. 数据库优化

- **连接池配置**：合理配置数据库连接池大小
- **索引优化**：为常用查询字段添加索引
- **分页查询**：大数据量查询使用分页
- **批量操作**：批量插入和更新操作

### 2. 缓存优化

- **多级缓存**：本地缓存 + Redis 缓存
- **缓存预热**：系统启动时预加载热点数据
- **缓存更新**：及时更新缓存数据
- **缓存穿透**：防止缓存穿透攻击
- **会话缓存**：用户会话和token的智能缓存管理
- **分布式会话**：支持集群环境下的会话共享

### 3. 并发优化

- **异步处理**：使用异步处理非关键路径
- **线程池**：合理配置线程池参数
- **分布式锁**：防止并发冲突
- **限流控制**：防止系统过载

## 安全设计

### 1. 认证安全

- **Token 管理**：安全的 Token 生成和验证
- **会话管理**：安全的会话存储和清理
- **会话超时**：智能的会话超时和续期机制
- **并发会话控制**：防止同一用户多设备同时登录
- **密码安全**：密码加密和验证
- **多因素认证**：支持多因素认证

### 2. 授权安全

- **权限控制**：细粒度的权限控制
- **角色管理**：灵活的角色管理
- **资源保护**：敏感资源访问控制
- **审计日志**：操作审计和日志记录

### 3. 数据安全

- **数据加密**：敏感数据加密存储
- **SQL 注入防护**：参数化查询
- **XSS 防护**：输入输出过滤
- **CSRF 防护**：跨站请求伪造防护

## 监控和运维

### 1. 应用监控

- **性能监控**：接口响应时间监控
- **错误监控**：异常和错误监控
- **业务监控**：关键业务指标监控
- **资源监控**：CPU、内存、磁盘监控

### 2. 日志管理

- **结构化日志**：统一的日志格式
- **日志级别**：合理的日志级别配置
- **日志聚合**：集中式日志收集
- **日志分析**：日志分析和告警

### 3. 部署运维

- **容器化部署**：Docker 容器化部署
- **自动化部署**：CI/CD 自动化部署
- **配置管理**：统一的配置管理
- **版本管理**：版本控制和回滚

## 总结

Synapse Framework 采用模块化、可扩展的架构设计，通过多种设计模式的组合，提供了灵活、高效、安全的开发框架。框架的核心优势包括：

- ✅ **模块化设计**：高内聚、低耦合的模块架构
- ✅ **扩展性强**：支持自定义扩展和插件
- ✅ **性能优化**：多层次的性能优化策略
- ✅ **安全可靠**：完善的安全防护机制
- ✅ **易于维护**：清晰的代码结构和文档

这种架构设计确保了框架的稳定性、可维护性和可扩展性，为企业的微服务架构提供了强有力的支撑。 