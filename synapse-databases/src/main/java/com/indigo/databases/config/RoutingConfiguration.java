package com.indigo.databases.config;

import com.indigo.databases.routing.DataSourceRouter;
import com.indigo.databases.routing.RoutingStrategyManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 路由配置类
 * 初始化路由策略管理器，配置默认路由策略
 *
 * @author 史偕成
 * @date 2025/01/19
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(SynapseDataSourceProperties.class)
public class RoutingConfiguration {
    
    private final SynapseDataSourceProperties properties;
    
    public RoutingConfiguration(SynapseDataSourceProperties properties) {
        this.properties = properties;
    }
    
    @Bean
    public RoutingStrategyManager routingStrategyManager(List<DataSourceRouter> routers) {
        RoutingStrategyManager manager = new RoutingStrategyManager();
        
        // 配置默认路由策略
        configureDefaultRoutingStrategies(manager);
        
        // 注册所有路由器
        registerAllRouters(manager, routers);
        
        log.info("路由策略管理器初始化完成");
        return manager;
    }
    
    /**
     * 配置默认路由策略
     */
    private void configureDefaultRoutingStrategies(RoutingStrategyManager manager) {
        // 根据配置设置默认策略
        if (properties.getReadWrite().isEnabled()) {
            log.info("配置读写分离为默认路由策略");
            // 读写分离策略会在DynamicRoutingDataSource中注册
        }
        
        if (properties.getFailover().isEnabled()) {
            log.info("配置故障转移为备用路由策略");
            // 故障转移策略会在DynamicRoutingDataSource中注册
        }
    }
    
    /**
     * 注册所有路由器
     */
    private void registerAllRouters(RoutingStrategyManager manager, List<DataSourceRouter> routers) {
        routers.forEach(router -> {
            String name = router.getStrategyName().toLowerCase();
            manager.registerRouter(name, router);
            log.info("注册路由器: [{}] -> [{}]", name, router.getClass().getSimpleName());
        });
    }
}
