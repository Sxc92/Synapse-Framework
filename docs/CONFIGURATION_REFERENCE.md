# Synapse Framework 配置参考文档

## 📚 概述

本文档提供了 Synapse Framework 各模块的详细配置参考，包括配置项说明、参数含义、配置示例等。开发者可以通过本文档了解如何正确配置框架的各个功能模块。

## 🏗️ 基础配置结构

### 配置层次结构
```
synapse:
  core:           # 核心模块配置
  datasource:     # 数据源模块配置
  cache:          # 缓存模块配置
  events:         # 事件模块配置
  security:       # 安全模块配置
```

### 环境配置支持
- **开发环境**: `application-dev.yml`
- **测试环境**: `application-test.yml`
- **生产环境**: `application-prod.yml`
- **本地环境**: `application-local.yml`

## 🔧 核心模块配置

### Synapse Core 配置

#### 国际化配置
```yaml
synapse:
  core:
    i18n:
      # 默认语言
      default-locale: zh_CN
      # 支持的语言列表
      supported-locales: zh_CN,en_US
      # 消息文件基础名
      message-basename: i18n/messages
      # 消息缓存大小
      cache-seconds: 3600
      # 是否启用回退语言
      fallback-to-system-locale: true
```

#### 异常处理配置
```yaml
synapse:
  core:
    exception:
      # 是否启用全局异常处理器
      enable-global-handler: true
      # 异常日志级别
      log-level: ERROR
      # 是否包含堆栈信息
      include-stack-trace: false
      # 是否记录异常详情
      log-details: true
      # 异常响应格式
      response-format: JSON
```

#### 上下文管理配置
```yaml
synapse:
  core:
    context:
      # 是否启用请求上下文
      enable-request-context: true
      # 是否启用用户上下文
      enable-user-context: true
      # 上下文超时时间（毫秒）
      context-timeout: 30000
      # 是否启用业务上下文
      enable-business-context: true
      # 上下文清理策略
      cleanup-strategy: AUTO
```

#### 配置管理配置
```yaml
synapse:
  core:
    configuration:
      # 配置刷新间隔（秒）
      refresh-interval: 60
      # 是否启用配置热更新
      enable-hot-reload: true
      # 配置验证策略
      validation-strategy: STRICT
      # 配置加密密钥
      encryption-key: ${SYNAPSE_ENCRYPTION_KEY:}
```

## 🗄️ 数据源模块配置

### 基础数据源配置
```yaml
synapse:
  datasource:
    # 主数据源名称
    primary: master
    # 是否启用自动配置
    auto-configuration: true
    # 数据源类型
    type: DYNAMIC
    # 连接池类型
    pool-type: HIKARI
```

### MyBatis-Plus 配置
```yaml
synapse:
  datasource:
    mybatis-plus:
      # 实体类包路径
      type-aliases-package: com.example.**.entity
      # Mapper XML 文件路径
      mapper-locations: "classpath*:mapper/**/*.xml"
      # 全局配置
      configuration:
        # 下划线转驼峰
        map-underscore-to-camel-case: true
        # 日志实现
        log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
        # 延迟加载
        lazy-loading-enabled: true
        # 积极延迟加载
        aggressive-lazy-loading: false
        # 多结果集支持
        multiple-result-sets-enabled: true
        # 列标签
        use-column-label: true
        # 使用生成的主键
        use-generated-keys: false
        # 自动映射行为
        auto-mapping-behavior: PARTIAL
        # 自动映射未知列
        auto-mapping-unknown-column-behavior: NONE
        # 默认执行器类型
        default-executor-type: SIMPLE
        # 默认语句超时时间
        default-statement-timeout: 25
        # 默认获取结果集超时时间
        default-fetch-size: 100
        # 安全结果处理
        safe-result-handler-enabled: true
        # 安全行边界
        safe-row-bounds-enabled: true
        # 映射下划线到驼峰
        map-underscore-to-camel-case: true
        # 本地缓存作用域
        local-cache-scope: SESSION
        # 调用设置器方法
        call-setters-on-nulls: false
        # 返回结果集
        return-instance-for-empty-row: false
        # 日志前缀
        log-prefix: "MyBatis"
```

