# Synapse Framework 配置模板

本文档提供了 Synapse Framework 在不同场景下的配置模板，您可以直接复制使用。

## 🚀 快速开始配置

### **基础配置模板**

```yaml
synapse:
  datasource:
    primary: master
    
    # 读写分离配置
    read-write:
      enabled: true
      read-sources: [slave1, slave2]
      write-sources: [master]
    
    # 负载均衡配置
    load-balance:
      strategy: ROUND_ROBIN
    
    # 故障转移配置
    failover:
      enabled: true
      timeout: 5000
      max-retries: 3
      strategy: PRIMARY_FIRST
    
    # 数据源配置
    datasources:
      master:
        type: MYSQL
        host: localhost
        port: 3306
        database: synapse_demo
        username: root
        password: 123456
        role: WRITE
        
        pool:
          type: HIKARI
          min-idle: 5
          max-size: 20
          connection-timeout: 30000
          idle-timeout: 600000
          max-lifetime: 1800000
          connection-test-query: SELECT 1
          leak-detection-threshold: 60000
          
      slave1:
        type: MYSQL
        host: localhost
        port: 3307
        database: synapse_demo
        username: root
        password: 123456
        role: READ
        
        pool:
          type: HIKARI
          min-idle: 5
          max-size: 15
          
      slave2:
        type: MYSQL
        host: localhost
        port: 3308
        database: synapse_demo
        username: root
        password: 123456
        role: READ
        
        pool:
          type: HIKARI
          min-idle: 5
          max-size: 15
```

## 🏭 生产环境配置

### **高可用生产环境配置**

```yaml
synapse:
  datasource:
    primary: master
    
    read-write:
      enabled: true
      read-sources: [slave1, slave2, slave3]
      write-sources: [master, master2]
    
    load-balance:
      strategy: WEIGHTED
      weights:
        slave1: 100
        slave2: 80
        slave3: 60
    
    failover:
      enabled: true
      timeout: 3000
      max-retries: 5
      strategy: HEALTHY_FIRST
    
    datasources:
      master:
        type: MYSQL
        host: ${DB_MASTER_HOST:master-db}
        port: 3306
        database: ${DB_NAME:synapse_prod}
        username: ${DB_USERNAME:synapse_user}
        password: ${DB_PASSWORD}
        role: WRITE
        weight: 100
        
        pool:
          type: HIKARI
          min-idle: 20
          max-size: 100
          connection-timeout: 30000
          idle-timeout: 600000
          max-lifetime: 1800000
          connection-test-query: SELECT 1
          leak-detection-threshold: 60000
          
      master2:
        type: MYSQL
        host: ${DB_MASTER2_HOST:master2-db}
        port: 3306
        database: ${DB_NAME:synapse_prod}
        username: ${DB_USERNAME:synapse_user}
        password: ${DB_PASSWORD}
        role: WRITE
        weight: 80
        
        pool:
          type: HIKARI
          min-idle: 15
          max-size: 80
          
      slave1:
        type: MYSQL
        host: ${DB_SLAVE1_HOST:slave1-db}
        port: 3306
        database: ${DB_NAME:synapse_prod}
        username: ${DB_USERNAME:synapse_user}
        password: ${DB_PASSWORD}
        role: READ
        weight: 100
        
        pool:
          type: HIKARI
          min-idle: 10
          max-size: 50
          
      slave2:
        type: MYSQL
        host: ${DB_SLAVE2_HOST:slave2-db}
        port: 3306
        database: ${DB_NAME:synapse_prod}
        username: ${DB_USERNAME:synapse_user}
        password: ${DB_PASSWORD}
        role: READ
        weight: 80
        
        pool:
          type: HIKARI
          min-idle: 8
          max-size: 40
          
      slave3:
        type: MYSQL
        host: ${DB_SLAVE3_HOST:slave3-db}
        port: 3306
        database: ${DB_NAME:synapse_prod}
        username: ${DB_USERNAME:synapse_user}
        password: ${DB_PASSWORD}
        role: READ
        weight: 60
        
        pool:
          type: HIKARI
          min-idle: 6
          max-size: 30
```

## 📊 高并发环境配置

### **高并发场景配置**

