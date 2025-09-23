# Synapse Framework - 数据库模块

## 概述

Synapse Framework 数据库模块是一个集成了 MyBatis-Plus 和动态数据源的强大数据库解决方案。它提供了灵活的配置选项，支持多种数据库类型和连接池，并且兼容标准的 Spring Boot 配置格式。

## 核心特性

- 🚀 **MyBatis-Plus 集成**: 完整的 MyBatis-Plus 配置支持
- 🔄 **动态数据源**: 支持多数据源动态切换
- 🗄️ **多数据库支持**: MySQL, PostgreSQL, Oracle, SQL Server, H2
- 🏊 **连接池支持**: HikariCP, Druid
- ⚙️ **灵活配置**: 支持自定义配置和默认值
- 🔌 **Spring Boot 兼容**: 兼容标准 Spring Boot 配置格式
- 🎯 **智能查询**: 基于 BaseRepository 的智能查询构建
- 📊 **多表关联**: 支持复杂的多表关联查询
- 🔧 **增强查询构建器**: 支持复杂查询、分页、聚合等
- 🛡️ **故障转移**: 智能数据源路由和故障恢复
- 📝 **SQL注解**: 简化SQL操作，减少样板代码
- 🏗️ **自动Repository**: 自动生成Repository实现

## 主要功能

### 1. 增强查询构建器 (EnhancedQueryBuilder)

提供强大的查询功能，支持：

- **基础查询**：分页、列表、单个查询，支持VO映射
- **多表关联**：支持INNER、LEFT、RIGHT、FULL JOIN
- **聚合查询**：COUNT、SUM、AVG、MAX、MIN等聚合函数
- **性能监控**：查询时间、执行计划、性能评级
- **便捷方法**：快速查询、统计查询、存在性查询
- **异步查询**：基于CompletableFuture的异步查询支持（实验性功能）

### 2. 字段转换功能

支持多种命名约定转换策略：

- **驼峰转下划线**：`userName` → `user_name`
- **下划线转驼峰**：`user_name` → `userName`
- **驼峰转短横线**：`userName` → `user-name`
- **自定义转换**：支持正则表达式自定义转换规则

### 3. SQL注解与自动Repository

- **SQL注解**：简化SQL操作，减少样板代码
- **自动Repository**：自动生成Repository实现
- **智能查询**：基于注解的自动查询条件构建

### 4. 故障转移与路由

- **智能路由**：自动根据SQL类型选择读写数据源
- **故障转移**：数据源故障时自动切换
- **负载均衡**：支持多种负载均衡策略
- **健康检查**：实时监控数据源健康状态

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
    # SQL注解功能（推荐启用）
    sql-annotation:
      enabled: true
      debug: false
    
    # 自动Repository功能（可选，避免与SQL注解冲突）
    auto-repository:
      enabled: false
      base-packages: ["com.indigo", "com.yourcompany", "com.example"]
      debug: false
      bean-name-strategy: SIMPLE_NAME

  datasource:
    # MyBatis-Plus 配置
    mybatis-plus:
      configuration:
        log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
        map-underscore-to-camel-case: true
        cache-enabled: true
        lazy-loading-enabled: true
        aggressive-lazy-loading: false
        multiple-result-sets-enabled: true
        use-column-label: true
        use-generated-keys: false
        auto-mapping-behavior: PARTIAL
        auto-mapping-unknown-column-behavior: WARNING
        default-executor-type: SIMPLE
        default-statement-timeout: 25
        default-fetch-size: 100
        safe-row-bounds-enabled: false
        safe-result-handler-enabled: true
        local-cache-scope: SESSION
        lazy-load-trigger-methods: "equals,clone,hashCode,toString"
      global-config:
        banner: false
        enable-sql-runner: false
        enable-meta-object-handler: true
        enable-sql-injector: true
        enable-pagination: true
        enable-optimistic-locker: true
        enable-block-attack: true
      type-aliases-package: "com.indigo.**.entity"
      mapper-locations: "classpath*:mapper/**/*.xml"
    
    # 主数据源名称
      primary: master1

    # 字段转换配置
    field-conversion:
      enabled: true
      strategy: CAMEL_TO_UNDERLINE
      custom-pattern:
        field-to-column-replacement: "$1_$2"
        column-to-field-replacement: "$1$2"

    # 数据源配置
    datasources:
        master1:
          type: MYSQL
          host: localhost
          port: 3306
          database: synapse_iam
          username: root
          password: your_password
          pool-type: HIKARI
          params:
            useUnicode: "true"
            characterEncoding: "utf8"
            useSSL: "false"
            serverTimezone: "Asia/Shanghai"
          hikari:
            minimum-idle: 5
            maximum-pool-size: 15
            idle-timeout: 30000
            max-lifetime: 1800000
            connection-timeout: 30000
            connection-test-query: "SELECT 1"