### 动态数据源配置
```yaml
synapse:
  datasource:
    dynamic-data-source:
      # 严格模式
      strict: false
      # 是否启用 Seata
      seata: false
      # 是否启用 P6Spy
      p6spy: false
      # 数据源配置
      datasource:
        master:
          type: MYSQL
          host: localhost
          port: 3306
          database: myapp
          username: root
          password: password
          pool-type: HIKARI
          # HikariCP 配置
          hikari:
            minimum-idle: 10
            maximum-pool-size: 50
            idle-timeout: 300000
            max-lifetime: 1800000
            connection-timeout: 20000
            connection-test-query: "SELECT 1"
        
        slave1:
          type: MYSQL
          host: slave1.example.com
          port: 3306
          database: myapp
          username: readonly
          password: readonly
          pool-type: HIKARI
          hikari:
            minimum-idle: 5
            maximum-pool-size: 20
            read-only: true
```

### 读写分离配置
```yaml
synapse:
  datasource:
    read-write-split:
      # 是否启用读写分离
      enabled: true
      # 主库名称
      master: master
      # 从库名称列表
      slaves: slave1,slave2
      # 负载均衡策略
      load-balance-strategy: ROUND_ROBIN
      # 是否启用故障转移
      failover-enabled: true
      # 健康检查间隔（秒）
      health-check-interval: 30
```

## 🚀 缓存模块配置

### 基础缓存配置
```yaml
synapse:
  cache:
    # 默认缓存类型
    default-type: REDIS
    # 缓存键前缀
    key-prefix: "synapse:"
    # 默认过期时间（秒）
    default-ttl: 3600
    # 最大过期时间（秒）
    max-ttl: 86400
    # 最小过期时间（秒）
    min-ttl: 60
    # 是否启用缓存统计
    enable-statistics: true
    # 缓存监控间隔（秒）
    monitor-interval: 60
```

### Redis 缓存配置
```yaml
synapse:
  cache:
    redis:
      # Redis 服务器地址
      host: localhost
      # Redis 端口
      port: 6379
      # Redis 密码
      password: 
      # Redis 数据库索引
      database: 0
      # 连接超时时间（毫秒）
      timeout: 3000
      # 连接池配置
      lettuce:
        pool:
          # 最大活跃连接数
          max-active: 8
          # 最大空闲连接数
          max-idle: 8
          # 最小空闲连接数
          min-idle: 0
          # 获取连接最大等待时间
          max-wait: -1ms
          # 空闲连接检测间隔
          time-between-eviction-runs: 30000ms
      # 序列化器类型
      serializer: JACKSON
      # 是否启用压缩
      compression: true
      # 压缩阈值（字节）
      compression-threshold: 1024
```

### Caffeine 本地缓存配置
```yaml
synapse:
  cache:
    caffeine:
      # 最大缓存条目数
      maximum-size: 1000
      # 最大权重
      maximum-weight: 10000
      # 写入后过期时间
      expire-after-write: 1h
      # 访问后过期时间
      expire-after-access: 30m
      # 是否记录统计信息
      record-stats: true
      # 是否启用弱引用
      weak-keys: false
      # 是否启用弱值引用
      weak-values: false
      # 是否启用软引用
      soft-values: false
```

### 分布式锁配置
```yaml
synapse:
  cache:
    distributed-lock:
      # 锁超时时间（毫秒）
      timeout: 30000
      # 重试次数
      retry-times: 3
      # 重试间隔（毫秒）
      retry-interval: 1000
      # 锁前缀
      key-prefix: "lock:"
      # 是否启用看门狗机制
      watchdog-enabled: true
      # 看门狗间隔（毫秒）
      watchdog-interval: 10000
      # 锁释放策略
      release-strategy: AUTO
```

## 📡 事件模块配置

### 基础事件配置
```yaml
synapse:
  events:
    # 是否启用事件模块
    enabled: true
    # 事件存储类型
    storage-type: MEMORY
    # 事件保留天数
    retention-days: 30
    # 是否启用事件审计
    audit-enabled: true
    # 事件序列化器
    serializer: JACKSON
```

