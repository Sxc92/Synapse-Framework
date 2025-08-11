# Synapse Databases 性能优化指南

## 📖 概述

本文档介绍 Synapse Databases 模块的性能优化策略，包括数据库连接池优化、查询性能优化、分库分表、读写分离等高级特性。通过合理的配置和优化，可以显著提升数据库访问性能。

## 🚀 连接池优化

### 1. HikariCP 配置优化

HikariCP 是 Spring Boot 默认的连接池，具有高性能和低延迟的特点。

```yaml
spring:
  datasource:
    hikari:
      # 连接池配置
      maximum-pool-size: 20          # 最大连接数
      minimum-idle: 5                # 最小空闲连接数
      connection-timeout: 30000      # 连接超时时间（毫秒）
      idle-timeout: 600000           # 空闲连接超时时间（毫秒）
      max-lifetime: 1800000          # 连接最大生命周期（毫秒）
      
      # 性能优化配置
      leak-detection-threshold: 60000  # 连接泄漏检测阈值
      connection-test-query: "SELECT 1"  # 连接测试查询
      validation-timeout: 5000       # 验证超时时间
      
      # 连接池监控
      pool-name: "SynapseHikariCP"
      register-mbeans: true          # 注册JMX监控
```

### 2. 连接池参数调优建议

```java
@Configuration
public class DataSourceConfig {
    
    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        
        // 基础配置
        config.setJdbcUrl("jdbc:mysql://localhost:3306/synapse");
        config.setUsername("synapse");
        config.setPassword("password");
        
        // 连接池大小配置
        // 计算公式：((核心数 * 2) + 有效磁盘数)
        int cpuCores = Runtime.getRuntime().availableProcessors();
        config.setMaximumPoolSize(cpuCores * 2 + 2);
        config.setMinimumIdle(cpuCores);
        
        // 连接生命周期配置
        config.setMaxLifetime(1800000);        // 30分钟
        config.setIdleTimeout(600000);         // 10分钟
        config.setConnectionTimeout(30000);    // 30秒
        
        // 性能优化配置
        config.setLeakDetectionThreshold(60000);
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        
        // 连接池监控
        config.setPoolName("SynapseHikariCP");
        config.setRegisterMbeans(true);
        
        return new HikariDataSource(config);
    }
}
```

### 3. 连接池监控

```java
@Component
public class DataSourceMonitor {
    
    @Autowired
    private DataSource dataSource;
    
    @Scheduled(fixedRate = 30000) // 每30秒监控一次
    public void monitorDataSource() {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
            
            log.info("连接池状态 - 活跃连接: {}, 空闲连接: {}, 总连接: {}",
                    poolMXBean.getActiveConnections(),
                    poolMXBean.getIdleConnections(),
                    poolMXBean.getTotalConnections());
            
            // 检查连接池健康状态
            if (poolMXBean.getActiveConnections() > poolMXBean.getMaximumPoolSize() * 0.8) {
                log.warn("连接池使用率过高: {}%", 
                        (double) poolMXBean.getActiveConnections() / poolMXBean.getMaximumPoolSize() * 100);
            }
        }
    }
}
```

## 🔍 查询性能优化

### 1. MyBatis-Plus 查询优化

#### 分页查询优化
```java
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 优化分页查询
     */
    public IPage<User> getUsersOptimized(Page<User> page, UserQuery query) {
        // 使用 count 查询优化
        page.setSearchCount(true);
        
        // 设置分页参数
        page.setCurrent(query.getPageNum());
        page.setSize(query.getPageSize());
        
        // 构建查询条件
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.isNotBlank(query.getUsername()), User::getUsername, query.getUsername())
               .eq(query.getStatus() != null, User::getStatus, query.getStatus())
               .orderByDesc(User::getCreateTime);
        
        // 执行分页查询
        return userMapper.selectPage(page, wrapper);
    }
    
    /**
     * 流式查询大数据量
     */
    public void processLargeDataStream(UserQuery query, Consumer<User> processor) {
        // 使用流式查询避免内存溢出
        userMapper.selectStream(query, processor);
    }
}
```

#### 批量操作优化
```java
@Service
public class BatchOperationService {
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 批量插入优化
     */
    @Transactional
    public void batchInsertUsers(List<User> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        
        // 分批处理，避免单次事务过大
        int batchSize = 1000;
        for (int i = 0; i < users.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, users.size());
            List<User> batch = users.subList(i, endIndex);
            
            // 使用 MyBatis-Plus 的批量插入
            userMapper.insertBatch(batch);
        }
    }
    
    /**
     * 批量更新优化
     */
    @Transactional
    public void batchUpdateUsers(List<User> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        
        // 使用 CASE WHEN 语句进行批量更新
        userMapper.batchUpdate(users);
    }
}
```

### 2. SQL 查询优化