```

### 3. 创建实体类

```java
@Data
@TableName("sys_user")
public class User extends BaseEntity {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    
    private String email;
    
    private String phone;
    
    private Integer status;
    
    private Long deptId;
    
    private Long roleId;
}
```

### 4. 创建Repository

```java
@Repository
public interface UserRepository extends BaseRepository<User, UserMapper> {
    
    // 基础分页查询 - 支持VO映射
    default PageResult<UserVO> pageUsers(UserQueryDTO queryDTO) {
        return pageWithCondition(queryDTO, UserVO.class);
    }
    
    // 多表关联查询 - 支持VO映射
    default PageResult<UserJoinResultDTO> pageUsersWithJoin(UserJoinQueryDTO queryDTO) {
        return pageWithJoin(queryDTO, UserJoinResultDTO.class);
    }
    
    // 聚合查询
    default AggregationPageResult<UserVO> pageUsersWithAggregation(UserAggregationQueryDTO queryDTO) {
        return pageWithAggregation(queryDTO, UserVO.class);
    }
    
    // 性能监控查询
    default PerformancePageResult<UserVO> pageUsersWithPerformance(UserPerformanceQueryDTO queryDTO) {
        return pageWithPerformance(queryDTO, UserVO.class);
    }
    
    // 异步查询（实验性功能）
    default CompletableFuture<PageResult<UserVO>> pageUsersAsync(UserQueryDTO queryDTO) {
        return pageWithConditionAsync(queryDTO, UserVO.class);
    }
}
```

### 5. 创建查询DTO

```java
// 用户查询DTO - 基础分页查询
public class UserQueryDTO extends PageDTO {
    private String username;      // 用户名
    private String email;         // 邮箱
    private String phone;         // 手机号
    private Integer userLevel;    // 用户等级
    private String realName;      // 真实姓名
    private Long deptId;          // 部门ID
    private Long roleId;          // 角色ID
}

// 用户关联查询DTO - 多表查询
public class UserJoinQueryDTO extends JoinPageDTO {
    private String username;      // 用户名
    private String deptName;      // 部门名称
    private String roleName;      // 角色名称
    private String realName;      // 真实姓名
    private Integer userLevel;    // 用户等级
    
    public UserJoinQueryDTO() {
        // 配置多表关联
        this.setTableJoins(Arrays.asList(
            new TableJoin("sys_department", "d", JoinType.LEFT, "u.dept_id = d.id"),
            new TableJoin("sys_role", "r", JoinType.LEFT, "u.role_id = r.id"),
            new TableJoin("sys_user_profile", "p", JoinType.LEFT, "u.id = p.user_id")
        ));
        
        // 设置选择字段
        this.getTableJoins().get(0).setSelectFields(Arrays.asList("d.dept_name", "d.dept_code"));
        this.getTableJoins().get(1).setSelectFields(Arrays.asList("r.role_name", "r.role_code"));
        this.getTableJoins().get(2).setSelectFields(Arrays.asList("p.real_name", "p.avatar"));
    }
}

// 用户聚合查询DTO
public class UserAggregationQueryDTO extends AggregationPageDTO {
    private String username;
    private String deptName;
    
