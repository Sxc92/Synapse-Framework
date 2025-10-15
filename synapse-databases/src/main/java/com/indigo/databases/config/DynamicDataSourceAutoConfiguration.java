package com.indigo.databases.config;

import com.indigo.databases.dynamic.DynamicRoutingDataSource;
import com.indigo.databases.routing.DataSourceRouter;
import com.indigo.databases.routing.SmartRouterSelector;
import com.indigo.databases.health.DataSourceHealthEventPublisher;
import com.indigo.databases.health.DataSourceHealthChecker;
import com.indigo.databases.factory.DataSourceFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 动态数据源自动配置类
 * 自动配置动态路由数据源和相关组件
 *
 * @author 史偕成
 * @date 2025/01/19
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(SynapseDataSourceProperties.class)
public class DynamicDataSourceAutoConfiguration {
    
    private final SynapseDataSourceProperties properties;
    private final List<DataSourceRouter> routers;
    private final SmartRouterSelector routerSelector;
    private final DataSourceFactory dataSourceFactory;
    private final ApplicationContext applicationContext;
    
    public DynamicDataSourceAutoConfiguration(SynapseDataSourceProperties properties,
                                            List<DataSourceRouter> routers,
                                            SmartRouterSelector routerSelector,
                                            DataSourceFactory dataSourceFactory,
                                            ApplicationContext applicationContext) {
        log.info("DynamicDataSourceAutoConfiguration 被加载");
        this.properties = properties;
        this.routers = routers;
        this.routerSelector = routerSelector;
        this.dataSourceFactory = dataSourceFactory;
        this.applicationContext = applicationContext;
    }
    
    @Bean
    @Primary
    public DynamicRoutingDataSource dynamicDataSource() {
        // 创建配置对象
        DynamicRoutingDataSource.DataSourceConfig config = 
                new DynamicRoutingDataSource.DataSourceConfig(properties.getPrimary());
        
        // 创建动态路由数据源
        DynamicRoutingDataSource dynamicDataSource = new DynamicRoutingDataSource(
                routers, 
                routerSelector, 
                config
        );

        // 配置数据源 - 支持优雅降级启动
        int successCount = 0;
        int totalCount = properties.getDatasources().size();
        DataSource firstSuccessfulDataSource = null;
        String firstSuccessfulDataSourceName = null;
        
        // 存储失败的数据源配置，用于后续恢复
        Map<String, SynapseDataSourceProperties.DataSourceConfig> failedDataSourceConfigs = new java.util.concurrent.ConcurrentHashMap<>();
        
        for (Map.Entry<String, SynapseDataSourceProperties.DataSourceConfig> entry : properties.getDatasources().entrySet()) {
            String name = entry.getKey();
            SynapseDataSourceProperties.DataSourceConfig props = entry.getValue();
            
            try {
                // 创建数据源
                DataSource dataSource = dataSourceFactory.createDataSource(props);
                
                // 使用 addDataSource 方法添加数据源
                dynamicDataSource.addDataSource(name, dataSource);
                
                log.info("Initialized datasource [{}] with type [{}], role [{}], pool [{}]",
                        name, props.getType(), props.getRole(), props.getPoolType());
                successCount++;
                
                // 记录第一个成功的数据源，用于 Seata 初始化
                if (firstSuccessfulDataSource == null) {
                    firstSuccessfulDataSource = dataSource;
                    firstSuccessfulDataSourceName = name;
                }
                        
            } catch (Exception e) {
                // 提取异常的根本原因信息
                String rootCause = extractRootCauseMessage(e);
                log.error("数据源 [{}] 初始化失败: {}", name, rootCause);
                
                // 如果启用了故障转移，允许部分数据源初始化失败
                if (properties.getFailover().isEnabled()) {
                    log.warn("数据源 [{}] 初始化失败，但由于启用了故障转移，应用将继续启动。失败原因: {}", name, rootCause);
                    // 将失败的数据源配置保存到待恢复列表
                    failedDataSourceConfigs.put(name, props);
                } else {
                    // 如果未启用故障转移，则抛出异常
                    throw new RuntimeException("Failed to initialize datasource: " + name + " - " + rootCause, e);
                }
            }
        }
        
        log.info("数据源初始化完成: 成功 {}/{} 个", successCount, totalCount);
        
        // 如果启用了故障转移，检查是否至少有一个数据源成功
        if (properties.getFailover().isEnabled()) {
            if (successCount == 0) {
                throw new RuntimeException("启用故障转移时，至少需要一个数据源成功初始化，但所有数据源都初始化失败");
            }
            
            // 将失败的数据源配置传递给健康检查器，用于后续恢复
            if (!failedDataSourceConfigs.isEmpty()) {
                log.info("启用故障转移模式，{} 个数据源将在后台尝试恢复: {}", 
                        failedDataSourceConfigs.size(), failedDataSourceConfigs.keySet());
                // 将失败的数据源配置传递给健康检查器
                setFailedDataSourceConfigsToHealthChecker(failedDataSourceConfigs);
            }
        }

        log.info("Setting default data source: {}", properties.getPrimary());
        
        // 设置默认数据源 - 支持故障转移
        DataSource primaryDataSource = dynamicDataSource.getDataSources().get(properties.getPrimary());
        if (primaryDataSource == null) {
            if (properties.getFailover().isEnabled()) {
                log.warn("主数据源 [{}] 不可用，但启用了故障转移，将使用第一个可用的数据源作为默认数据源", properties.getPrimary());
                
                // 使用第一个成功的数据源作为默认数据源
                if (firstSuccessfulDataSource != null) {
                    dynamicDataSource.setDefaultTargetDataSource(firstSuccessfulDataSource);
                    log.info("使用数据源 [{}] 作为默认数据源", firstSuccessfulDataSourceName);
                } else {
                    throw new IllegalStateException("启用故障转移时，没有可用的数据源作为默认数据源");
                }
            } else {
                throw new IllegalStateException("Primary data source [" + properties.getPrimary() + "] not found");
            }
        } else {
            dynamicDataSource.setDefaultTargetDataSource(primaryDataSource);
        }

        return dynamicDataSource;
    }
    