#### 索引优化
```sql
-- 为常用查询字段创建索引
CREATE INDEX idx_user_username ON user(username);
CREATE INDEX idx_user_status_create_time ON user(status, create_time);
CREATE INDEX idx_user_email ON user(email);

-- 复合索引优化
CREATE INDEX idx_user_composite ON user(status, user_type, create_time);

-- 覆盖索引优化
CREATE INDEX idx_user_covering ON user(status, username, email, create_time);
```

#### 查询语句优化
```java
@Repository
public class OptimizedUserRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 使用原生SQL优化复杂查询
     */
    public List<UserDTO> getUsersWithStats() {
        String sql = """
            SELECT 
                u.id,
                u.username,
                u.email,
                u.status,
                u.create_time,
                COUNT(o.id) as order_count,
                SUM(o.amount) as total_amount
            FROM user u
            LEFT JOIN orders o ON u.id = o.user_id
            WHERE u.status = 'ACTIVE'
            GROUP BY u.id, u.username, u.email, u.status, u.create_time
            HAVING COUNT(o.id) > 0
            ORDER BY total_amount DESC
            LIMIT 100
            """;
        
        return jdbcTemplate.query(sql, new UserWithStatsRowMapper());
    }
    
    /**
     * 使用存储过程优化复杂业务逻辑
     */
    public void processUserData(Long userId) {
        String sql = "CALL process_user_data(?)";
        jdbcTemplate.call(con -> {
            CallableStatement cs = con.prepareCall(sql);
            cs.setLong(1, userId);
            return cs;
        });
    }
}
```

## 🗄️ 分库分表优化

### 1. 分库分表策略

#### 水平分表策略
```java
@Component
public class TableShardingStrategy {
    
    /**
     * 基于用户ID的分表策略
     */
    public String getTableName(Long userId, String baseTableName) {
        // 使用取模分表
        int tableIndex = (int) (userId % 16);
        return String.format("%s_%02d", baseTableName, tableIndex);
    }
    
    /**
     * 基于时间的分表策略
     */
    public String getTableNameByTime(LocalDateTime dateTime, String baseTableName) {
        // 按月分表
        String month = dateTime.format(DateTimeFormatter.ofPattern("yyyyMM"));
        return String.format("%s_%s", baseTableName, month);
    }
    
    /**
     * 基于哈希的分表策略
     */
    public String getTableNameByHash(String key, String baseTableName, int tableCount) {
        int hash = Math.abs(key.hashCode());
        int tableIndex = hash % tableCount;
        return String.format("%s_%02d", baseTableName, tableIndex);
    }
}
```

#### 分库策略
```java
@Component
public class DatabaseShardingStrategy {
    
    /**
     * 基于用户ID的分库策略
     */
    public String getDatabaseName(Long userId) {
        // 使用取模分库
        int dbIndex = (int) (userId % 4);
        return String.format("synapse_db_%d", dbIndex);
    }
    
    /**
     * 基于业务类型的分库策略
     */
    public String getDatabaseNameByBusiness(String businessType) {
        switch (businessType) {
            case "USER":
                return "synapse_user_db";
            case "ORDER":
                return "synapse_order_db";
            case "PRODUCT":
                return "synapse_product_db";
            default:
                return "synapse_main_db";
        }
    }
}
```

### 2. 分库分表实现

#### 动态数据源路由
```java
@Component
public class DynamicDataSourceRouter extends AbstractRoutingDataSource {
    
    @Override
    protected Object determineCurrentLookupKey() {
        return DynamicDataSourceContextHolder.getDataSourceType();
    }
    
    @Override
    protected DataSource determineTargetDataSource() {
        String dataSourceType = DynamicDataSourceContextHolder.getDataSourceType();
        DataSource dataSource = getResolvedDataSources().get(dataSourceType);
        
        if (dataSource == null) {
            throw new IllegalStateException("数据源不存在: " + dataSourceType);
        }
        
        return dataSource;
    }
}

@Component
public class DynamicDataSourceAspect {
    
    @Around("@annotation(dynamicDataSource)")
    public Object around(ProceedingJoinPoint point, DynamicDataSource dynamicDataSource) throws Throwable {
        try {
            // 设置数据源
            DynamicDataSourceContextHolder.setDataSourceType(dynamicDataSource.value());
            return point.proceed();
        } finally {
            // 清除数据源
            DynamicDataSourceContextHolder.clearDataSourceType();
        }
    }
}
```

