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
@ConditionalOnClass(DataSource.class)
@ConditionalOnProperty(prefix = "synapse.datasource", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SynapseDataSourceProperties.class)
public class DynamicDataSourceAutoConfiguration {
    
    private final SynapseDataSourceProperties properties;
    private final List<DataSourceRouter> routers;
    private final SmartRouterSelector routerSelector;
    
    public DynamicDataSourceAutoConfiguration(SynapseDataSourceProperties properties,
                                            List<DataSourceRouter> routers,
                                            SmartRouterSelector routerSelector) {
        this.properties = properties;
        this.routers = routers;
        this.routerSelector = routerSelector;
        log.info("SynapseDataSourceProperties loaded: {}", properties);
        log.info("Found {} data source routers: {}", routers.size(), 
                routers.stream().map(router -> router.getClass().getSimpleName()).toList());
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
        // 这里需要实现数据源创建逻辑
        // 暂时返回null，实际实现时需要根据配置创建对应的数据源
        log.warn("createDataSource method not implemented yet for datasource: {}", props);
        throw new UnsupportedOperationException("createDataSource method not implemented yet");
    }
} 