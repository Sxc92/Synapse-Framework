package com.indigo.databases.health;

import com.indigo.databases.config.SynapseDataSourceProperties;
import com.indigo.databases.dynamic.DynamicRoutingDataSource;
import com.indigo.databases.enums.DatabaseType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;

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
    
    private final Map<String, Boolean> dataSourceHealth = new ConcurrentHashMap<>();
    private final Map<String, DatabaseType> dataSourceTypes = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
    
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
     * 初始化时执行一次健康检查
     */
    @PostConstruct
    public void init() {
        if (dataSourceProperties.getHealthCheck().isCheckOnStartup()) {
            checkHealth();
        }
        initialized = true;
    }
    
    /**
     * 根据配置的间隔时间检查数据源健康状态
     */
    @Scheduled(fixedRateString = "${synapse.datasource.health-check.interval:30000}")
    public void checkHealth() {
        // 检查是否启用健康检查
        if (!dataSourceProperties.getHealthCheck().isEnabled()) {
            return;
        }
        Map<String, DataSource> dataSources = routingDataSource.getDataSources();
        // 异步执行健康检查，避免阻塞主线程
        dataSources.forEach(this::scheduleHealthCheck);
    }
    
    /**
     * 调度单个数据源的健康检查（异步）
     *
     * @param name       数据源名称
     * @param dataSource 数据源
     */
    private void scheduleHealthCheck(String name, DataSource dataSource) {
        CompletableFuture.runAsync(() -> performHealthCheck(name, dataSource), executorService)
                .exceptionally(throwable -> {
                    log.error("Failed to execute health check for dataSource [{}]: {}", name, throwable.getMessage());
                    return null;
                });
    }
    
    /**
     * 执行单个数据源的健康检查
     *
     * @param name       数据源名称
     * @param dataSource 数据源
     */
    private void performHealthCheck(String name, DataSource dataSource) {
        var healthCheckConfig = dataSourceProperties.getHealthCheck();
        scheduleRetryWithBackoff(name, dataSource, 0, healthCheckConfig.getMaxRetries());
    }
    
    /**
     * 使用退避策略调度重试
     *
     * @param name        数据源名称
     * @param dataSource  数据源
     * @param attempt     当前尝试次数
     * @param maxRetries  最大重试次数
     */
    private void scheduleRetryWithBackoff(String name, DataSource dataSource, int attempt, int maxRetries) {
        executorService.schedule(() -> executeHealthCheckAttempt(name, dataSource, attempt, maxRetries), 
                calculateBackoffDelay(attempt), TimeUnit.MILLISECONDS);
    }
    
    /**
     * 执行单次健康检查尝试
     *
     * @param name        数据源名称
     * @param dataSource  数据源
     * @param attempt     当前尝试次数
     * @param maxRetries  最大重试次数
     */
    private void executeHealthCheckAttempt(String name, DataSource dataSource, int attempt, int maxRetries) {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            var healthCheckConfig = dataSourceProperties.getHealthCheck();
            
            // 设置查询超时时间
            jdbcTemplate.setQueryTimeout((int) (healthCheckConfig.getTimeout() / 1000));
            
            DatabaseType dbType = detectDatabaseType(dataSource, name);
            String healthCheckSql = HEALTH_CHECK_SQL.get(dbType);
            
            jdbcTemplate.queryForObject(healthCheckSql, Integer.class);
            dataSourceHealth.put(name, true);
            log.debug("DataSource [{}] ({}) is healthy", name, dbType);
            
        } catch (Exception e) {
            if (attempt >= maxRetries) {
                dataSourceHealth.put(name, false);
                log.error("DataSource [{}] is unhealthy after {} retries: {}", 
                         name, maxRetries + 1, e.getMessage());
            } else {
                log.warn("DataSource [{}] health check failed (attempt {}/{}): {}", 
                        name, attempt + 1, maxRetries + 1, e.getMessage());
                // 使用退避策略调度下次重试
                scheduleRetryWithBackoff(name, dataSource, attempt + 1, maxRetries);
            }
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
        checkHealth();
        // 等待健康检查完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
    
    /**
     * 销毁时清理资源
     */
    @PreDestroy
    public void destroy() {
        if (!executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                    if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                        log.warn("Health checker executor did not terminate gracefully");
                    }
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

} 