```yaml
synapse:
  datasource:
    primary: master
    
    read-write:
      enabled: true
      read-sources: [slave1, slave2, slave3, slave4]
      write-sources: [master]
    
    load-balance:
      strategy: ROUND_ROBIN
    
    failover:
      enabled: true
      timeout: 2000
      max-retries: 3
      strategy: PRIMARY_FIRST
    
    datasources:
      master:
        type: MYSQL
        host: ${DB_HOST:master-db}
        port: 3306
        database: ${DB_NAME:synapse_high_concurrency}
        username: ${DB_USERNAME:synapse_user}
        password: ${DB_PASSWORD}
        role: WRITE
        
        pool:
          type: HIKARI
          min-idle: 30
          max-size: 200
          connection-timeout: 20000
          idle-timeout: 300000
          max-lifetime: 900000
          connection-test-query: SELECT 1
          leak-detection-threshold: 30000
          
      slave1:
        type: MYSQL
        host: ${DB_SLAVE1_HOST:slave1-db}
        port: 3306
        database: ${DB_NAME:synapse_high_concurrency}
        username: ${DB_USERNAME:synapse_user}
        password: ${DB_PASSWORD}
        role: READ
        
        pool:
          type: HIKARI
          min-idle: 25
          max-size: 150
          
      slave2:
        type: MYSQL
        host: ${DB_SLAVE2_HOST:slave2-db}
        port: 3306
        database: ${DB_NAME:synapse_high_concurrency}
        username: ${DB_USERNAME:synapse_user}
        password: ${DB_PASSWORD}
        role: READ
        
        pool:
          type: HIKARI
          min-idle: 25
          max-size: 150
          
      slave3:
        type: MYSQL
        host: ${DB_SLAVE3_HOST:slave3-db}
        port: 3306
        database: ${DB_NAME:synapse_high_concurrency}
        username: ${DB_USERNAME:synapse_user}
        password: ${DB_PASSWORD}
        role: READ
        
        pool:
          type: HIKARI
          min-idle: 20
          max-size: 120
          
      slave4:
        type: MYSQL
        host: ${DB_SLAVE4_HOST:slave4-db}
        port: 3306
        database: ${DB_NAME:synapse_high_concurrency}
        username: ${DB_USERNAME:synapse_user}
        password: ${DB_PASSWORD}
        role: READ
        
        pool:
          type: HIKARI
          min-idle: 20
          max-size: 120
```

## 🗄️ 大数据量环境配置

### **大数据量场景配置**

```yaml
synapse:
  datasource:
    primary: master
    
    read-write:
      enabled: true
      read-sources: [slave1, slave2]
      write-sources: [master]
    
    load-balance:
      strategy: WEIGHTED
      weights:
        slave1: 100
        slave2: 80
    
    failover:
      enabled: true
      timeout: 10000
      max-retries: 5
      strategy: HEALTHY_FIRST
    
    datasources:
      master:
        type: MYSQL
        host: ${DB_HOST:master-db}
        port: 3306
        database: ${DB_NAME:synapse_big_data}
        username: ${DB_USERNAME:synapse_user}
        password: ${DB_PASSWORD}
        role: WRITE
        
        pool:
          type: HIKARI
          min-idle: 15
          max-size: 80
          connection-timeout: 60000
          idle-timeout: 300000
          max-lifetime: 900000
          connection-test-query: SELECT 1
          leak-detection-threshold: 120000
          
      slave1:
        type: MYSQL
        host: ${DB_SLAVE1_HOST:slave1-db}
        port: 3306
        database: ${DB_NAME:synapse_big_data}
        username: ${DB_USERNAME:synapse_user}
        password: ${DB_PASSWORD}
        role: READ
        
        pool:
          type: HIKARI
          min-idle: 12
          max-size: 60
          
      slave2:
        type: MYSQL
        host: ${DB_SLAVE2_HOST:slave2-db}
        port: 3306
        database: ${DB_NAME:synapse_big_data}
        username: ${DB_USERNAME:synapse_user}
        password: ${DB_PASSWORD}
        role: READ
        
        pool:
          type: HIKARI
          min-idle: 10
          max-size: 50
```

## 🔧 多数据库类型配置

### **混合数据库环境配置**

