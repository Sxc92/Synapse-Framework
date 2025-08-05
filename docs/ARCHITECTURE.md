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
│  │ • Result    │  │ • BaseRepo  │  │ • Sa-Token  │            │
│  │ • Exception │  │ • Query     │  │ • Auth      │            │
│  │ • Context   │  │ • Entity    │  │ • Session   │            │
│  │ • Utils     │  │ • Proxy     │  │ • Permission│            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
│                                                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │   synapse-  │  │   synapse-  │  │   synapse-  │            │
│  │    cache    │  │   events    │  │     bom     │            │
│  │             │  │             │  │             │            │
│  │ • Redis     │  │ • Publisher │  │ • Versions  │            │
│  │ • Session   │  │ • Consumer  │  │ • Dependencies│          │
│  │ • Lock      │  │ • Event     │  │ • Management│            │
│  │ • Cache     │  │ • Config    │  │             │            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
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
│  │ • MySQL     │  │ • Redis     │  │ • Sa-Token  │            │
│  │ • MyBatis+  │  │ • Session   │  │ • JWT       │            │
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
    │   └── synapse-core
    │
    ├── synapse-security (依赖 core, cache)
    │   ├── Sa-Token
    │   ├── synapse-core
    │   └── synapse-cache
    │
    ├── synapse-cache (依赖 core)
    │   ├── Spring Data Redis
    │   └── synapse-core
    │
    └── synapse-events (依赖 core)
        ├── Spring Events
        └── synapse-core
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
// 认证策略接口
public interface AuthenticationStrategy {
    UserContext authenticate(String token);
}

// Sa-Token 认证策略
public class SaTokenAuthenticationStrategy implements AuthenticationStrategy {
    @Override
    public UserContext authenticate(String token) {
        // Sa-Token 认证逻辑
    }
}

// OAuth2 认证策略
public class OAuth2AuthenticationStrategy implements AuthenticationStrategy {
    @Override
    public UserContext authenticate(String token) {
        // OAuth2 认证逻辑
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

### 3. 并发优化

- **异步处理**：使用异步处理非关键路径
- **线程池**：合理配置线程池参数
- **分布式锁**：防止并发冲突
- **限流控制**：防止系统过载

## 安全设计

### 1. 认证安全

- **Token 管理**：安全的 Token 生成和验证
- **会话管理**：安全的会话存储和清理
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