    public UserAggregationQueryDTO() {
        // 配置聚合字段
        this.setAggregationFields(Arrays.asList(
            new AggregationField("COUNT", "id", "totalCount"),
            new AggregationField("SUM", "user_level", "totalLevel"),
            new AggregationField("AVG", "user_level", "avgLevel")
        ));
    }
}

// 用户性能监控查询DTO
public class UserPerformanceQueryDTO extends PerformancePageDTO {
    private String username;
    private String deptName;
    
    public UserPerformanceQueryDTO() {
        // 配置性能监控
        this.setEnablePerformanceMonitoring(true);
        this.setPerformanceThreshold(1000); // 1秒
    }
}
```

### 6. 使用示例

```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    // 基础分页查询
    public PageResult<UserVO> pageUsers(UserQueryDTO queryDTO) {
        return userRepository.pageUsers(queryDTO);
    }
    
    // 多表关联分页查询
    public PageResult<UserJoinResultDTO> pageUsersWithJoin(UserJoinQueryDTO queryDTO) {
        return userRepository.pageUsersWithJoin(queryDTO);
    }
    
    // 聚合查询
    public AggregationPageResult<UserVO> pageUsersWithAggregation(UserAggregationQueryDTO queryDTO) {
        return userRepository.pageUsersWithAggregation(queryDTO);
    }
    
    // 性能监控查询
    public PerformancePageResult<UserVO> pageUsersWithPerformance(UserPerformanceQueryDTO queryDTO) {
        return userRepository.pageUsersWithPerformance(queryDTO);
    }
    
    // 异步查询
    public CompletableFuture<PageResult<UserVO>> pageUsersAsync(UserQueryDTO queryDTO) {
        return userRepository.pageUsersAsync(queryDTO);
    }
    
    // 动态数据源切换
    @DS("slave")
    public List<User> getUsersFromSlave() {
        return userRepository.list();
    }
    
    @DS("master")
    public void saveUser(User user) {
        userRepository.save(user);
    }
}
```

## 配置说明

### 配置前缀

使用 `synapse.datasource` 作为配置前缀，同时保持向后兼容性。

### 主要配置结构

```yaml
synapse:
  databases:
    # SQL注解功能
    sql-annotation:
      enabled: true
      debug: false
    
    # 自动Repository功能
    auto-repository:
      enabled: false
      base-packages: ["com.indigo", "com.yourcompany"]
      debug: false
      bean-name-strategy: SIMPLE_NAME

  datasource:
    # MyBatis-Plus 配置
    mybatis-plus:
      configuration: { ... }
      global-config: { ... }
      type-aliases-package: "com.indigo.**.entity"
      mapper-locations: "classpath*:mapper/**/*.xml"
    
    # 主数据源名称
      primary: master1
    
    # 字段转换配置
    field-conversion:
      enabled: true
      strategy: CAMEL_TO_UNDERLINE
      custom-pattern:
        field-to-column-replacement: "$1_$2"
        column-to-field-replacement: "$1$2"
    
    # 读写分离配置
    read-write:
      enabled: false
      mode: AUTO
      read-sources: ["slave1", "slave2"]
      write-sources: ["master"]
    
    # 负载均衡配置
    load-balance:
      enabled: false
      strategy: ROUND_ROBIN
      weights:
        slave1: 1
        slave2: 2
    
    # 故障转移配置
    failover:
      enabled: false
      strategy: HEALTHY_FIRST
      health-check-interval: 30000
      failure-threshold: 3
      recovery-timeout: 60000
    
    # Seata分布式事务配置
    seata:
      enabled: false
      application-id: "synapse-app"
      tx-service-group: "synapse-tx-group"
    
    # 数据源配置
    datasources:
        master1:
          type: MYSQL
          host: localhost
          port: 3306
          database: synapse_iam
          username: root
          password: your_password
          pool-type: HIKARI
        params: { ... }
        hikari: { ... }
        druid: { ... }
