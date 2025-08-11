# Synapse Databases 模块

## 概述

Synapse Databases 模块是 Synapse Framework 的核心数据库访问模块，提供了统一的数据库配置管理、动态数据源路由、MyBatis-Plus 集成以及多种连接池支持。

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
    primary: master1
    mybatis-plus:
      type-aliases-package: com.example.**.entity
      mapper-locations: "classpath*:mapper/**/*.xml"
      configuration:
        map-underscore-to-camel-case: true
        log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    dynamic-data-source:
      strict: false
      seata: false
      p6spy: false
      datasource:
        master1:
          type: MYSQL
          host: localhost
          port: 3306
          database: myapp
          username: root
          password: password
          pool-type: HIKARI
```

### 3. 使用示例

**编程式数据源切换**
```java
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    // 使用主数据源进行写操作
    public User createUser(User user) {
        // 动态切换到主数据源
        DynamicDataSourceContextHolder.setDataSource("master1");
        try {
            return userMapper.insert(user);
        } finally {
            // 清除数据源上下文
            DynamicDataSourceContextHolder.clearDataSource();
        }
    }
    
    // 使用从数据源进行读操作
    public User getUserById(Long id) {
        // 动态切换到从数据源
        DynamicDataSourceContextHolder.setDataSource("slave1");
        try {
            return userMapper.selectById(id);
        } finally {
            // 清除数据源上下文
            DynamicDataSourceContextHolder.clearDataSource();
        }
    }
}
```

**自动数据源路由（推荐）**
```java
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    // 系统会自动根据SQL类型选择数据源：
    // SELECT语句 -> 从库（读操作）
    // INSERT/UPDATE/DELETE语句 -> 主库（写操作）
    
    public User createUser(User user) {
        // 自动使用主数据源（写操作）
        return userMapper.insert(user);
    }
    
    public User getUserById(Long id) {
        // 自动使用从数据源（读操作）
        return userMapper.selectById(id);
    }
}
```

## 配置说明

### 配置结构

```
synapse:
  datasource:
    primary: String                    # 主数据源名称
    mybatis-plus: MybatisPlus         # MyBatis-Plus 配置
    dynamic-data-source: DynamicDataSource # 动态数据源配置
    spring-datasource: SpringDatasource   # 兼容性配置
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

## 版本历史

| 版本 | 更新内容 |
|------|----------|
| 1.0.0 | 初始版本，基础功能 |
| 1.1.0 | 添加 HikariCP 支持 |
| 1.2.0 | 添加 Druid 支持 |
| 1.3.0 | 配置重构，统一管理 |
| 1.4.0 | 添加 Seata 和 P6Spy 支持 |
| 1.5.0 | 移除@DS注解依赖，实现智能数据源路由 |

## 贡献

欢迎提交 Issue 和 Pull Request 来改进这个模块。

## 许可证

本项目采用 MIT 许可证。 