### 异步事件配置
```yaml
synapse:
  events:
    async:
      # 是否启用异步处理
      enabled: true
      # 核心线程池大小
      core-pool-size: 5
      # 最大线程池大小
      max-pool-size: 20
      # 队列容量
      queue-capacity: 100
      # 线程保持活跃时间（秒）
      keep-alive-seconds: 60
      # 线程名前缀
      thread-name-prefix: "event-async-"
      # 是否等待任务完成
      wait-for-tasks-to-complete-on-shutdown: true
      # 关闭超时时间（秒）
      await-termination-seconds: 60
```

### 分布式事件配置
```yaml
synapse:
  events:
    distributed:
      # 是否启用分布式事件
      enabled: false
      # 消息代理类型
      broker-type: ACTIVEMQ
      # 消息代理地址
      broker-url: tcp://localhost:61616
      # 主题前缀
      topic-prefix: synapse.events
      # 是否启用持久化
      persistence-enabled: true
      # 消息确认模式
      acknowledge-mode: AUTO_ACKNOWLEDGE
```

### 事务事件配置
```yaml
synapse:
  events:
    transaction:
      # 是否启用事务事件
      enabled: true
      # 事务事件传播策略
      propagation: REQUIRED
      # 事务事件隔离级别
      isolation: READ_COMMITTED
      # 事务事件超时时间（秒）
      timeout: 30
      # 是否只读事务
      read-only: false
      # 回滚异常类
      rollback-for: java.lang.Exception
      # 不回滚异常类
      no-rollback-for: java.lang.RuntimeException
```

## 🔒 安全模块配置

### 基础安全配置
```yaml
synapse:
  security:
    # 是否启用安全模块
    enabled: true
    # 安全模式
    mode: STRICT
    # 是否启用安全日志
    security-logging: true
    # 安全日志级别
    security-log-level: INFO
```

### Sa-Token 配置
```yaml
sa-token:
  # Token 名称
  token-name: Authorization
  # Token 有效期（秒）
  timeout: 2592000
  # Token 最低活跃频率（秒）
  active-timeout: -1
  # 是否允许同一账号多地同时登录
  is-concurrent: true
  # 是否共用一个 Token
  is-share: false
  # Token 风格
  token-style: uuid
  # 是否输出操作日志
  is-log: true
  # 是否尝试从请求体里读取 Token
  is-read-body: false
  # 是否尝试从 Cookie 里读取 Token
  is-read-cookie: false
  # 是否尝试从 Header 里读取 Token
  is-read-header: true
  # 是否尝试从 Session 里读取 Token
  is-read-session: false
  # 是否在登录后自动写入 Token 到响应头
  is-write-header: false
  # 是否在登录后自动写入 Token 到响应体
  is-write-body: false
  # 是否在登录后自动写入 Token 到 Cookie
  is-write-cookie: false
  # 是否在登录后自动写入 Token 到 Session
  is-write-session: false
```

### JWT 配置
```yaml
sa-token:
  # JWT 密钥
  jwt-secret-key: your-secret-key
  # JWT 有效期
  jwt-timeout: 2592000
  # JWT 临时有效期
  jwt-activity-timeout: -1
  # JWT 签名算法
  jwt-sign-algorithm: HS256
  # JWT 发行者
  jwt-issuer: synapse-framework
  # JWT 主题
  jwt-subject: user-authentication
  # JWT 受众
  jwt-audience: web-application
```

### 权限配置
```yaml
synapse:
  security:
    permission:
      # 是否启用权限控制
      enabled: true
      # 权限验证模式
      mode: ANNOTATION
      # 默认权限
      default-permissions: "user:read"
      # 超级管理员角色
      super-admin-role: "SUPER_ADMIN"
      # 是否启用角色继承
      role-inheritance: true
      # 权限缓存时间（秒）
      permission-cache-time: 300
```

### 安全防护配置
```yaml
synapse:
  security:
    protection:
      # XSS 防护
      xss:
        enabled: true
        exclude-paths: "/api/public/**"
        filter-mode: ESCAPE
        escape-html: true
      
      # CSRF 防护
      csrf:
        enabled: true
        token-header: "X-CSRF-TOKEN"
        token-parameter: "_csrf"
        exclude-paths: "/api/public/**"
      
      # SQL 注入防护
      sql-injection:
        enabled: true
        filter-mode: STRICT
        exclude-paths: "/api/public/**"
      
      # 请求限流
      rate-limiting:
        enabled: true
        max-requests: 100
        time-window: 60
        exclude-paths: "/api/public/**"
```