```

### 兼容性配置

为了保持向后兼容性，模块仍然支持标准的 Spring Boot 配置格式：

```yaml
spring:
  datasource:
    dynamic:
      primary: master1
      strict: false
      datasource:
        master1:
          type: MYSQL
          host: localhost
          port: 3306
          database: synapse_iam
          username: root
          password: your_password
          pool-type: HIKARI
          params:
            useUnicode: "true"
            characterEncoding: "utf8"
            useSSL: "false"
            serverTimezone: "Asia/Shanghai"
          hikari:
            minimum-idle: 5
            maximum-pool-size: 15
            idle-timeout: 30000
            max-lifetime: 1800000
            connection-timeout: 30000
            connection-test-query: "SELECT 1"
```

### 数据源类型

| 类型 | 描述 | 驱动类 |
|------|------|--------|
| MYSQL | MySQL 数据库 | com.mysql.cj.jdbc.Driver |
| POSTGRESQL | PostgreSQL 数据库 | org.postgresql.Driver |
| ORACLE | Oracle 数据库 | oracle.jdbc.OracleDriver |
| SQLSERVER | SQL Server 数据库 | com.microsoft.sqlserver.jdbc.SQLServerDriver |
| H2 | H2 内存数据库 | org.h2.Driver |

### 连接池类型

| 类型 | 描述 | 特点 |
|------|------|------|
| HIKARI | HikariCP 连接池 | 高性能、轻量级 |
| DRUID | Druid 连接池 | 功能丰富、监控完善 |

## 高级功能

### 1. 增强查询构建器

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

#### 异步查询方法（实验性功能）

```java
// 异步分页查询
public static <T, V extends BaseVO> CompletableFuture<PageResult<V>> pageWithConditionAsync(
    IService<T> service, PageDTO pageDTO, Class<V> voClass)

// 异步列表查询
public static <T, V extends BaseVO> CompletableFuture<List<V>> listWithConditionAsync(
    IService<T> service, QueryDTO queryDTO, Class<V> voClass)

// 异步单个查询
public static <T, V extends BaseVO> CompletableFuture<V> getOneWithConditionAsync(
    IService<T> service, QueryDTO queryDTO, Class<V> voClass)
```

### 2. 字段转换功能

#### 转换策略

```java
// 驼峰转下划线
String columnName = FieldConversionUtils.convertFieldToColumn("userName");
// 结果: "user_name"

// 下划线转驼峰
String fieldName = FieldConversionUtils.convertColumnToField("user_name");
// 结果: "userName"

// 驼峰转短横线
String kebabCase = FieldConversionUtils.convertFieldToKebabCase("userName");
// 结果: "user-name"
```

#### 自定义转换规则

```yaml
synapse:
  datasource:
    field-conversion:
      enabled: true
      strategy: CUSTOM
      custom-pattern:
        field-to-column-replacement: "$1_$2"
        column-to-field-replacement: "$1$2"
```

### 3. VoMapping 注解功能

`VoMapping` 注解是 Synapse Framework 的核心功能之一，用于简化多表关联查询的VO映射配置。通过注解方式配置表关联关系，框架会自动生成复杂的多表查询SQL。

#### 3.1 基础使用

```java
@VoMapping(
    table = "sys_user",
    alias = "u",
    joins = {
        @VoMapping.Join(
            table = "sys_department", 
            alias = "d", 
            type = JoinType.LEFT, 
            on = "u.dept_id = d.id"
        ),
        @VoMapping.Join(
            table = "sys_role", 
            alias = "r", 
            type = JoinType.LEFT, 
            on = "u.role_id = r.id"
        )
    },
    fields = {
        @VoMapping.Field(source = "u.id", target = "id"),
        @VoMapping.Field(source = "u.username", target = "username"),
        @VoMapping.Field(source = "d.dept_name", target = "deptName"),
        @VoMapping.Field(source = "r.role_name", target = "roleName"),
        @VoMapping.Field(source = "u.create_time", target = "createTime")
    }
)
public class UserWithDeptRoleVO extends BaseVO {
    private Long id;
    private String username;
    private String deptName;
    private String roleName;
    private LocalDateTime createTime;
    
