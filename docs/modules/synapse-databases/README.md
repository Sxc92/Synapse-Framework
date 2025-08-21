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

## 主要特性

- 🗄️ **统一配置管理**：整合 MyBatis-Plus 和动态数据源配置
- 🔄 **动态数据源路由**：支持运行时数据源切换（编程式切换）
- 🚀 **高性能连接池**：支持 HikariCP 和 Druid
- 🎯 **多数据库支持**：MySQL、PostgreSQL、Oracle、SQL Server、H2
- 🔒 **分布式事务**：集成 Seata 支持
- 📊 **SQL监控**：集成 P6Spy 进行 SQL 性能分析
- 🔧 **自动配置**：Spring Boot 自动配置支持
- 🤖 **智能路由**：自动根据SQL类型选择读写数据源

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.indigo</groupId>
    <artifactId>synapse-databases</artifactId>
    <version>${synapse.version}</version>
</dependency>
```

### 2. 基础配置

```yaml
synapse:
  datasource:
    mybatis-plus:
      configuration:
        log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
        map-underscore-to-camel-case: true
        # ... 其他 MyBatis-Plus 配置
      global-config:
        banner: false
        enable-pagination: true
        # ... 其他全局配置
      type-aliases-package: com.indigo.**.entity
      mapper-locations: "classpath*:mapper/**/*.xml"
    
    dynamic-data-source:
      primary: master1
      strict: false
      seata: false
      p6spy: false
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

### 4. 创建 Repository

```java
public interface UserRepository extends BaseRepository<User, UserMapper> {
    
    // 单表查询 - 使用基础DTO + 业务字段
    default PageResult<User> pageUsers(UserQueryDTO queryDTO) {
        // 框架自动处理：基础字段 + 业务字段 + 分页 + 排序
        return pageWithCondition(queryDTO);
    }
    
    // 多表关联查询 - 使用JoinPageDTO + 业务字段
    default PageResult<UserJoinResultDTO> pageUsersWithJoin(UserJoinQueryDTO queryDTO) {
        // 框架自动处理：多表关联 + 业务字段 + 分页 + 排序
        PageResult<User> pageResult = pageWithJoin(queryDTO);
        
        // 转换为结果DTO
        List<UserJoinResultDTO> resultList = pageResult.getRecords().stream()
            .map(this::convertToJoinResult)
            .collect(Collectors.toList());
        
        return new PageResult<>(resultList, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize());
    }
}
```

### 5. 创建查询DTO

```java
// 用户查询DTO - 单表查询
public class UserQueryDTO extends PageDTO {
    // 业务字段
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
    // 业务字段
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
```

### 6. 使用示例

```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    // 单表分页查询
    public PageResult<User> pageUsers(UserQueryDTO queryDTO) {
        return userRepository.pageUsers(queryDTO);
    }
    
    // 多表关联分页查询
    public PageResult<UserJoinResultDTO> pageUsersWithJoin(UserJoinQueryDTO queryDTO) {
        return userRepository.pageUsersWithJoin(queryDTO);
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

新的配置类使用 `synapse.datasource` 作为配置前缀，替代了之前的 `synapse.databases`。

### 主要配置结构

```yaml
synapse:
  datasource:
    mybatis-plus:
      configuration:
        log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
        map-underscore-to-camel-case: true
        # ... 其他 MyBatis-Plus 配置
      global-config:
        banner: false
        enable-pagination: true
        # ... 其他全局配置
      type-aliases-package: com.indigo.**.entity
      mapper-locations: "classpath*:mapper/**/*.xml"
    
    dynamic-data-source:
      primary: master1
      strict: false
      seata: false
      p6spy: false
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

### 1. 读写分离

**配置示例**
```yaml
synapse:
  datasource:
    primary: master
    dynamic-data-source:
      strict: true
      datasource:
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

**工作原理**
- 系统自动根据SQL类型选择数据源
- SELECT语句自动路由到从库（轮询负载均衡）
- INSERT/UPDATE/DELETE语句自动路由到主库
- 无需手动指定数据源，系统智能路由

### 2. 多数据库类型

```yaml
synapse:
  datasource:
    primary: mysql-master
    dynamic-data-source:
      datasource:
        mysql-master:
          type: MYSQL
          host: mysql.example.com
        postgres-analytics:
          type: POSTGRESQL
          host: postgres.example.com
        oracle-reporting:
          type: ORACLE
          host: oracle.example.com
```

### 3. 分布式事务

```yaml
synapse:
  datasource:
    dynamic-data-source:
      seata: true
      datasource:
        master:
          type: MYSQL
          host: localhost
```

### 4. SQL 监控

```yaml
synapse:
  datasource:
    dynamic-data-source:
      p6spy: true
      datasource:
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

### 3. 数据源切换策略

**推荐使用自动路由**
- 让系统根据SQL类型自动选择数据源
- 减少手动切换的代码复杂度
- 提高代码的可维护性

**手动切换场景**
- 需要精确控制数据源的业务场景
- 复杂的多数据源操作
- 特殊的数据源需求

### 4. 监控和告警

- 配置连接池监控
- 设置慢 SQL 告警
- 监控连接池使用情况

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

### 日志配置

```yaml
logging:
  level:
    com.indigo.databases: DEBUG
    com.zaxxer.hikari: DEBUG
    com.alibaba.druid: DEBUG
```

## 注意事项

1. **配置前缀变更**: 从 `synapse.databases` 变更为 `synapse.datasource`
2. **移除 enabled 属性**: 不再需要 `enabled: true` 配置
3. **向后兼容**: 仍然支持 `spring.datasource.dynamic` 配置格式
4. **类型安全**: 使用枚举类型确保配置的正确性

## 迁移指南

如果你正在从旧版本迁移，请按照以下步骤操作：

1. 将配置前缀从 `synapse.databases` 改为 `synapse.datasource`
2. 移除 `enabled: true` 配置项
3. 更新代码中的配置类引用（如果直接使用配置类）
4. 测试配置是否正确加载

## 贡献

欢迎提交 Issue 和 Pull Request 来改进这个模块。

## 许可证

本项目采用 MIT 许可证。 