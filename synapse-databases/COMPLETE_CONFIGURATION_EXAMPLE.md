# Synapse Databases 完整配置示例

## 基础配置

```yaml
# Synapse 数据库模块配置
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
      strategy: CAMEL_TO_UNDERLINE  # CAMEL_TO_UNDERLINE, CAMEL_TO_KEBAB_CASE, NO_CONVERSION, CUSTOM
      # 自定义转换配置（当strategy为CUSTOM时使用）
      custom-pattern:
        field-to-column-pattern: "([A-Z])"
        field-to-column-replacement: "_$1"
        column-to-field-pattern: "_([a-z])"
        column-to-field-replacement: "$1"

    # 故障转移配置
    failover:
      enabled: true
      max-retry-times: 3
      retry-interval: 1000
      detection-interval: 5000
      recovery-interval: 10000
      strategy: PRIMARY_FIRST  # PRIMARY_FIRST, HEALTHY_FIRST, ROUND_ROBIN

    # Seata分布式事务配置
    seata:
      enabled: false

    # 数据源配置
    datasources:
      # 主数据源
      master1:
        type: MYSQL
        host: localhost
        port: 3306
        database: synapse_main
        username: root
        password: password
        pool-type: HIKARI
        params:
          useUnicode: "true"
          characterEncoding: "utf8"
          useSSL: "false"
          allowPublicKeyRetrieval: "true"
          serverTimezone: "Asia/Shanghai"
          autoReconnect: "true"
          failOverReadOnly: "false"
          maxReconnects: "3"
          initialTimeout: "2"
        hikari:
          minimum-idle: 5
          maximum-pool-size: 15
          idle-timeout: 30000
          max-lifetime: 1800000
          connection-timeout: 30000
          connection-test-query: "SELECT 1"
          connection-init-sql: ""
          validation-timeout: 5000
          leak-detection-threshold: 0
          register-mbeans: false

      # 从数据源（读写分离）
      slave1:
        type: MYSQL
        host: localhost
        port: 3307
        database: synapse_main
        username: root
        password: password
        pool-type: HIKARI
        params:
          useUnicode: "true"
          characterEncoding: "utf8"
          useSSL: "false"
          allowPublicKeyRetrieval: "true"
          serverTimezone: "Asia/Shanghai"
        hikari:
          minimum-idle: 3
          maximum-pool-size: 10
          idle-timeout: 30000
          max-lifetime: 1800000
          connection-timeout: 30000
          connection-test-query: "SELECT 1"

      # 分析数据源
      analytics:
        type: MYSQL
        host: analytics-server
        port: 3306
        database: synapse_analytics
        username: analytics_user
        password: analytics_password
        pool-type: HIKARI
        hikari:
          minimum-idle: 2
          maximum-pool-size: 8
          idle-timeout: 60000
          max-lifetime: 3600000
          connection-timeout: 30000
          connection-test-query: "SELECT 1"

# Spring Boot 兼容性配置
spring:
  datasource:
    dynamic:
      primary: master1
      strict: false
      seata: false
      p6spy: false
      datasource:
        master1:
          type: MYSQL
          pool-type: HIKARI
          host: localhost
          port: 3306
          database: synapse_main
          username: root
          password: password
          params:
            useUnicode: "true"
            characterEncoding: "utf8"
            useSSL: "false"
            allowPublicKeyRetrieval: "true"
            serverTimezone: "Asia/Shanghai"
          hikari:
            minimum-idle: 5
            maximum-pool-size: 15
            idle-timeout: 30000
            max-lifetime: 1800000
            connection-timeout: 30000
            connection-test-query: "SELECT 1"

# 日志配置
logging:
  level:
    root: INFO
    com.indigo: INFO
    com.indigo.databases: INFO
    com.indigo.databases.utils: DEBUG
    com.indigo.databases.routing: DEBUG
    com.indigo.databases.service: DEBUG
    org.springframework.jdbc: DEBUG
    org.mybatis: DEBUG

# MyBatis-Plus 全局配置（兼容性）
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    map-underscore-to-camel-case: true
  global-config:
    banner: false
```

## 环境特定配置

### 开发环境 (application-dev.yml)

```yaml
synapse:
  datasource:
    mybatis-plus:
      configuration:
        log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    datasources:
      master1:
        host: dev-db-server
        database: synapse_dev
        username: dev_user
        password: dev_password
        hikari:
          minimum-idle: 2
          maximum-pool-size: 5
    failover:
      enabled: false  # 开发环境可以禁用故障转移

logging:
  level:
    com.indigo.databases: DEBUG
```