#### 分表查询实现
```java
@Service
public class ShardingUserService {
    
    @Autowired
    private TableShardingStrategy tableShardingStrategy;
    
    @Autowired
    private DatabaseShardingStrategy databaseShardingStrategy;
    
    /**
     * 分库分表查询
     */
    public User getUserById(Long userId) {
        // 确定数据源
        String dataSourceType = databaseShardingStrategy.getDatabaseName(userId);
        DynamicDataSourceContextHolder.setDataSourceType(dataSourceType);
        
        try {
            // 确定表名
            String tableName = tableShardingStrategy.getTableName(userId, "user");
            
            // 执行查询
            return userMapper.selectByIdFromTable(userId, tableName);
        } finally {
            DynamicDataSourceContextHolder.clearDataSourceType();
        }
    }
    
    /**
     * 跨表查询
     */
    public List<User> getUsersByIds(List<Long> userIds) {
        Map<String, List<Long>> tableGroups = new HashMap<>();
        
        // 按表分组用户ID
        for (Long userId : userIds) {
            String tableName = tableShardingStrategy.getTableName(userId, "user");
            tableGroups.computeIfAbsent(tableName, k -> new ArrayList<>()).add(userId);
        }
        
        List<User> result = new ArrayList<>();
        
        // 分别查询每个表
        for (Map.Entry<String, List<Long>> entry : tableGroups.entrySet()) {
            String tableName = entry.getKey();
            List<Long> ids = entry.getValue();
            
            List<User> users = userMapper.selectByIdsFromTable(ids, tableName);
            result.addAll(users);
        }
        
        return result;
    }
}
```

## 🔄 读写分离优化

### 1. 读写分离配置

```yaml
spring:
  datasource:
    # 主数据源（写）
    master:
      jdbc-url: jdbc:mysql://master:3306/synapse
      username: synapse
      password: password
      driver-class-name: com.mysql.cj.jdbc.Driver
      
    # 从数据源（读）
    slave:
      jdbc-url: jdbc:mysql://slave:3306/synapse
      username: synapse
      password: password
      driver-class-name: com.mysql.cj.jdbc.Driver
      
    # 读写分离配置
    routing:
      enabled: true
      write-datasource: master
      read-datasources: slave
      load-balance-strategy: ROUND_ROBIN  # ROUND_ROBIN, WEIGHTED, RANDOM
```

### 2. 读写分离实现

```java
@Configuration
public class ReadWriteSeparationConfig {
    
    @Bean
    @Primary
    public DataSource dataSource() {
        ReadWriteSeparationDataSource dataSource = new ReadWriteSeparationDataSource();
        
        // 设置写数据源
        dataSource.setWriteDataSource(masterDataSource());
        
        // 设置读数据源
        dataSource.setReadDataSources(Arrays.asList(slaveDataSource1(), slaveDataSource2()));
        
        // 设置负载均衡策略
        dataSource.setLoadBalanceStrategy(new RoundRobinLoadBalanceStrategy());
        
        return dataSource;
    }
    
    @Bean
    public DataSource masterDataSource() {
        // 主数据源配置
        return createDataSource("master");
    }
    
    @Bean
    public DataSource slaveDataSource1() {
        // 从数据源1配置
        return createDataSource("slave1");
    }
    
    @Bean
    public DataSource slaveDataSource2() {
        // 从数据源2配置
        return createDataSource("slave2");
    }
}

@Component
public class ReadWriteSeparationAspect {
    
    @Around("@annotation(readOnly)")
    public Object around(ProceedingJoinPoint point, ReadOnly readOnly) throws Throwable {
        try {
            // 设置读数据源
            DataSourceContextHolder.setDataSourceType(DataSourceType.READ);
            return point.proceed();
        } finally {
            // 清除数据源类型
            DataSourceContextHolder.clearDataSourceType();
        }
    }
}
```

### 3. 负载均衡策略

```java
public interface LoadBalanceStrategy {
    DataSource select(List<DataSource> dataSources);
}

@Component
public class RoundRobinLoadBalanceStrategy implements LoadBalanceStrategy {
    
    private final AtomicInteger counter = new AtomicInteger(0);
    
    @Override
    public DataSource select(List<DataSource> dataSources) {
        if (dataSources == null || dataSources.isEmpty()) {
            throw new IllegalArgumentException("数据源列表不能为空");
        }
        
        int index = counter.incrementAndGet() % dataSources.size();
        return dataSources.get(index);
    }
}

@Component
public class WeightedLoadBalanceStrategy implements LoadBalanceStrategy {
    
    @Override
    public DataSource select(List<DataSource> dataSources) {
        // 实现加权轮询算法
        return weightedRoundRobin(dataSources);
    }
    
    private DataSource weightedRoundRobin(List<DataSource> dataSources) {
        // 加权轮询实现
        // 这里简化实现，实际应该考虑数据源的权重配置
        return dataSources.get(0);
    }
}
```

## 📊 性能监控

### 1. 数据库性能监控