## 🌍 环境特定配置

### 开发环境配置
```yaml
# application-dev.yml
synapse:
  core:
    configuration:
      enable-hot-reload: true
      refresh-interval: 10
  
  datasource:
    dynamic-data-source:
      p6spy: true
  
  cache:
    redis:
      host: localhost
      port: 6379
  
  events:
    async:
      core-pool-size: 2
      max-pool-size: 5
  
  security:
    security-log-level: DEBUG
```

### 测试环境配置
```yaml
# application-test.yml
synapse:
  datasource:
    dynamic-data-source:
      datasource:
        master:
          host: test-db.example.com
        slave1:
          host: test-slave.example.com
  
  cache:
    redis:
      host: test-redis.example.com
  
  events:
    storage-type: DATABASE
    retention-days: 7
```

### 生产环境配置
```yaml
# application-prod.yml
synapse:
  core:
    configuration:
      enable-hot-reload: false
      refresh-interval: 300
  
  datasource:
    dynamic-data-source:
      p6spy: false
      seata: true
  
  cache:
    redis:
      host: prod-redis-cluster.example.com
      password: ${REDIS_PASSWORD}
  
  events:
    async:
      core-pool-size: 10
      max-pool-size: 50
      queue-capacity: 500
  
  security:
    security-log-level: WARN
    protection:
      rate-limiting:
        max-requests: 1000
        time-window: 60
```

## 🔍 配置验证

### 配置验证规则
- **必需配置**: 框架运行必需的基础配置
- **可选配置**: 功能增强的可选配置
- **环境配置**: 不同环境的特定配置
- **安全配置**: 敏感信息的配置验证

### 配置验证策略
- **启动验证**: 应用启动时的配置验证
- **运行时验证**: 运行时的配置变更验证
- **配置热更新**: 支持配置的动态更新
- **配置回滚**: 配置错误时的自动回滚

## 📊 配置监控

### 配置监控指标
- **配置加载时间**: 配置文件加载耗时
- **配置验证结果**: 配置验证的成功率
- **配置热更新次数**: 配置动态更新次数
- **配置错误率**: 配置错误的统计

### 配置健康检查
- **配置完整性检查**: 检查必需配置是否完整
- **配置有效性检查**: 检查配置值是否有效
- **配置一致性检查**: 检查配置间的一致性
- **配置安全性检查**: 检查敏感配置的安全性

## 📝 最佳实践

### 配置管理原则
- **环境分离**: 不同环境使用不同的配置文件
- **配置分层**: 按功能模块组织配置结构
- **配置验证**: 启动时验证配置的完整性和有效性
- **配置安全**: 敏感配置使用环境变量或加密存储

### 配置优化建议
- **合理设置缓存**: 根据业务需求设置合适的缓存参数
- **连接池调优**: 根据并发量调整数据库连接池参数
- **异步处理配置**: 根据系统负载调整异步处理参数
- **安全防护配置**: 根据安全要求配置防护策略

## 🐛 常见配置问题

### 配置加载问题
- **配置文件路径错误**: 检查配置文件路径和名称
- **配置格式错误**: 检查YAML语法和缩进
- **配置绑定失败**: 检查配置类字段映射

### 配置验证问题
- **必需配置缺失**: 检查必需配置项是否完整
- **配置值无效**: 检查配置值是否在有效范围内
- **配置冲突**: 检查配置项之间是否存在冲突

## 📚 相关文档

- [Synapse Framework 架构设计](ARCHITECTURE.md)
- [Synapse Framework 使用指南](USAGE_GUIDE.md)
- [Synapse Framework API 参考](API_REFERENCE.md)
- [Synapse Framework 开发笔记](DEVELOPMENT_NOTES.md)

## 🔗 相关链接

- [Spring Boot 配置参考](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html)
- [YAML 语法指南](https://yaml.org/spec/)
- [配置管理最佳实践](https://12factor.net/config)

---

*最后更新时间：2025年08月11日 12:41:56* 