    // getter/setter...
}
```

#### 3.2 高级功能

##### 3.2.1 计算字段

```java
@VoMapping(
    table = "sys_user",
    alias = "u",
    joins = {
        @VoMapping.Join(
            table = "sys_department", 
            alias = "d", 
            type = JoinType.LEFT, 
            on = "u.dept_id = d.id"
        )
    },
    fields = {
        @VoMapping.Field(source = "u.id", target = "id"),
        @VoMapping.Field(source = "u.username", target = "username"),
        @VoMapping.Field(source = "d.dept_name", target = "deptName"),
        // 计算字段：用户年龄
        @VoMapping.Field(
            source = "TIMESTAMPDIFF(YEAR, u.birthday, CURDATE())", 
            target = "age",
            type = FieldType.EXPRESSION
        ),
        // 计算字段：用户状态描述
        @VoMapping.Field(
            source = "CASE WHEN u.status = 1 THEN '启用' WHEN u.status = 0 THEN '禁用' ELSE '未知' END", 
            target = "statusDesc",
            type = FieldType.EXPRESSION
        )
    }
)
public class UserWithCalculatedFieldsVO extends BaseVO {
    private Long id;
    private String username;
    private String deptName;
    private Integer age;
    private String statusDesc;
    
    // getter/setter...
}
```

##### 3.2.2 复杂关联查询

```java
@VoMapping(
    table = "sys_user",
    alias = "u",
    joins = {
        @VoMapping.Join(
            table = "sys_department", 
            alias = "d", 
            type = JoinType.LEFT, 
            on = "u.dept_id = d.id"
        ),
        @VoMapping.Join(
            table = "sys_role", 
            alias = "r", 
            type = JoinType.LEFT, 
            on = "u.role_id = r.id"
        ),
        @VoMapping.Join(
            table = "sys_user_profile", 
            alias = "p", 
            type = JoinType.LEFT, 
            on = "u.id = p.user_id"
        ),
        @VoMapping.Join(
            table = "sys_company", 
            alias = "c", 
            type = JoinType.LEFT, 
            on = "d.company_id = c.id"
        )
    },
    fields = {
        // 用户基础信息
        @VoMapping.Field(source = "u.id", target = "id"),
        @VoMapping.Field(source = "u.username", target = "username"),
        @VoMapping.Field(source = "u.email", target = "email"),
        @VoMapping.Field(source = "u.phone", target = "phone"),
        
        // 部门信息
        @VoMapping.Field(source = "d.dept_name", target = "deptName"),
        @VoMapping.Field(source = "d.dept_code", target = "deptCode"),
        
        // 角色信息
        @VoMapping.Field(source = "r.role_name", target = "roleName"),
        @VoMapping.Field(source = "r.role_code", target = "roleCode"),
        
        // 用户档案信息
        @VoMapping.Field(source = "p.real_name", target = "realName"),
        @VoMapping.Field(source = "p.avatar", target = "avatar"),
        @VoMapping.Field(source = "p.gender", target = "gender"),
        
        // 公司信息
        @VoMapping.Field(source = "c.company_name", target = "companyName"),
        @VoMapping.Field(source = "c.company_code", target = "companyCode"),
        
        // 时间信息
        @VoMapping.Field(source = "u.create_time", target = "createTime"),
        @VoMapping.Field(source = "u.modify_time", target = "modifyTime")
    }
)
public class UserCompleteInfoVO extends BaseVO {
    // 用户基础信息
    private Long id;
    private String username;
    private String email;
    private String phone;
    
    // 部门信息
    private String deptName;
    private String deptCode;
    
    // 角色信息
    private String roleName;
    private String roleCode;
    
    // 用户档案信息
    private String realName;
    private String avatar;
    private Integer gender;
    
    // 公司信息
    private String companyName;
    private String companyCode;
    
    // 时间信息
    private LocalDateTime createTime;
    private LocalDateTime modifyTime;
    