```java
@Component
public class DatabasePerformanceMonitor {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    /**
     * 监控SQL执行时间
     */
    public void recordSqlExecutionTime(String sql, long executionTime) {
        Timer.builder("database.sql.execution.time")
            .tag("sql", sql)
            .register(meterRegistry)
            .record(executionTime, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 监控连接池状态
     */
    public void recordConnectionPoolStatus(String dataSource, int active, int idle, int total) {
        Gauge.builder("database.connection.pool.active")
            .tag("datasource", dataSource)
            .register(meterRegistry, active);
            
        Gauge.builder("database.connection.pool.idle")
            .tag("datasource", dataSource)
            .register(meterRegistry, idle);
            
        Gauge.builder("database.connection.pool.total")
            .tag("datasource", dataSource)
            .register(meterRegistry, total);
    }
    
    /**
     * 监控慢查询
     */
    public void recordSlowQuery(String sql, long executionTime) {
        if (executionTime > 1000) { // 超过1秒的查询
            Counter.builder("database.slow.query")
                .tag("sql", sql)
                .register(meterRegistry)
                .increment();
                
            log.warn("检测到慢查询: {}, 执行时间: {}ms", sql, executionTime);
        }
    }
}
```

### 2. 健康检查

```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public Health health() {
        try {
            // 检查数据库连接
            try (Connection connection = dataSource.getConnection()) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("SELECT 1");
                }
            }
            
            return Health.up()
                .withDetail("status", "UP")
                .withDetail("message", "数据库连接正常")
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("status", "DOWN")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

## 🔧 自定义扩展

### 1. 自定义数据源

```java
@Component
public class CustomDataSource extends AbstractDataSource {
    
    private final DataSource masterDataSource;
    private final List<DataSource> slaveDataSources;
    private final LoadBalanceStrategy loadBalanceStrategy;
    
    public CustomDataSource(DataSource masterDataSource, 
                          List<DataSource> slaveDataSources,
                          LoadBalanceStrategy loadBalanceStrategy) {
        this.masterDataSource = masterDataSource;
        this.slaveDataSources = slaveDataSources;
        this.loadBalanceStrategy = loadBalanceStrategy;
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        // 根据当前上下文决定使用主数据源还是从数据源
        DataSourceType dataSourceType = DataSourceContextHolder.getDataSourceType();
        
        if (DataSourceType.WRITE.equals(dataSourceType)) {
            return masterDataSource.getConnection();
        } else {
            DataSource selectedDataSource = loadBalanceStrategy.select(slaveDataSources);
            return selectedDataSource.getConnection();
        }
    }
    
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        // 实现用户名密码认证的连接获取
        return getConnection();
    }
}
```

### 2. 自定义拦截器

```java
@Component
public class PerformanceInterceptor implements Interceptor {
    
    @Autowired
    private DatabasePerformanceMonitor performanceMonitor;
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = invocation.proceed();
            return result;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 记录执行时间
            String methodName = invocation.getMethod().getName();
            performanceMonitor.recordSqlExecutionTime(methodName, executionTime);
            
            // 记录慢查询
            performanceMonitor.recordSlowQuery(methodName, executionTime);
        }
    }
}
```

## 📝 最佳实践

### 1. 连接池配置
- 根据CPU核心数合理设置连接池大小
- 设置合适的连接生命周期和空闲超时
- 启用连接泄漏检测
- 配置连接验证查询

### 2. 查询优化
- 为常用查询字段创建合适的索引
- 使用覆盖索引减少回表查询
- 避免SELECT *，只查询需要的字段
- 合理使用分页查询避免大数据量查询

### 3. 分库分表
- 选择合适的分片键，避免数据倾斜
- 合理设置分片数量，平衡性能和复杂度
- 实现跨分片查询的聚合逻辑
- 考虑数据迁移和扩容策略

### 4. 读写分离
- 主从数据同步延迟的处理
- 读写分离的一致性保证
- 负载均衡策略的选择
- 故障转移和容错处理

### 5. 监控告警
- 设置合理的性能指标阈值
- 实现完整的健康检查
- 配置告警通知机制
- 定期分析性能数据

## 🐛 常见问题

### 1. 连接池耗尽
**问题**：数据库连接池连接数不足
**解决方案**：
- 增加连接池最大连接数
- 优化SQL查询减少连接占用时间
- 检查是否有连接泄漏
- 实现连接池监控和告警

### 2. 慢查询问题
**问题**：某些SQL查询执行时间过长
**解决方案**：
- 分析执行计划，优化索引
- 重写复杂查询语句
- 使用分页查询避免大数据量
- 考虑读写分离分担查询压力

### 3. 分库分表数据倾斜
**问题**：某些分片数据量过大
**解决方案**：
- 重新设计分片策略
- 使用复合分片键
- 实现数据重新分片
- 监控分片数据分布

## 📚 相关文档

- [Synapse Databases 基础使用](README.md)
- [配置重构文档](CONFIGURATION_REFACTORING.md)

---

*最后更新时间：2025年08月11日 12:41:56* 