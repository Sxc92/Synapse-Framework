# Seata 延迟初始化配置

## 问题描述

Seata 相关的 Bean 在 Spring Boot 启动时会出现以下警告：

```
Bean 'org.apache.seata.spring.boot.autoconfigure.SeataCoreAutoConfiguration' of type [org.apache.seata.spring.boot.autoconfigure.SeataCoreAutoConfiguration] is not eligible for getting processed by all BeanPostProcessors
```

## 解决方案

### 方案1：全局延迟初始化（推荐）

在 `application.yml` 中添加以下配置：

```yaml
spring:
  main:
    lazy-initialization: true
```

### 方案2：选择性延迟初始化

如果不想全局延迟初始化，可以只对 Seata 相关的 Bean 进行延迟初始化：

```yaml
spring:
  cloud:
    alibaba:
      seata:
        enabled: true
        lazy-initialization: true
```

### 方案3：排除不必要的 Seata 自动配置

如果不需要 Seata 功能，可以排除相关自动配置：

```java
@SpringBootApplication(exclude = {
    SeataAutoConfiguration.class,
    SeataCoreAutoConfiguration.class
})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 方案4：自定义 Seata 配置

```yaml
seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: default_tx_group
  config:
    type: nacos
    nacos:
      server-addr: ${spring.cloud.nacos.discovery.server-addr}
      namespace: ""
      group: SEATA_GROUP
  registry:
    type: nacos
    nacos:
      application: seata-server
      server-addr: ${spring.cloud.nacos.discovery.server-addr}
      namespace: ""
      group: SEATA_GROUP
```

## 最佳实践

### 开发环境
```yaml
spring:
  main:
    lazy-initialization: false  # 开发环境立即初始化，便于调试
```

### 生产环境
```yaml
spring:
  main:
    lazy-initialization: true   # 生产环境延迟初始化，提高启动速度
```

### 测试环境
```yaml
spring:
  main:
    lazy-initialization: true   # 测试环境延迟初始化
```

## 注意事项

1. **功能影响**：延迟初始化不会影响 Seata 的分布式事务功能
2. **性能影响**：首次使用分布式事务时可能会有轻微的延迟
3. **监控**：建议监控首次事务的性能表现

## 验证方法

启动应用后检查日志，应该不再出现 Seata 相关的 BeanPostProcessor 警告。
