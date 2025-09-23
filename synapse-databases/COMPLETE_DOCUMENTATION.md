# Synapse Databases 完整文档

## 目录
- [概述](#概述)
- [快速开始](#快速开始)
- [核心功能](#核心功能)
- [配置管理](#配置管理)
- [字段转换](#字段转换)
- [SQL注解与自动Repository](#sql注解与自动repository)
- [现代Spring配置](#现代spring配置)
- [故障转移与路由](#故障转移与路由)
- [API参考](#api参考)
- [最佳实践](#最佳实践)
- [故障排除](#故障排除)

## 概述

Synapse Databases 是 Synapse Framework 的核心数据库模块，提供了强大的数据访问、配置管理和智能路由功能。

### 主要特性
- 🚀 **增强查询构建器**：支持复杂查询、分页、聚合等
- 🔄 **字段转换**：支持多种命名约定转换策略
- 📝 **SQL注解**：简化SQL操作，减少样板代码
- 🏗️ **自动Repository**：自动生成Repository实现
- 🛡️ **故障转移**：智能数据源路由和故障恢复
- ⚙️ **灵活配置**：支持多种配置方式和动态更新

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.indigo</groupId>
    <artifactId>synapse-databases</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 基础配置

```yaml
synapse:
  databases:
    sql-annotation:
      enabled: true
    auto-repository:
      enabled: false
  datasource:
    primary: master1
    datasources:
      master1:
        type: MYSQL
        host: localhost
        port: 3306
        database: your_database
        username: root
        password: password
        pool-type: HIKARI
        hikari:
          minimum-idle: 5
          maximum-pool-size: 15
    field-conversion:
      enabled: true
      strategy: CAMEL_TO_UNDERLINE
```

### 3. 使用示例

```java
@Repository
public interface UserRepository extends BaseRepository<User> {
    // 自动获得所有CRUD方法
}

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    public List<User> findUsers(UserQueryDTO query) {
        return userRepository.listWithDTO(query, UserVO.class);
    }
}
```

## 核心功能

### 增强查询构建器

`EnhancedQueryBuilder` 提供了强大的查询功能：

```java
// 基础查询
List<UserVO> users = EnhancedQueryBuilder.listWithCondition(queryDTO, UserVO.class);

// 分页查询
PageResult<UserVO> pageResult = EnhancedQueryBuilder.pageWithCondition(pageDTO, UserVO.class);

// 聚合查询
AggregationPageResult<UserVO> aggResult = EnhancedQueryBuilder.pageWithAggregation(aggDTO, UserVO.class);

// 性能监控查询
PerformancePageResult<UserVO> perfResult = EnhancedQueryBuilder.pageWithPerformance(perfDTO, UserVO.class);
```

### VO字段选择器

`VoFieldSelector` 自动选择需要的字段：

```java
// 自动选择VO中定义的字段
String selectFields = VoFieldSelector.getSelectFields(UserVO.class);
// 结果: "id, username, email, create_time"
```

### 字段映射

支持 `@FieldMapping` 注解进行字段映射：

```java
public class UserVO extends BaseVO {
    @FieldMapping("user_name")
    private String username;
    
    @FieldMapping("email_address")
    private String email;
    
    @FieldMapping(ignore = true)
    private String password; // 忽略此字段
}
```

## 配置管理

### 数据源配置

```yaml
synapse:
  datasource:
    primary: master1
    datasources:
      master1:
        type: MYSQL
        host: localhost
        port: 3306
        database: main_db
        username: root
        password: password
        pool-type: HIKARI
        hikari:
          minimum-idle: 5
          maximum-pool-size: 15
          idle-timeout: 30000
          max-lifetime: 1800000
          connection-timeout: 30000
          connection-test-query: "SELECT 1"
      slave1:
        type: MYSQL
        host: localhost
        port: 3307
        database: main_db
        username: root
        password: password
        pool-type: HIKARI
```

### MyBatis-Plus配置

```yaml
synapse:
  datasource:
    mybatis-plus:
      configuration:
        log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
        map-underscore-to-camel-case: true
        cache-enabled: true
        lazy-loading-enabled: true
      global-config:
        banner: false
        enable-pagination: true
        enable-optimistic-locker: true
        enable-block-attack: true
```

### 故障转移配置

```yaml
synapse:
  datasource:
    failover:
      enabled: true
      max-retry-times: 3
      retry-interval: 1000
      detection-interval: 5000
      recovery-interval: 10000
      strategy: PRIMARY_FIRST  # PRIMARY_FIRST, HEALTHY_FIRST, ROUND_ROBIN
```

## 字段转换

### 支持的转换策略

1. **驼峰转下划线** (CAMEL_TO_UNDERLINE) - 默认
2. **驼峰转短横线** (CAMEL_TO_KEBAB_CASE)
3. **无转换** (NO_CONVERSION)
4. **自定义转换** (CUSTOM)

### 配置示例

```yaml
synapse:
  datasource:
    field-conversion:
      enabled: true
      strategy: CAMEL_TO_UNDERLINE
      # 自定义转换配置
      custom-pattern:
        field-to-column-pattern: "([A-Z])"
        field-to-column-replacement: "_$1"
        column-to-field-pattern: "_([a-z])"
        column-to-field-replacement: "$1"
```

### 使用示例

```java
// 自动转换
String columnName = FieldConversionUtils.convertFieldToColumn("userName");
// 结果: "user_name"

String fieldName = FieldConversionUtils.convertColumnToField("user_name");
// 结果: "userName"
```

## SQL注解与自动Repository

### SQL注解功能

启用SQL注解功能：

```yaml
synapse:
  databases:
    sql-annotation:
      enabled: true
      debug: false
```

使用示例：

```java
@Repository
public interface UserRepository extends BaseRepository<User> {
    @Select("SELECT * FROM users WHERE status = #{status}")
    List<User> findByStatus(@Param("status") String status);
    
    @Select("SELECT * FROM users WHERE age > #{minAge}")
    List<User> findByAgeGreaterThan(@Param("minAge") Integer minAge);
}
```

### 自动Repository功能

启用自动Repository功能：

```yaml
synapse:
  databases:
    auto-repository:
      enabled: true
      base-packages: ["com.indigo", "com.yourcompany"]
      debug: false
      bean-name-strategy: SIMPLE_NAME
```

使用示例：

```java
@AutoRepository
public interface UserRepository {
    // 自动生成实现
    List<User> findAll();
    User findById(Long id);
    void save(User user);
    void deleteById(Long id);
}
```

### 功能对比

| 功能 | SQL注解 | 自动Repository |
|------|---------|----------------|
| 自定义SQL | ✅ | ❌ |
| 复杂查询 | ✅ | ❌ |
| 自动CRUD | ❌ | ✅ |
| 性能 | 高 | 中 |
| 灵活性 | 高 | 中 |

**推荐使用SQL注解**，因为它提供了更好的灵活性和性能。

## 现代Spring配置

### 配置类

```java
@Configuration
@EnableConfigurationProperties(SynapseDataSourceProperties.class)
public class ModernConfigurationConfig {
    
    @Bean
    public FieldConversionService fieldConversionService(
            SynapseDataSourceProperties dataSourceProperties) {
        return new FieldConversionService(dataSourceProperties);
    }
}
```

### 配置属性

```java
@ConfigurationProperties(prefix = "synapse.datasource")
@Data
public class SynapseDataSourceProperties {
    private String primary = "master1";
    private Map<String, DataSourceConfig> datasources = new LinkedHashMap<>();
    private FieldConversionConfig fieldConversion = new FieldConversionConfig();
    private FailoverConfig failover = new FailoverConfig();
    // ... 其他配置
}
```

## 故障转移与路由

### 故障转移策略

1. **PRIMARY_FIRST**：主数据源优先
2. **HEALTHY_FIRST**：健康数据源优先
3. **ROUND_ROBIN**：轮询故障转移

### 智能路由

```java
// 根据SQL类型和用户上下文进行智能路由
private String selectHealthyFirst(List<String> healthyDataSources, RoutingContext context) {
    return healthyDataSources.stream()
            .max((ds1, ds2) -> {
                int score1 = calculateHealthScore(ds1, context);
                int score2 = calculateHealthScore(ds2, context);
                return Integer.compare(score1, score2);
            })
            .orElse(healthyDataSources.get(0));
}
```

### 健康评分算法

```java
private int calculateHealthScore(String dataSourceName, RoutingContext context) {
    int baseScore = calculateHealthScore(dataSourceName);
    
    // 用户一致性路由
    if (dataSourceName.equals(getUserPreferredDataSource(context.getUserId()))) {
        baseScore += 20;
    }
    
    // SQL类型优化
    SqlType sqlType = context.getSqlType();
    if (sqlType == SqlType.SELECT && isReadOptimizedDataSource(dataSourceName)) {
        baseScore += 15;
    }
    
    // 负载均衡
    if (getDataSourceLoad(dataSourceName) < 50) {
        baseScore += 10;
    }
    
    return Math.max(0, baseScore);
}
```

## API参考

### EnhancedQueryBuilder

#### 基础查询方法

```java
// 条件查询
public static <V> List<V> listWithCondition(QueryDTO queryDTO, Class<V> voClass)

// 分页查询
public static <V> PageResult<V> pageWithCondition(PageDTO pageDTO, Class<V> voClass)

// 聚合查询
public static <V> AggregationPageResult<V> pageWithAggregation(AggregationPageDTO aggDTO, Class<V> voClass)

// 性能监控查询
public static <V> PerformancePageResult<V> pageWithPerformance(PerformancePageDTO perfDTO, Class<V> voClass)
```

#### 高级查询方法

```java
// 增强查询
public static <V> EnhancedPageResult<V> pageWithEnhanced(EnhancedPageDTO enhancedDTO, Class<V> voClass)

// 多表查询
public static <V> List<V> listWithMultiTableQuery(MultiTableQueryDTO queryDTO, Class<V> voClass)

// 连接查询
public static <V> PageResult<V> pageWithJoin(JoinPageDTO joinDTO, Class<V> voClass)

// 复杂查询
public static <V> PageResult<V> pageWithComplexQuery(ComplexPageDTO complexDTO, Class<V> voClass)
```

### VoFieldSelector

```java
// 获取选择字段
public static String getSelectFields(Class<?> voClass)

// 获取所有字段（包括父类）
public static Field[] getAllFields(Class<?> clazz)
```

### FieldConversionUtils

```java
// 字段转列名
public static String convertFieldToColumn(String fieldName)

// 列名转字段
public static String convertColumnToField(String columnName)
```

## 最佳实践

### 1. 配置管理

```yaml
# 推荐配置
synapse:
  databases:
    sql-annotation:
      enabled: true
    auto-repository:
      enabled: false  # 避免冲突
  datasource:
    field-conversion:
      enabled: true
      strategy: CAMEL_TO_UNDERLINE
    failover:
      enabled: true
      strategy: HEALTHY_FIRST
```

### 2. VO设计

```java
// 推荐的VO设计
public class UserVO extends BaseVO {
    @FieldMapping("user_name")
    private String username;
    
    @FieldMapping("email_address")
    private String email;
    
    // 忽略敏感字段
    @FieldMapping(ignore = true)
    private String password;
    
    // 使用父类的基础字段
    // createTime, modifyTime, createUser, modifyUser
}
```

### 3. Repository设计

```java
// 推荐使用SQL注解
@Repository
public interface UserRepository extends BaseRepository<User> {
    @Select("SELECT * FROM users WHERE status = #{status}")
    List<User> findByStatus(@Param("status") String status);
    
    @Select("SELECT * FROM users WHERE age BETWEEN #{minAge} AND #{maxAge}")
    List<User> findByAgeRange(@Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge);
}
```

### 4. 服务层设计

```java
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    public List<UserVO> findUsers(UserQueryDTO query) {
        return userRepository.listWithDTO(query, UserVO.class);
    }
    
    public PageResult<UserVO> findUsersPage(UserPageDTO pageDTO) {
        return userRepository.pageWithDTO(pageDTO, UserVO.class);
    }
}
```

## 故障排除

### 常见问题

#### 1. 字段转换不生效

**问题**：字段转换没有按预期工作

**解决方案**：
```yaml
synapse:
  datasource:
    field-conversion:
      enabled: true
      strategy: CAMEL_TO_UNDERLINE
```

#### 2. SQL注解不生效

**问题**：SQL注解方法无法执行

**解决方案**：
```yaml
synapse:
  databases:
    sql-annotation:
      enabled: true
```

#### 3. 故障转移不工作

**问题**：数据源故障时没有自动切换

**解决方案**：
```yaml
synapse:
  datasource:
    failover:
      enabled: true
      strategy: HEALTHY_FIRST
```

#### 4. 配置不生效

**问题**：配置修改后没有生效

**解决方案**：
- 检查配置文件格式
- 重启应用
- 检查配置属性绑定

### 调试技巧

#### 1. 启用调试日志

```yaml
logging:
  level:
    com.indigo.databases: DEBUG
    com.indigo.databases.utils: DEBUG
    com.indigo.databases.routing: DEBUG
```

#### 2. 检查配置

```java
@Autowired
private SynapseDataSourceProperties properties;

@EventListener(ApplicationReadyEvent.class)
public void checkConfiguration() {
    log.info("Current configuration: {}", properties);
}
```

#### 3. 监控数据源状态

```java
@Autowired
private FailoverRouter failoverRouter;

public void checkDataSourceHealth() {
    Map<String, Integer> stats = failoverRouter.getFailureStatistics();
    log.info("Data source failure statistics: {}", stats);
}
```

### 性能优化

#### 1. 连接池优化

```yaml
synapse:
  datasource:
    datasources:
      master1:
        hikari:
          minimum-idle: 10
          maximum-pool-size: 20
          idle-timeout: 300000
          max-lifetime: 1800000
```

#### 2. 查询优化

```java
// 使用字段选择器减少数据传输
String selectFields = VoFieldSelector.getSelectFields(UserVO.class);
// 只查询需要的字段
```

#### 3. 缓存优化

```yaml
synapse:
  datasource:
    mybatis-plus:
      configuration:
        cache-enabled: true
        local-cache-scope: SESSION
```

---

## 更新日志

### v1.0.0
- ✅ 初始版本发布
- ✅ 增强查询构建器
- ✅ 字段转换功能
- ✅ SQL注解支持
- ✅ 故障转移路由
- ✅ 现代Spring配置

---

**Synapse Databases** - 让数据库操作更简单、更智能！