### 测试环境 (application-test.yml)

```yaml
synapse:
  datasource:
    datasources:
      master1:
        host: test-db-server
        database: synapse_test
        username: test_user
        password: test_password
        hikari:
          minimum-idle: 3
          maximum-pool-size: 8
    failover:
      enabled: true
      strategy: HEALTHY_FIRST

logging:
  level:
    com.indigo.databases: INFO
```

### 生产环境 (application-prod.yml)

```yaml
synapse:
  datasource:
    mybatis-plus:
      configuration:
        log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl
    datasources:
      master1:
        host: prod-master-db
        database: synapse_prod
        username: prod_user
        password: ${DB_PASSWORD:prod_password}
        hikari:
          minimum-idle: 10
          maximum-pool-size: 20
          idle-timeout: 300000
          max-lifetime: 1800000
          leak-detection-threshold: 60000
      slave1:
        host: prod-slave-db
        database: synapse_prod
        username: prod_user
        password: ${DB_PASSWORD:prod_password}
        hikari:
          minimum-idle: 5
          maximum-pool-size: 15
    failover:
      enabled: true
      strategy: HEALTHY_FIRST
      max-retry-times: 5
      retry-interval: 2000
    seata:
      enabled: true

logging:
  level:
    com.indigo.databases: WARN
    org.springframework.jdbc: WARN
```

## 高级配置示例

### 读写分离配置

```yaml
synapse:
  datasource:
    datasources:
      # 主库（写）
      master1:
        type: MYSQL
        host: master-db-server
        port: 3306
        database: synapse_main
        username: master_user
        password: master_password
        pool-type: HIKARI
        hikari:
          minimum-idle: 8
          maximum-pool-size: 20
          connection-timeout: 30000
          connection-test-query: "SELECT 1"
      
      # 从库1（读）
      slave1:
        type: MYSQL
        host: slave1-db-server
        port: 3306
        database: synapse_main
        username: slave_user
        password: slave_password
        pool-type: HIKARI
        hikari:
          minimum-idle: 5
          maximum-pool-size: 15
          connection-timeout: 30000
          connection-test-query: "SELECT 1"
      
      # 从库2（读）
      slave2:
        type: MYSQL
        host: slave2-db-server
        port: 3306
        database: synapse_main
        username: slave_user
        password: slave_password
        pool-type: HIKARI
        hikari:
          minimum-idle: 5
          maximum-pool-size: 15
          connection-timeout: 30000
          connection-test-query: "SELECT 1"
    
    failover:
      enabled: true
      strategy: HEALTHY_FIRST
      max-retry-times: 3
      retry-interval: 1000
```

### 多数据库配置

```yaml
synapse:
  datasource:
    datasources:
      # 主业务数据库
      business:
        type: MYSQL
        host: business-db-server
        port: 3306
        database: synapse_business
        username: business_user
        password: business_password
        pool-type: HIKARI
        hikari:
          minimum-idle: 10
          maximum-pool-size: 20
      
      # 用户数据库
      user:
        type: MYSQL
        host: user-db-server
        port: 3306
        database: synapse_user
        username: user_user
        password: user_password
        pool-type: HIKARI
        hikari:
          minimum-idle: 5
          maximum-pool-size: 15
      
      # 日志数据库
      log:
        type: MYSQL
        host: log-db-server
        port: 3306
        database: synapse_log
        username: log_user
        password: log_password
        pool-type: HIKARI
        hikari:
          minimum-idle: 3
          maximum-pool-size: 10
      
      # 分析数据库
      analytics:
        type: MYSQL
        host: analytics-db-server
        port: 3306
        database: synapse_analytics
        username: analytics_user
        password: analytics_password
        pool-type: HIKARI
        hikari:
          minimum-idle: 2
          maximum-pool-size: 8
```

### 字段转换策略配置

#### 驼峰转下划线（默认）

```yaml
synapse:
  datasource:
    field-conversion:
      enabled: true
      strategy: CAMEL_TO_UNDERLINE
```

#### 驼峰转短横线

```yaml
synapse:
  datasource:
    field-conversion:
      enabled: true
      strategy: CAMEL_TO_KEBAB_CASE
```

#### 无转换

```yaml
synapse:
  datasource:
    field-conversion:
      enabled: true
      strategy: NO_CONVERSION
```