    // getter/setter...
}
```

#### 3.3 Repository 使用

```java
@Repository
public interface UserRepository extends BaseRepository<User, UserMapper> {
    
    // 使用VoMapping注解的多表查询
    default PageResult<UserWithDeptRoleVO> pageUsersWithDeptRole(UserQueryDTO queryDTO) {
        return pageWithCondition(queryDTO, UserWithDeptRoleVO.class);
    }
    
    // 使用VoMapping注解的列表查询
    default List<UserWithDeptRoleVO> listUsersWithDeptRole(UserQueryDTO queryDTO) {
        return listWithCondition(queryDTO, UserWithDeptRoleVO.class);
    }
    
    // 使用VoMapping注解的单个查询
    default UserWithDeptRoleVO getUserWithDeptRole(Long userId) {
        QueryDTO queryDTO = new QueryDTO();
        queryDTO.addCondition("u.id", "eq", userId);
        return getOneWithCondition(queryDTO, UserWithDeptRoleVO.class);
    }
}
```

#### 3.4 Service 使用

```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    // 分页查询用户及其部门角色信息
    public PageResult<UserWithDeptRoleVO> pageUsersWithDeptRole(UserQueryDTO queryDTO) {
        return userRepository.pageUsersWithDeptRole(queryDTO);
    }
    
    // 列表查询用户及其部门角色信息
    public List<UserWithDeptRoleVO> listUsersWithDeptRole(UserQueryDTO queryDTO) {
        return userRepository.listUsersWithDeptRole(queryDTO);
    }
    
    // 获取单个用户的完整信息
    public UserWithDeptRoleVO getUserWithDeptRole(Long userId) {
        return userRepository.getUserWithDeptRole(userId);
    }
}
```

#### 3.5 注解参数说明

##### 3.5.1 VoMapping 主注解

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| table | String | 是 | 主表名 |
| alias | String | 否 | 主表别名，默认为 "t" |
| joins | Join[] | 否 | 关联表配置数组 |
| fields | Field[] | 否 | 字段映射配置数组 |

##### 3.5.2 Join 关联配置

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| table | String | 是 | 关联表名 |
| alias | String | 是 | 关联表别名 |
| type | JoinType | 否 | 关联类型，默认为 LEFT |
| on | String | 是 | 关联条件 |

##### 3.5.3 Field 字段配置

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| source | String | 是 | 数据库字段（支持表别名.字段名） |
| target | String | 否 | VO字段名，默认为空（使用source） |
| type | FieldType | 否 | 字段类型，默认为 DIRECT |
| expression | String | 否 | 自定义SQL表达式（用于计算字段） |

##### 3.5.4 JoinType 关联类型

| 类型 | SQL关键字 | 说明 |
|------|-----------|------|
| INNER | INNER JOIN | 内连接 |
| LEFT | LEFT JOIN | 左连接 |
| RIGHT | RIGHT JOIN | 右连接 |
| FULL | FULL JOIN | 全连接 |

##### 3.5.5 FieldType 字段类型

| 类型 | 说明 |
|------|------|
| DIRECT | 直接映射 |
| EXPRESSION | 表达式计算 |
| ALIAS | 别名映射 |

#### 3.6 最佳实践

##### 3.6.1 VO设计原则

```java
// ✅ 推荐：清晰的字段分组和注释
@VoMapping(
    table = "sys_user",
    alias = "u",
    joins = {
        @VoMapping.Join(table = "sys_department", alias = "d", type = JoinType.LEFT, on = "u.dept_id = d.id")
    },
    fields = {
        // 用户基础信息
        @VoMapping.Field(source = "u.id", target = "id"),
        @VoMapping.Field(source = "u.username", target = "username"),
        
        // 部门信息
        @VoMapping.Field(source = "d.dept_name", target = "deptName"),
        @VoMapping.Field(source = "d.dept_code", target = "deptCode")
    }
)
public class UserWithDeptVO extends BaseVO {
    // 用户基础信息
    private Long id;
    private String username;
    