```yaml
synapse:
  datasource:
    primary: mysql_master
    
    read-write:
      enabled: true
      read-sources: [mysql_slave1, postgres_slave1]
      write-sources: [mysql_master, postgres_master]
    
    load-balance:
      strategy: ROUND_ROBIN
    
    failover:
      enabled: true
      timeout: 5000
      max-retries: 3
      strategy: PRIMARY_FIRST
    
    datasources:
      mysql_master:
        type: MYSQL
        host: ${MYSQL_HOST:mysql-master}
        port: 3306
        database: ${MYSQL_DB:synapse_mysql}
        username: ${MYSQL_USER:synapse_user}
        password: ${MYSQL_PASSWORD}
        role: WRITE
        
        pool:
          type: HIKARI
          min-idle: 10
          max-size: 50
          
      mysql_slave1:
        type: MYSQL
        host: ${MYSQL_SLAVE1_HOST:mysql-slave1}
        port: 3306
        database: ${MYSQL_DB:synapse_mysql}
        username: ${MYSQL_SLAVE1_USER:synapse_user}
        password: ${MYSQL_SLAVE1_PASSWORD}
        role: READ
        
        pool:
          type: HIKARI
          min-idle: 8
          max-size: 40
          
      postgres_master:
        type: POSTGRESQL
        host: ${POSTGRES_HOST:postgres-master}
        port: 5432
        database: ${POSTGRES_DB:synapse_postgres}
        username: ${POSTGRES_USER:synapse_user}
        password: ${POSTGRES_PASSWORD}
        role: WRITE
        
        pool:
          type: HIKARI
          min-idle: 10
          max-size: 50
          
      postgres_slave1:
        type: POSTGRESQL
        host: ${POSTGRES_SLAVE1_HOST:postgres-slave1}
        port: 5432
        database: ${POSTGRES_DB:synapse_postgres}
        username: ${POSTGRES_SLAVE1_USER:synapse_user}
        password: ${POSTGRES_SLAVE1_PASSWORD}
        role: READ
        
        pool:
          type: HIKARI
          min-idle: 8
          max-size: 40
```

## 🧪 开发环境配置

### **开发环境配置**

```yaml
synapse:
  datasource:
    primary: dev_master
    
    read-write:
      enabled: false  # 开发环境通常不需要读写分离
    
    load-balance:
      strategy: ROUND_ROBIN
    
    failover:
      enabled: false  # 开发环境通常不需要故障转移
    
    datasources:
      dev_master:
        type: MYSQL
        host: localhost
        port: 3306
        database: synapse_dev
        username: root
        password: 123456
        role: READ_WRITE
        
        pool:
          type: HIKARI
          min-idle: 2
          max-size: 10
          connection-timeout: 30000
          idle-timeout: 300000
          max-lifetime: 900000
          connection-test-query: SELECT 1
          leak-detection-threshold: 0  # 开发环境可以禁用泄漏检测
```

## 📝 配置说明

### **配置参数说明**

| 配置项 | 说明 | 默认值 | 建议值 |
|--------|------|--------|--------|
| `primary` | 主数据源名称 | master | 根据实际环境命名 |
| `read-write.enabled` | 是否启用读写分离 | true | 生产环境建议启用 |
| `load-balance.strategy` | 负载均衡策略 | ROUND_ROBIN | 根据需求选择 |
| `failover.enabled` | 是否启用故障转移 | true | 生产环境建议启用 |
| `failover.timeout` | 故障转移超时时间(ms) | 5000 | 根据网络情况调整 |
| `failover.max-retries` | 最大重试次数 | 3 | 根据业务需求调整 |

### **负载均衡策略说明**

| 策略 | 说明 | 适用场景 |
|------|------|----------|
| `ROUND_ROBIN` | 轮询策略 | 数据源性能相近 |
| `WEIGHTED` | 权重策略 | 数据源性能差异较大 |
| `RANDOM` | 随机策略 | 简单负载分散 |

### **故障转移策略说明**

| 策略 | 说明 | 适用场景 |
|------|------|----------|
| `PRIMARY_FIRST` | 主数据源优先 | 主从架构 |
| `HEALTHY_FIRST` | 健康数据源优先 | 多活架构 |
| `ROUND_ROBIN` | 轮询故障转移 | 负载均衡架构 |

## 🔗 相关文档

- [配置指南](CONFIGURATION.md) - 详细配置参数说明
- [快速开始](QUICKSTART.md) - 基础配置示例
- [架构设计](ARCHITECTURE.md) - 配置架构说明
