# Synapse Framework 配置指南

本文档详细说明了 Synapse Framework 的所有配置选项和参数。

## 📋 配置概览

Synapse Framework 支持以下配置方式：
- **application.yml** - 主要配置文件
- **application-{profile}.yml** - 环境特定配置
- **Java 代码配置** - 编程式配置
- **环境变量** - 系统环境变量

## 🗄️ 数据库配置

### 动态数据源配置

```yaml
synapse:
  datasource:
    dynamic-data-source:
      primary: master
      datasource:
        master:
          type: MYSQL
          host: localhost
          port: 3306
          database: synapse_demo
          username: root
          password: 123456
          pool-type: HIKARI
          
        # 从库配置（可选）
        slave1:
          type: MYSQL
          host: localhost
          port: 3307
          database: synapse_demo
          username: root
          password: 123456
          pool-type: HIKARI
```

## 🔐 安全配置

### Token 认证配置

```yaml
synapse:
  security:
    # 是否启用安全模块
    enabled: true
    # 安全模式：STRICT(严格)、PERMISSIVE(宽松)、DISABLED(关闭)
    mode: STRICT
    # 白名单配置
    white-list:
      enabled: true
      paths:
        - "/api/auth/login"
        - "/api/public/**"
    # Token 配置
    token:
      # Token 前缀（用于 Authorization 请求头）
      prefix: "Bearer "
      # Token 查询参数名
      query-param: "token"
      # Authorization 请求头名称
      header-name: "Authorization"
      # X-Auth-Token 请求头名称（备用 token 传递方式）
      x-auth-token-header: "X-Auth-Token"
      # Token 过期时间（秒），默认 2 小时
      timeout: 7200
      # 是否启用滑动过期（自动刷新）
      enable-sliding-expiration: true
      # 刷新阈值（秒），当 token 剩余时间少于此值时自动刷新 token
      refresh-threshold: 600  # 10 分钟
      # 续期时长（秒），刷新 token 时将过期时间延长到此值
      renewal-duration: 7200  # 2 小时
```

## 🗃️ 缓存配置

### Redis 配置

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: your_password
      database: 0
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0

# 缓存配置
synapse:
  cache:
    enabled: true
    default-strategy: "LOCAL_AND_REDIS"
    two-level:
      enabled: true
      local:
        enabled: true
        maximum-size: 1000
      redis:
        enabled: true
        default-ttl: 3600
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
