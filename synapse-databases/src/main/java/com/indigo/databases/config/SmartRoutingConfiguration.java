package com.indigo.databases.config;

import com.indigo.databases.routing.DataSourceRouter;
import com.indigo.databases.routing.SmartRouterSelector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 智能路由配置类
 * 初始化智能路由器选择器，配置路由策略
 *
 * @author 史偕成
 * @date 2025/01/19
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(SynapseDataSourceProperties.class)
public class SmartRoutingConfiguration {
    
    private final SynapseDataSourceProperties properties;
    
    public SmartRoutingConfiguration(SynapseDataSourceProperties properties) {
        this.properties = properties;
    }
    
    @Bean
    public SmartRouterSelector smartRouterSelector(List<DataSourceRouter> routers) {
        SmartRouterSelector selector = new SmartRouterSelector();
        
        // 配置智能路由策略
        configureSmartRoutingStrategies(selector);
        
        // 注册所有路由器
        registerAllRouters(selector, routers);
        
        log.debug("智能路由器选择器初始化完成");
        return selector;
    }
    
    /**
     * 配置智能路由策略
     */
    private void configureSmartRoutingStrategies(SmartRouterSelector selector) {
        // 根据配置设置智能策略
        if (properties.getReadWrite().isEnabled()) {
            log.debug("配置读写分离为智能主要策略");
        }
        
        if (properties.getFailover().isEnabled()) {
            log.debug("配置故障转移为智能备用策略");
        }
        
        // 负载均衡总是启用的，因为它是一个功能特性
        log.debug("配置负载均衡为智能策略");
    }
    
    /**
     * 注册所有路由器
     */
    private void registerAllRouters(SmartRouterSelector selector, List<DataSourceRouter> routers) {
        routers.forEach(router -> {
            String name = getRouterName(router);
            selector.registerRouter(name, router);
            log.debug("智能选择器注册路由器: [{}] -> [{}]", name, router.getClass().getSimpleName());
        });
    }
    
    /**
     * 获取路由器名称
     */
    private String getRouterName(DataSourceRouter router) {
        String className = router.getClass().getSimpleName();
        if (className.contains("ReadWrite")) {
            return "read-write";
        } else if (className.contains("Failover")) {
            return "failover";
        } else if (className.contains("LoadBalance")) {
            return "load-balance";
        } else {
            return router.getStrategyName().toLowerCase();
        }
    }
}