    /**
     * 创建数据源
     */
    private DataSource createDataSource(SynapseDataSourceProperties.DataSourceConfig props) {
        log.info("创建数据源: {}, 类型: {}, 连接池: {}", props.getDatabase(), props.getType(), props.getPoolType());
        
        try {
            switch (props.getPoolType()) {
                case HIKARI:
                    return createHikariDataSource(props);
                case DRUID:
                    return createDruidDataSource(props);
                default:
                    throw new IllegalArgumentException("不支持的连接池类型: " + props.getPoolType());
            }
        } catch (Exception e) {
            log.error("创建数据源失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建数据源失败: " + props.getDatabase(), e);
        }
    }
    
    /**
     * 创建HikariCP数据源
     */
    private DataSource createHikariDataSource(SynapseDataSourceProperties.DataSourceConfig props) {
        com.zaxxer.hikari.HikariConfig config = new com.zaxxer.hikari.HikariConfig();
        
        // 基本配置
        config.setJdbcUrl(props.getUrl());
        config.setDriverClassName(props.getDriverClassName());
        config.setUsername(props.getUsername());
        config.setPassword(props.getPassword());
        
        // HikariCP配置
        SynapseDataSourceProperties.HikariConfig hikariConfig = props.getHikari();
        config.setMinimumIdle(hikariConfig.getMinimumIdle());
        config.setMaximumPoolSize(hikariConfig.getMaximumPoolSize());
        config.setIdleTimeout(hikariConfig.getIdleTimeout());
        config.setMaxLifetime(hikariConfig.getMaxLifetime());
        config.setConnectionTimeout(hikariConfig.getConnectionTimeout());
        config.setConnectionTestQuery(hikariConfig.getConnectionTestQuery());
        config.setValidationTimeout(hikariConfig.getValidationTimeout());
        config.setLeakDetectionThreshold(hikariConfig.getLeakDetectionThreshold());
        config.setRegisterMbeans(hikariConfig.isRegisterMbeans());
        
        if (hikariConfig.getConnectionInitSql() != null && !hikariConfig.getConnectionInitSql().isEmpty()) {
            config.setConnectionInitSql(hikariConfig.getConnectionInitSql());
        }
        
        // 设置数据源名称
        config.setPoolName(props.getDatabase() + "-hikari-pool");
        
        return new com.zaxxer.hikari.HikariDataSource(config);
    }
    
