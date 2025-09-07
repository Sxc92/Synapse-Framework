package com.indigo.databases.config;

import com.indigo.databases.dynamic.DynamicRoutingDataSource;
import com.indigo.databases.routing.DataSourceRouter;
import com.indigo.databases.routing.SmartRouterSelector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.List;

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
    
    public DynamicDataSourceAutoConfiguration(SynapseDataSourceProperties properties,
                                            List<DataSourceRouter> routers,
                                            SmartRouterSelector routerSelector) {
        log.info("DynamicDataSourceAutoConfiguration 被加载");
        this.properties = properties;
        this.routers = routers;
        this.routerSelector = routerSelector;
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

        // 配置数据源
        properties.getDatasources().forEach((name, props) -> {
            try {
                // 创建数据源
                DataSource dataSource = createDataSource(props);
                
                // 使用 addDataSource 方法添加数据源
                dynamicDataSource.addDataSource(name, dataSource);
                
                log.info("Initialized datasource [{}] with type [{}], role [{}], pool [{}]",
                        name, props.getType(), props.getRole(), props.getPoolType());
                        
            } catch (Exception e) {
                log.error("Failed to initialize datasource [{}]: {}", name, e.getMessage());
                throw new RuntimeException("Failed to initialize datasource: " + name, e);
            }
        });

        log.info("Setting default data source: {}", properties.getPrimary());
        
        // 设置默认数据源
        DataSource primaryDataSource = dynamicDataSource.getDataSources().get(properties.getPrimary());
        if (primaryDataSource == null) {
            throw new IllegalStateException("Primary data source [" + properties.getPrimary() + "] not found");
        }
        
        dynamicDataSource.setDefaultTargetDataSource(primaryDataSource);

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
} 