#### 自定义转换

```yaml
synapse:
  datasource:
    field-conversion:
      enabled: true
      strategy: CUSTOM
      custom-pattern:
        # 字段名转列名：驼峰转下划线
        field-to-column-pattern: "([A-Z])"
        field-to-column-replacement: "_$1"
        # 列名转字段名：下划线转驼峰
        column-to-field-pattern: "_([a-z])"
        column-to-field-replacement: "$1"
```

### 故障转移策略配置

#### 主数据源优先

```yaml
synapse:
  datasource:
    failover:
      enabled: true
      strategy: PRIMARY_FIRST
      max-retry-times: 3
      retry-interval: 1000
```

#### 健康数据源优先

```yaml
synapse:
  datasource:
    failover:
      enabled: true
      strategy: HEALTHY_FIRST
      max-retry-times: 5
      retry-interval: 2000
      detection-interval: 5000
      recovery-interval: 10000
```

#### 轮询故障转移

```yaml
synapse:
  datasource:
    failover:
      enabled: true
      strategy: ROUND_ROBIN
      max-retry-times: 3
      retry-interval: 1000
```

## 性能优化配置

### 高并发配置

```yaml
synapse:
  datasource:
    datasources:
      master1:
        hikari:
          minimum-idle: 20
          maximum-pool-size: 50
          idle-timeout: 300000
          max-lifetime: 1800000
          connection-timeout: 30000
          validation-timeout: 5000
          leak-detection-threshold: 60000
```

### 低延迟配置

```yaml
synapse:
  datasource:
    datasources:
      master1:
        hikari:
          minimum-idle: 10
          maximum-pool-size: 20
          idle-timeout: 60000
          max-lifetime: 600000
          connection-timeout: 10000
          connection-test-query: "SELECT 1"
```

### 内存优化配置

```yaml
synapse:
  datasource:
    datasources:
      master1:
        hikari:
          minimum-idle: 2
          maximum-pool-size: 8
          idle-timeout: 300000
          max-lifetime: 1800000
          connection-timeout: 30000
```

## 监控和调试配置

### 开发调试

```yaml
synapse:
  databases:
    sql-annotation:
      enabled: true
      debug: true
    auto-repository:
      debug: true

logging:
  level:
    com.indigo.databases: DEBUG
    com.indigo.databases.utils: DEBUG
    com.indigo.databases.routing: DEBUG
    org.springframework.jdbc: DEBUG
    org.mybatis: DEBUG
```

### 生产监控

```yaml
synapse:
  datasource:
    mybatis-plus:
      configuration:
        log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl

logging:
  level:
    com.indigo.databases: INFO
    org.springframework.jdbc: WARN
```

## 配置验证

### 配置检查

```java
@Component
public class ConfigurationValidator {
    
    @Autowired
    private SynapseDataSourceProperties properties;
    
    @EventListener(ApplicationReadyEvent.class)
    public void validateConfiguration() {
        log.info("Validating Synapse Databases configuration...");
        
        // 检查主数据源
        if (!properties.getDatasources().containsKey(properties.getPrimary())) {
            throw new IllegalStateException("Primary datasource not found: " + properties.getPrimary());
        }
        
        // 检查字段转换配置
        if (properties.getFieldConversion().isEnabled()) {
            log.info("Field conversion enabled: {}", properties.getFieldConversion().getStrategy());
        }
        
        // 检查故障转移配置
        if (properties.getFailover().isEnabled()) {
            log.info("Failover enabled: {}", properties.getFailover().getStrategy());
        }
        
        log.info("Configuration validation completed successfully");
    }
}
```

---

## 配置说明

### 配置优先级

1. **环境特定配置** (application-{profile}.yml)
2. **主配置文件** (application.yml)
3. **默认配置** (代码中的默认值)

### 配置热更新

某些配置支持运行时更新：

```java
@Autowired
private FieldConversionService fieldConversionService;

public void updateFieldConversionStrategy(FieldConversionStrategyType strategy) {
    // 动态更新字段转换策略
    fieldConversionService.updateStrategy(strategy);
}
```

### 配置最佳实践

1. **生产环境**：禁用调试日志，启用故障转移
2. **开发环境**：启用调试日志，可以禁用故障转移
3. **测试环境**：使用独立的测试数据库
4. **连接池**：根据并发量调整连接池大小
5. **监控**：启用必要的监控和日志记录

---

**Synapse Databases** - 灵活、强大的数据库配置管理！