    // 部门信息
    private String deptName;
    private String deptCode;
    
    // getter/setter...
}
```

##### 3.6.2 性能优化建议

```java
// ✅ 推荐：只选择需要的字段
@VoMapping(
    table = "sys_user",
    alias = "u",
    joins = {
        @VoMapping.Join(table = "sys_department", alias = "d", type = JoinType.LEFT, on = "u.dept_id = d.id")
    },
    fields = {
        // 只选择必要的字段，避免查询大字段
        @VoMapping.Field(source = "u.id", target = "id"),
        @VoMapping.Field(source = "u.username", target = "username"),
        @VoMapping.Field(source = "d.dept_name", target = "deptName")
        // 避免选择大字段如：u.avatar, u.description 等
    }
)
public class UserSimpleVO extends BaseVO {
    private Long id;
    private String username;
    private String deptName;
    
    // getter/setter...
}
```

##### 3.6.3 错误处理

```java
// ✅ 推荐：使用LEFT JOIN避免数据丢失
@VoMapping(
    table = "sys_user",
    alias = "u",
    joins = {
        // 使用LEFT JOIN确保即使没有部门信息的用户也能查询出来
        @VoMapping.Join(table = "sys_department", alias = "d", type = JoinType.LEFT, on = "u.dept_id = d.id")
    }
)
public class UserWithDeptVO extends BaseVO {
    private Long id;
    private String username;
    private String deptName; // 可能为null
    
    // getter/setter...
}
```

### 4. SQL注解功能

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
    
    @Select("SELECT u.*, d.dept_name FROM users u LEFT JOIN departments d ON u.dept_id = d.id WHERE u.status = #{status}")
    List<UserWithDeptVO> findUsersWithDept(@Param("status") String status);
}
```

### 5. 自动Repository功能

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
    
    // 支持自定义方法
    List<User> findByStatus(String status);
    PageResult<User> pageUsers(PageDTO pageDTO);
}
```

### 6. 读写分离

#### 配置示例

```yaml
synapse:
  datasource:
    primary: master
    read-write:
      enabled: true
      mode: AUTO
      read-sources: ["slave1", "slave2"]
      write-sources: ["master"]
    datasources:
        master:
          type: MYSQL
          host: master-db.example.com
          pool-type: HIKARI
        slave1:
          type: MYSQL
          host: slave1-db.example.com
          pool-type: HIKARI
        slave2:
          type: MYSQL
          host: slave2-db.example.com
          pool-type: HIKARI
```

#### 工作原理

- 系统自动根据SQL类型选择数据源
- SELECT语句自动路由到从库（轮询负载均衡）
- INSERT/UPDATE/DELETE语句自动路由到主库
- 无需手动指定数据源，系统智能路由

### 7. 故障转移

#### 配置示例

```yaml
synapse:
  datasource:
    failover:
      enabled: true
      strategy: HEALTHY_FIRST
      health-check-interval: 30000
      failure-threshold: 3
      recovery-timeout: 60000
    datasources:
      master:
        type: MYSQL
        host: master-db.example.com
      slave1:
        type: MYSQL
        host: slave1-db.example.com
      slave2:
        type: MYSQL
        host: slave2-db.example.com
```

#### 故障转移策略

- **HEALTHY_FIRST**：优先选择健康的数据源
- **ROUND_ROBIN**：轮询选择数据源
- **WEIGHTED_ROUND_ROBIN**：基于权重的轮询
- **LEAST_CONNECTIONS**：选择连接数最少的数据源

### 8. 分布式事务

```yaml
synapse:
  datasource:
    seata:
      enabled: true
      application-id: "synapse-app"
      tx-service-group: "synapse-tx-group"
    datasources:
      master:
        type: MYSQL
        host: localhost
```

### 9. SQL 监控

```yaml
synapse:
  datasource:
    p6spy:
      enabled: true
    datasources:
      master:
        type: MYSQL
        host: localhost
