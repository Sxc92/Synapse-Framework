# Synapse Framework - 数据库模块

## 概述

Synapse Framework 数据库模块是一个集成了 MyBatis-Plus 和动态数据源的强大数据库解决方案。它提供了灵活的配置选项，支持多种数据库类型和连接池，并且兼容标准的 Spring Boot 配置格式。

## 特性

- 🚀 **MyBatis-Plus 集成**: 完整的 MyBatis-Plus 配置支持
- 🔄 **动态数据源**: 支持多数据源动态切换
- 🗄️ **多数据库支持**: MySQL, PostgreSQL, Oracle, SQL Server, H2
- 🏊 **连接池支持**: HikariCP, Druid
- ⚙️ **灵活配置**: 支持自定义配置和默认值
- 🔌 **Spring Boot 兼容**: 兼容标准 Spring Boot 配置格式

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

## 数据库类型支持

| 数据库类型 | 枚举值 | 驱动类 | 默认端口 |
|------------|--------|---------|----------|
| MySQL | MYSQL | com.mysql.cj.jdbc.Driver | 3306 |
| PostgreSQL | POSTGRESQL | org.postgresql.Driver | 5432 |
| Oracle | ORACLE | oracle.jdbc.OracleDriver | 1521 |
| SQL Server | SQLSERVER | com.microsoft.sqlserver.jdbc.SQLServerDriver | 1433 |
| H2 | H2 | org.h2.Driver | 8082 |

## 连接池类型支持

| 连接池类型 | 枚举值 | 说明 |
|------------|--------|------|
| HikariCP | HIKARI | 高性能连接池，Spring Boot 默认 |
| Druid | DRUID | 阿里巴巴开源连接池，功能丰富 |

## 使用示例

### 1. 基本配置

```yaml
synapse:
  datasource:
    mybatis-plus:
      configuration:
        log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
        map-underscore-to-camel-case: true
      global-config:
        banner: false
        enable-pagination: true
    primary: master
    dynamic-data-source:
      datasource:
        master:
          type: MYSQL
          host: localhost
          port: 3306
          database: test_db
          username: root
          password: password
          pool-type: HIKARI
```

### 2. 多数据源配置

```yaml
synapse:
  datasource:
    dynamic-data-source:
      primary: master
      datasource:
        master:
          type: MYSQL
          host: master-host
          database: master_db
          username: user
          password: pass
          pool-type: HIKARI
        slave:
          type: MYSQL
          host: slave-host
          database: slave_db
          username: user
          password: pass
          pool-type: HIKARI
```

### 3. 高级连接池配置

```yaml
synapse:
  datasource:
    dynamic-data-source:
      datasource:
        master:
          type: MYSQL
          host: localhost
          database: test_db
          username: root
          password: password
          pool-type: HIKARI
          hikari:
            minimum-idle: 10
            maximum-pool-size: 50
            idle-timeout: 600000
            max-lifetime: 3600000
            connection-timeout: 60000
            connection-test-query: "SELECT 1"
            leak-detection-threshold: 300000
```

## 代码中使用

### 动态切换数据源

```java
@Service
public class UserService {
    
    @DS("slave") // 使用 @DS 注解切换数据源
    public List<User> getUsersFromSlave() {
        return userMapper.selectList(null);
    }
    
    @DS("master") // 切换到主数据源
    public void saveUser(User user) {
        userMapper.insert(user);
    }
}
```

### 编程式切换数据源

```java
@Service
public class UserService {
    
    public List<User> getUsersFromSlave() {
        DynamicDataSourceContextHolder.setDataSource("slave");
        try {
            return userMapper.selectList(null);
        } finally {
            DynamicDataSourceContextHolder.clearDataSource();
        }
    }
}
```

## 配置属性说明

### MyBatis-Plus 配置

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `log-impl` | String | `org.apache.ibatis.logging.stdout.StdOutImpl` | 日志实现类 |
| `map-underscore-to-camel-case` | boolean | `true` | 下划线转驼峰 |
| `cache-enabled` | boolean | `true` | 缓存启用 |
| `lazy-loading-enabled` | boolean | `true` | 延迟加载启用 |

### 动态数据源配置

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `primary` | String | `master1` | 主数据源名称 |
| `strict` | boolean | `false` | 是否启用严格模式 |
| `seata` | boolean | `false` | 是否启用Seata分布式事务 |
| `p6spy` | boolean | `false` | 是否启用P6Spy |

### HikariCP 配置

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `minimum-idle` | Integer | `5` | 最小空闲连接数 |
| `maximum-pool-size` | Integer | `15` | 最大连接池大小 |
| `idle-timeout` | Long | `30000` | 空闲超时时间(毫秒) |
| `max-lifetime` | Long | `1800000` | 最大生命周期(毫秒) |

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

本项目采用 Apache License 2.0 许可证。 