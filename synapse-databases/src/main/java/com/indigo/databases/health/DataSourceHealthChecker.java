package com.indigo.databases.health;

import com.indigo.databases.config.SynapseDataSourceProperties;
import com.indigo.databases.dynamic.DynamicRoutingDataSource;
import com.indigo.databases.enums.DatabaseType;
import com.indigo.databases.factory.DataSourceFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 数据源健康检查器
 * 支持多种数据库类型的心跳检测
 *
 * @author 史偕成
 * @date 2025/03/21
 */
@Slf4j
@Component
public class DataSourceHealthChecker {
    
    private final DynamicRoutingDataSource routingDataSource;
    @Autowired
    private SynapseDataSourceProperties dataSourceProperties;
    @Autowired
    private DataSourceHealthEventPublisher eventPublisher;
    @Autowired
    private DataSourceFactory dataSourceFactory;
    
    private final Map<String, Boolean> dataSourceHealth = new ConcurrentHashMap<>();
    private final Map<String, DatabaseType> dataSourceTypes = new ConcurrentHashMap<>();
    private final ScheduledExecutorService healthCheckExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "DataSourceHealthChecker");
        t.setDaemon(true);  // 设置为守护线程
        return t;
    });
    
    // 存储失败的数据源配置，用于动态恢复
    private final Map<String, SynapseDataSourceProperties.DataSourceConfig> failedDataSourceConfigs = new ConcurrentHashMap<>();
    
    /**
     * 各数据库类型的心跳SQL
     */
    private static final Map<DatabaseType, String> HEALTH_CHECK_SQL = Map.of(
        DatabaseType.MYSQL, "SELECT 1",
        DatabaseType.POSTGRESQL, "SELECT 1",
        DatabaseType.ORACLE, "SELECT 1 FROM DUAL",
        DatabaseType.SQLSERVER, "SELECT 1",
        DatabaseType.H2, "SELECT 1"
    );
    
    /**
     * -- GETTER --
     *  检查是否已初始化
     *
     * @return 是否已初始化
     */
    @Getter
    private volatile boolean initialized = false;
    
    public DataSourceHealthChecker(@Qualifier("dynamicDataSource") DynamicRoutingDataSource routingDataSource) {
        this.routingDataSource = routingDataSource;
    }
    
    /**
     * 设置失败的数据源配置，用于动态恢复
     */
    public void setFailedDataSourceConfigs(Map<String, SynapseDataSourceProperties.DataSourceConfig> failedConfigs) {
        this.failedDataSourceConfigs.clear();
        this.failedDataSourceConfigs.putAll(failedConfigs);
        log.info("设置失败数据源配置，共 {} 个: {}", failedConfigs.size(), failedConfigs.keySet());
    }
    
    /**
     * 初始化时启动健康检查守护线程
     */
    @PostConstruct
    public void init() {
        if (dataSourceProperties.getHealthCheck().isEnabled()) {
            // 启动时执行一次健康检查
            if (dataSourceProperties.getHealthCheck().isCheckOnStartup()) {
                performHealthCheckOnce();
            }
            
            // 启动定时健康检查守护线程
            startHealthCheckDaemon();
            log.debug("数据源健康检查守护线程已启动，检查间隔: {}ms",
                    dataSourceProperties.getHealthCheck().getInterval());
        }
        initialized = true;
    }
    
    /**
     * 启动健康检查守护线程
     */
    private void startHealthCheckDaemon() {
        long interval = dataSourceProperties.getHealthCheck().getInterval();
        healthCheckExecutor.scheduleWithFixedDelay(
            this::performHealthCheckOnce,
            interval,  // 初始延迟
            interval,  // 执行间隔
            java.util.concurrent.TimeUnit.MILLISECONDS
        );
    }
    
    /**
     * 执行一次完整的健康检查
     */
    private void performHealthCheckOnce() {
        try {
            Map<String, DataSource> dataSources = routingDataSource.getDataSources();
            log.debug("开始执行数据源健康检查，共{}个数据源", dataSources.size());
            
            // 在守护线程中顺序检查所有数据源
            dataSources.forEach((name, dataSource) -> {
                try {
                    performSingleHealthCheck(name, dataSource);
                } catch (Exception e) {
                    log.error("健康检查数据源[{}]时发生异常: {}", name, e.getMessage());
                }
            });
            
            // 尝试恢复失败的数据源
            if (!failedDataSourceConfigs.isEmpty()) {
                log.info("尝试恢复失败的数据源，共{}个: {}", failedDataSourceConfigs.size(), failedDataSourceConfigs.keySet());
                attemptRecoverFailedDataSources();
            }
            
        } catch (Exception e) {
            log.error("执行健康检查时发生异常: {}", e.getMessage());
        }
    }
    
    /**
     * 执行单个数据源的健康检查（单线程同步执行）
     *
     * @param name       数据源名称
     * @param dataSource 数据源
     */
    private void performSingleHealthCheck(String name, DataSource dataSource) {
        // 检查是否为占位符数据源
        if (isPlaceholderDataSource(dataSource)) {
            log.debug("跳过占位符数据源 [{}] 的健康检查", name);
            dataSourceHealth.put(name, false);
            return;
        }
        
        executeHealthCheckWithRetry(name, dataSource);
    }
    
    /**
     * 检查是否为占位符数据源
     */
    private boolean isPlaceholderDataSource(DataSource dataSource) {
        return dataSource.getClass().getSimpleName().equals("PlaceholderDataSource");
    }
    
    /**
     * 检查数据源是否存在于动态路由数据源中
     */
    private boolean isDataSourceExists(String name) {
        return routingDataSource.getDataSources().containsKey(name);
    }
    
    /**
     * 尝试恢复失败的数据源
     */
    private void attemptRecoverFailedDataSources() {
        // 使用迭代器安全地遍历和修改集合
        var iterator = failedDataSourceConfigs.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            String name = entry.getKey();
            SynapseDataSourceProperties.DataSourceConfig config = entry.getValue();
            
            try {
                log.info("尝试恢复数据源 [{}]", name);
                
                // 尝试创建数据源
                DataSource dataSource = createDataSourceFromConfig(config);
                
                // 测试连接
                testDataSourceConnection(dataSource);
                
                // 恢复成功，添加到动态路由
                routingDataSource.addDataSource(name, dataSource);
                dataSourceHealth.put(name, true);
                
                // 从失败列表中移除
                iterator.remove();
                
                // 发布恢复事件
                eventPublisher.publishDataSourceRecovered(name);
                
                log.info("数据源 [{}] 恢复成功，已添加到动态路由", name);
                
            } catch (Exception e) {
                log.warn("数据源 [{}] 恢复失败: {}", name, e.getMessage());
                // 继续尝试其他数据源
            }
        }
    }
    
    /**
     * 从配置创建数据源
     */
    private DataSource createDataSourceFromConfig(SynapseDataSourceProperties.DataSourceConfig config) throws Exception {
        return dataSourceFactory.createDataSource(config);
    }
    
    /**
     * 测试数据源连接
     */
    private void testDataSourceConnection(DataSource dataSource) throws Exception {
        try (var connection = dataSource.getConnection()) {
            // 简单的连接测试
            connection.isValid(5); // 5秒超时
        }
    }
    
    /**
     * 执行健康检查并处理重试（同步执行）
     *
     * @param name        数据源名称
     * @param dataSource  数据源
     * @param attempt     当前尝试次数
     * @param maxRetries  最大重试次数
     */
    private void executeHealthCheckWithRetry(String name, DataSource dataSource) {
        try {
            executeHealthCheckAttempt(name, dataSource);
        } catch (Exception e) {
            // 立即标记为不健康，触发故障转移
            dataSourceHealth.put(name, false);
            log.warn("数据源 [{}] 健康检查失败，已标记为不健康: {}", name, e.getMessage());
            
            // 发布故障事件
            if (eventPublisher != null) {
                eventPublisher.publishDataSourceFailure(name, e.getMessage());
                log.debug("已发布数据源 [{}] 故障事件", name);
            } else {
                log.error("事件发布器未初始化，无法发布数据源 [{}] 故障事件", name);
            }
        }
    }
    
    /**
     * 执行单次健康检查尝试
     *
     * @param name        数据源名称
     * @param dataSource  数据源
     */
    private void executeHealthCheckAttempt(String name, DataSource dataSource) throws Exception {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        var healthCheckConfig = dataSourceProperties.getHealthCheck();
        
        // 设置查询超时时间
        jdbcTemplate.setQueryTimeout((int) (healthCheckConfig.getTimeout() / 1000));
        
        DatabaseType dbType = detectDatabaseType(dataSource, name);
        String healthCheckSql = HEALTH_CHECK_SQL.get(dbType);
        
        jdbcTemplate.queryForObject(healthCheckSql, Integer.class);
        
        // ✅ 健康检查成功 - 更新状态并发布事件
        boolean wasUnhealthy = Boolean.FALSE.equals(dataSourceHealth.get(name));
        dataSourceHealth.put(name, true);
        
        if (wasUnhealthy) {
            // 数据源从故障状态恢复
            eventPublisher.publishDataSourceRecovered(name);
            log.info("DataSource [{}] ({}) recovered from failure", name, dbType);
        } else {
            // 数据源保持健康状态
            eventPublisher.publishHealthStatus(name, true, "Health check passed");
            log.debug("DataSource [{}] ({}) is healthy", name, dbType);
        }
    }
    
    /**
     * 计算退避延迟时间
     *
     * @param attempt 尝试次数
     * @return 延迟时间（毫秒）
     */
    private long calculateBackoffDelay(int attempt) {
        // 指数退避：1秒、2秒、4秒...
        return Math.min(1000L * (1L << attempt), 10000L); // 最大10秒
    }
    
    /**
     * 检测数据源类型
     *
     * @param dataSource 数据源
     * @param name       数据源名称
     * @return URI 数据库类型
     */
    private DatabaseType detectDatabaseType(DataSource dataSource, String name) {
        // 优先从缓存中获取
        DatabaseType cachedType = dataSourceTypes.get(name);
        if (cachedType != null) {
            return cachedType;
        }
        
        // 尝试从配置中获取指定的数据库类型
        DatabaseType configuredType = getConfiguredDatabaseType(name);
        if (configuredType != null) {
            dataSourceTypes.put(name, configuredType);
            log.info("Using configured database type for [{}]: {}", name, configuredType);
            return configuredType;
        }
        
        // 自动检测数据库类型
        DatabaseType detectedType = detectDatabaseTypeFromConnection(dataSource);
        dataSourceTypes.put(name, detectedType);
        return detectedType;
    }
    
    /**
     * 从配置中获取指定数据源的数据库类型
     *
     * @param dataSourceName 数据源名称
     * @return 配置的数据库类型，如果没有配置则返回 null
     */
    private DatabaseType getConfiguredDatabaseType(String dataSourceName) {
        try {
            // 从动态路由数据源获取配置信息
            var dataSourceConfigs = dataSourceProperties.getDatasources();
            if (dataSourceConfigs != null && dataSourceConfigs.containsKey(dataSourceName)) {
                SynapseDataSourceProperties.DataSourceConfig config = dataSourceConfigs.get(dataSourceName);
                return config.getType();
            }
        } catch (Exception e) {
            log.debug("Unable to get configured database type for [{}]: {}", dataSourceName, e.getMessage());
        }
        return null;
    }
    
    /**
     * 从连接中检测数据库类型
     * 使用更严格的检测逻辑，避免误判
     *
     * @param dataSource 数据源
     * @return 数据库类型
     */
    private DatabaseType detectDatabaseTypeFromConnection(DataSource dataSource) {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            
            // 先检测 Oracle：使用 Oracle 特有的函数
            if (isOracleDatabase(jdbcTemplate)) {
                return DatabaseType.ORACLE;
            }
            
            // 检测 MySQL：使用 MySQL 特有的函数
            if (isMysqlDatabase(jdbcTemplate)) {
                return DatabaseType.MYSQL;
            }
            
            // 检测 PostgreSQL：使用 PostgreSQL 特有的函数
            if (isPostgresqlDatabase(jdbcTemplate)) {
                return DatabaseType.POSTGRESQL;
            }
            
            // 检测 SQL Server：使用 SQL Server 特有的函数
            if (isSqlServerDatabase(jdbcTemplate)) {
                return DatabaseType.SQLSERVER;
            }
            
            // 检测 H2：使用 H2 特有的函数
            if (isH2Database(jdbcTemplate)) {
                return DatabaseType.H2;
            }
            
            // 默认返回 MySQL
            log.warn("Unable to detect database type, defaulting to MYSQL");
            return DatabaseType.MYSQL;
        } catch (Exception e) {
            log.warn("Error detecting database type: {}, defaulting to MYSQL", e.getMessage());
            return DatabaseType.MYSQL;
        }
    }
    
    /**
     * 检测是否为 Oracle 数据库
     */
    private boolean isOracleDatabase(JdbcTemplate jdbcTemplate) {
        try {
            // Oracle 特有的 SYSDATE 函数
            jdbcTemplate.queryForObject("SELECT SYSDATE FROM DUAL", java.sql.Timestamp.class);
            log.debug("Detected database type: ORACLE");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检测是否为 MySQL 数据库
     */
    private boolean isMysqlDatabase(JdbcTemplate jdbcTemplate) {
        try {
            // MySQL 特有的 DATABASE() 函数
            String result = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            log.debug("Detected database type: MYSQL (database: {})", result);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检测是否为 PostgreSQL 数据库
     */
    private boolean isPostgresqlDatabase(JdbcTemplate jdbcTemplate) {
        try {
            // PostgreSQL 特有的 current_database() 函数
            String result = jdbcTemplate.queryForObject("SELECT current_database()", String.class);
            log.debug("Detected database type: POSTGRESQL (database: {})", result);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检测是否为 SQL Server 数据库
     */
    private boolean isSqlServerDatabase(JdbcTemplate jdbcTemplate) {
        try {
            // SQL Server 特有的 DB_NAME() 函数
            String result = jdbcTemplate.queryForObject("SELECT DB_NAME()", String.class);
            log.debug("Detected database type: SQLSERVER (database: {})", result);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检测是否为 H2 数据库
     */
    private boolean isH2Database(JdbcTemplate jdbcTemplate) {
        try {
            // H2 特有的 H2VERSION() 函数
            String result = jdbcTemplate.queryForObject("SELECT H2VERSION()", String.class);
            log.debug("Detected database type: H2 (version: {})", result);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 手动触发健康检查并等待完成
     */
    public void checkHealthAndWait() {
        performHealthCheckOnce();
        // 等待健康检查完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 应用关闭时清理资源
     */
    @PreDestroy
    public void destroy() {
        log.info("正在关闭数据源健康检查守护线程...");
        healthCheckExecutor.shutdown();
        try {
            if (!healthCheckExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                log.warn("健康检查守护线程未能在5秒内正常关闭，强制关闭");
                healthCheckExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            healthCheckExecutor.shutdownNow();
        }
        log.info("数据源健康检查守护线程已关闭");
    }
    
    /**
     * 检查数据源是否健康
     *
     * @param dataSourceName 数据源名称
     * @return 是否健康
     */
    public boolean isHealthy(String dataSourceName) {
        if (dataSourceName == null || dataSourceName.trim().isEmpty()) {
            log.warn("DataSource name is null or empty, returning false");
            return false;
        }
        
        // 如果数据源不存在，返回 false
        if (!isDataSourceExists(dataSourceName)) {
            log.debug("数据源 [{}] 不存在，返回不健康状态", dataSourceName);
            return false;
        }
        
        if (!initialized) {
            checkHealthAndWait();
        }
        return dataSourceHealth.getOrDefault(dataSourceName, false);
    }
    
    /**
     * 获取所有数据源的健康状态
     *
     * @return 数据源健康状态映射
     */
    public Map<String, Boolean> getHealthStatus() {
        if (!initialized) {
            checkHealthAndWait();
        }
        return new ConcurrentHashMap<>(dataSourceHealth);
    }
    
    /**
     * 获取数据源类型信息
     *
     * @param dataSourceName 数据源名称
     * @return 数据库类型
     */
    public DatabaseType getDatabaseType(String dataSourceName) {
        return dataSourceTypes.getOrDefault(dataSourceName, DatabaseType.MYSQL);
    }
    
    /**
     * 获取所有数据源的类型信息
     *
     * @return 数据源类型映射
     */
    public Map<String, DatabaseType> getDataSourceTypes() {
        return new ConcurrentHashMap<>(dataSourceTypes);
    }

} 