    /**
     * 创建Druid数据源
     */
    private DataSource createDruidDataSource(SynapseDataSourceProperties.DataSourceConfig props) {
        com.alibaba.druid.pool.DruidDataSource dataSource = new com.alibaba.druid.pool.DruidDataSource();
        
        // 基本配置
        dataSource.setUrl(props.getUrl());
        dataSource.setDriverClassName(props.getDriverClassName());
        dataSource.setUsername(props.getUsername());
        dataSource.setPassword(props.getPassword());
        
        // Druid配置
        SynapseDataSourceProperties.DruidConfig druidConfig = props.getDruid();
        dataSource.setInitialSize(druidConfig.getInitialSize());
        dataSource.setMinIdle(druidConfig.getMinIdle());
        dataSource.setMaxActive(druidConfig.getMaxActive());
        dataSource.setMaxWait(druidConfig.getMaxWait());
        dataSource.setTimeBetweenEvictionRunsMillis(druidConfig.getTimeBetweenEvictionRunsMillis());
        dataSource.setMinEvictableIdleTimeMillis(druidConfig.getMinEvictableIdleTimeMillis());
        dataSource.setMaxEvictableIdleTimeMillis(druidConfig.getMaxEvictableIdleTimeMillis());
        dataSource.setValidationQuery(druidConfig.getValidationQuery());
        dataSource.setTestWhileIdle(druidConfig.getTestWhileIdle());
        dataSource.setTestOnBorrow(druidConfig.getTestOnBorrow());
        dataSource.setTestOnReturn(druidConfig.getTestOnReturn());
        dataSource.setPoolPreparedStatements(druidConfig.getPoolPreparedStatements());
        dataSource.setMaxPoolPreparedStatementPerConnectionSize(druidConfig.getMaxPoolPreparedStatementPerConnectionSize());
        
        try {
            dataSource.setFilters(druidConfig.getFilters());
        } catch (Exception e) {
            log.warn("设置Druid过滤器失败: {}", e.getMessage());
        }
        
        return dataSource;
    }
    
    /**
     * 创建占位符数据源（用于标记不可用的数据源）
     */
    private DataSource createPlaceholderDataSource(String name) {
        return new PlaceholderDataSource(name);
    }
    
    
    /**
     * 设置失败的数据源配置给健康检查器
     */
    private void setFailedDataSourceConfigsToHealthChecker(Map<String, SynapseDataSourceProperties.DataSourceConfig> failedConfigs) {
        // 使用异步方式延迟设置，确保健康检查器已经完全初始化
        new Thread(() -> {
            try {
                // 等待健康检查器初始化完成
                Thread.sleep(2000);
                
                // 通过 ApplicationContext 获取健康检查器 Bean
                DataSourceHealthChecker healthChecker = applicationContext.getBean(DataSourceHealthChecker.class);
                if (healthChecker != null) {
                    healthChecker.setFailedDataSourceConfigs(failedConfigs);
                    log.info("成功将失败的数据源配置传递给健康检查器: {}", failedConfigs.keySet());
                } else {
                    log.warn("无法获取 DataSourceHealthChecker Bean，失败的数据源配置将无法传递给健康检查器");
                }
            } catch (Exception e) {
                log.error("设置失败数据源配置给健康检查器时发生异常: {}", e.getMessage());
                // 不抛出异常，避免影响应用启动
            }
        }, "DataSourceConfigSetter").start();
    }
    
    /**
     * 提取异常的根本原因信息，避免冗长的堆栈信息
     */
    private String extractRootCauseMessage(Exception e) {
        Throwable cause = e;
        String message = e.getMessage();
        
        // 查找根本原因
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
            if (cause.getMessage() != null && !cause.getMessage().isEmpty()) {
                message = cause.getMessage();
            }
        }
        
        // 简化常见的数据库连接错误信息
        if (message != null) {
            if (message.contains("Connection refused")) {
                return "数据库连接被拒绝，请检查数据库服务是否启动";
            } else if (message.contains("Communications link failure")) {
                return "数据库通信链路失败，请检查网络连接";
            } else if (message.contains("Access denied")) {
                return "数据库访问被拒绝，请检查用户名和密码";
            } else if (message.contains("Unknown database")) {
                return "数据库不存在，请检查数据库名称";
            } else if (message.contains("timeout")) {
                return "数据库连接超时，请检查网络和数据库配置";
            }
        }
        
        return message != null ? message : "未知错误";
    }
    
    /**
     * 占位符数据源 - 用于标记不可用的数据源
     */
    private static class PlaceholderDataSource implements DataSource {
        private final String name;
        
        public PlaceholderDataSource(String name) {
            this.name = name;
        }
        
        @Override
        public Connection getConnection() throws java.sql.SQLException {
            throw new SQLException("数据源 [" + name + "] 不可用，请检查数据库连接");
        }
        
        @Override
        public Connection getConnection(String username, String password) throws java.sql.SQLException {
            throw new SQLException("数据源 [" + name + "] 不可用，请检查数据库连接");
        }
        
        @Override
        public PrintWriter getLogWriter() {
            return null;
        }
        
        @Override
        public void setLogWriter(java.io.PrintWriter out) {
            // 空实现
        }
        
        @Override
        public void setLoginTimeout(int seconds) {
            // 空实现
        }
        
        @Override
        public int getLoginTimeout() {
            return 0;
        }
        
        @Override
        public Logger getParentLogger() {
            return null;
        }
        
        @Override
        public <T> T unwrap(Class<T> iface) {
            return null;
        }
        
        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }
        
        @Override
        public String toString() {
            return "PlaceholderDataSource{name='" + name + "'}";
        }
    }
} 