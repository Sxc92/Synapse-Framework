package com.indigo.databases.factory;

import com.indigo.databases.config.SynapseDataSourceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 数据源工厂类
 * 负责根据配置创建数据源
 *
 * @author 史偕成
 * @date 2025/01/15
 */
@Slf4j
@Component
public class DataSourceFactory {
    
    /**
     * 根据配置创建数据源
     *
     * @param config 数据源配置
     * @return 数据源实例
     * @throws Exception 创建失败时抛出异常
     */
    public DataSource createDataSource(SynapseDataSourceProperties.DataSourceConfig config) throws Exception {
        log.info("创建数据源: {}, 类型: {}, 连接池: {}", config.getDatabase(), config.getType(), config.getPoolType());
        
        try {
            switch (config.getPoolType()) {
                case HIKARI:
                    return createHikariDataSource(config);
                case DRUID:
                    return createDruidDataSource(config);
                default:
                    throw new IllegalArgumentException("不支持的连接池类型: " + config.getPoolType());
            }
        } catch (Exception e) {
            String rootCause = extractRootCauseMessage(e);
            log.error("创建数据源失败: {}", rootCause);
            throw new RuntimeException("创建数据源失败: " + config.getDatabase() + " - " + rootCause, e);
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
            } else if (message.contains("Failed to initialize pool")) {
                return "连接池初始化失败，请检查数据库连接配置";
            } else if (message.contains("Could not create connection to database server")) {
                return "无法创建数据库连接，请检查数据库服务状态";
            }
        }
        
        return message != null ? message : "未知错误";
    }
}