```

## 最佳实践

### 1. 数据源命名

- 使用有意义的名称：`master`、`slave1`、`analytics`、`reporting`
- 避免使用数字后缀：`db1`、`db2` 等

### 2. 连接池配置

**HikariCP 推荐配置**
```yaml
hikari:
  minimum-idle: 10
  maximum-pool-size: 50
  idle-timeout: 300000        # 5分钟
  max-lifetime: 1800000       # 30分钟
  connection-timeout: 20000   # 20秒
```

**Druid 推荐配置**
```yaml
druid:
  initial-size: 10
  min-idle: 10
  max-active: 50
  max-wait: 60000
  time-between-eviction-runs-millis: 60000
  validation-query: "SELECT 1"
  test-while-idle: true
```

### 3. 查询优化

**使用字段选择器减少数据传输**
```java
// 只查询需要的字段
String selectFields = VoFieldSelector.getSelectFields(UserVO.class);
// 结果: "id, username, email, create_time"
```

**使用缓存优化**
```yaml
synapse:
  datasource:
    mybatis-plus:
      configuration:
        cache-enabled: true
        local-cache-scope: SESSION
```

### 4. 异步查询使用建议

**适用场景**
- 大数据量查询（>10万条记录）
- 复杂多表关联查询
- 需要并行执行多个查询的场景
- 提升用户体验（避免界面卡顿）

**注意事项**
- 异步查询会增加内存消耗和线程管理复杂度
- 错误处理相对复杂，需要正确处理CompletableFuture的异常
- 调试相对困难，建议在性能瓶颈明确时再使用
- API可能会在后续版本中调整

### 5. 监控和告警

- 配置连接池监控
- 设置慢 SQL 告警
- 监控连接池使用情况
- 使用性能监控查询功能

## 故障排除

### 常见问题

1. **配置绑定失败**
   - 检查配置结构是否正确
   - 确保 `primary` 属性位置正确

2. **数据源切换不生效**
   - 检查数据源名称是否与配置文件中的名称一致
   - 确保在finally块中清除数据源上下文
   - 验证数据源配置是否正确

3. **连接池配置不生效**
   - 检查配置路径是否正确
   - 确保连接池类型配置正确

4. **MyBatis参数绑定错误**
   - 检查 `@Param` 注解使用是否正确
   - 确保参数名与注解名一致
   - 验证动态SQL参数绑定

5. **查询性能问题**
   - 使用性能监控查询功能
   - 检查SQL执行计划
   - 优化查询条件和索引

### 日志配置

```yaml
logging:
  level:
    com.indigo.databases: DEBUG
    com.indigo.databases.utils: DEBUG
    com.indigo.databases.routing: DEBUG
    com.zaxxer.hikari: DEBUG
    com.alibaba.druid: DEBUG
```

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

## 注意事项

1. **配置前缀**: 使用 `synapse.datasource` 作为配置前缀
2. **向后兼容**: 仍然支持 `spring.datasource.dynamic` 配置格式
3. **类型安全**: 使用枚举类型确保配置的正确性
4. **异步查询**: 异步查询功能目前处于实验阶段，请谨慎使用
5. **性能监控**: 建议在生产环境中启用性能监控功能

## 迁移指南

如果你正在从旧版本迁移，请按照以下步骤操作：

1. 将配置前缀从 `synapse.databases` 改为 `synapse.datasource`
2. 更新代码中的配置类引用（如果直接使用配置类）
3. 测试配置是否正确加载
4. 验证所有功能是否正常工作

## 更新日志

### v1.0.0
- ✅ 初始版本发布
- ✅ 增强查询构建器
- ✅ 字段转换功能
- ✅ SQL注解支持
- ✅ 自动Repository功能
- ✅ 故障转移路由
- ✅ 现代Spring配置
- ✅ 异步查询支持（实验性功能）
- ✅ 性能监控功能
- ✅ 多表关联查询
- ✅ 聚合查询功能

## 贡献

欢迎提交 Issue 和 Pull Request 来改进这个模块。

## 许可证

本项目采用 MIT 许可证。