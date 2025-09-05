# Synapse Framework 配置指南

本文档详细说明了 Synapse Framework 的所有配置选项和参数。

## 📋 配置概览

Synapse Framework 支持以下配置方式：
- **application.yml** - 主要配置文件
- **application-{profile}.yml** - 环境特定配置
- **Java 代码配置** - 编程式配置
- **环境变量** - 系统环境变量

## 🗄️ 数据库配置

### 基础数据源配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/synapse_demo
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 动态数据源配置

```yaml
synapse:
  databases:
    primary: master
    dynamic-datasource:
      strict: false
      seata: false
      p6spy: false
      datasource:
        master:
          type: MYSQL
          host: localhost
          port: 3306
          database: synapse_demo
          username: root
          password: 123456
          pool-type: HIKARI
```

## 🔐 安全配置

### Sa-Token 配置

```yaml
sa-token:
  token-name: Authorization
  timeout: 2592000
  activity-timeout: -1
  is-concurrent: true
  is-share: false
  token-style: uuid
  is-log: false
```

## 🗃️ 缓存配置

### Redis 配置

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

## 📊 监控配置

### 健康检查

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

## 🔧 环境特定配置

### 开发环境

```yaml
# application-dev.yml
spring:
  profiles:
    active: dev
logging:
  level:
    com.indigo: DEBUG
```

### 生产环境

```yaml
# application-prod.yml
spring:
  profiles:
    active: prod
logging:
  level:
    com.indigo: WARN
```

## 📝 配置最佳实践

1. **环境分离** - 使用 profile 分离不同环境配置
2. **安全考虑** - 敏感信息使用环境变量
3. **性能优化** - 根据负载调整连接池和缓存配置
4. **配置验证** - 使用 @Validated 验证配置属性

## 🔗 相关文档

- [快速开始](QUICKSTART.md) - 基础配置示例
- [架构设计](ARCHITECTURE.md) - 配置架构说明
- [模块文档](MODULES/) - 各模块配置详情
