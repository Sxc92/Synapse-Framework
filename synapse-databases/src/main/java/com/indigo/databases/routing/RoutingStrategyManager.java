package com.indigo.databases.routing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 路由策略管理器
 * 统一管理不同的路由策略，提供策略选择和切换功能
 *
 * @author 史偕成
 * @date 2025/01/19
 */
@Slf4j
@Component
public class RoutingStrategyManager {
    
    private final Map<String, DataSourceRouter> routerRegistry = new ConcurrentHashMap<>();
    private final Map<DataSourceRouter.SqlType, String> defaultStrategies = new ConcurrentHashMap<>();
    
    /**
     * 注册路由器
     */
    public void registerRouter(String name, DataSourceRouter router) {
        routerRegistry.put(name, router);
        log.info("注册路由器: [{}] -> [{}]", name, router.getStrategyName());
    }
    
    /**
     * 设置默认策略
     */
    public void setDefaultStrategy(DataSourceRouter.SqlType sqlType, String strategyName) {
        defaultStrategies.put(sqlType, strategyName);
        log.info("设置默认策略: [{}] -> [{}]", sqlType, strategyName);
    }
    
    /**
     * 获取路由器
     */
    public DataSourceRouter getRouter(String name) {
        return routerRegistry.get(name);
    }
    
    /**
     * 获取最佳路由器
     */
    public DataSourceRouter getBestRouter(DataSourceRouter.SqlType sqlType, List<String> availableDataSources) {
        // 1. 尝试使用默认策略
        String defaultStrategy = defaultStrategies.get(sqlType);
        if (defaultStrategy != null) {
            DataSourceRouter router = routerRegistry.get(defaultStrategy);
            if (router != null && isRouterSuitable(router, sqlType, availableDataSources)) {
                return router;
            }
        }
        
        // 2. 根据SQL类型选择最合适的路由器
        return selectBestRouterByType(sqlType, availableDataSources);
    }
    
    /**
     * 根据SQL类型选择最合适的路由器
     */
    private DataSourceRouter selectBestRouterByType(DataSourceRouter.SqlType sqlType, List<String> availableDataSources) {
        // 读操作优先使用读写分离路由器
        if (sqlType.isReadOperation()) {
            DataSourceRouter router = findRouterByName("read-write");
            if (router != null && isRouterSuitable(router, sqlType, availableDataSources)) {
                return router;
            }
        }
        
        // 写操作优先使用读写分离路由器
        if (!sqlType.isReadOperation()) {
            DataSourceRouter router = findRouterByName("read-write");
            if (router != null && isRouterSuitable(router, sqlType, availableDataSources)) {
                return router;
            }
        }
        
        // 如果没有合适的读写分离路由器，使用故障转移路由器
        DataSourceRouter failoverRouter = findRouterByName("failover");
        if (failoverRouter != null && isRouterSuitable(failoverRouter, sqlType, availableDataSources)) {
            return failoverRouter;
        }
        
        // 最后返回第一个可用的路由器
        return routerRegistry.values().stream()
                .filter(router -> isRouterSuitable(router, sqlType, availableDataSources))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 根据名称查找路由器
     */
    private DataSourceRouter findRouterByName(String routerName) {
        return routerRegistry.get(routerName);
    }
    
    /**
     * 根据类型查找路由器（保留兼容性）
     */
    private DataSourceRouter findRouterByClass(Class<? extends DataSourceRouter> routerClass) {
        return routerRegistry.values().stream()
                .filter(routerClass::isInstance)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 检查路由器是否适合当前场景
     */
    private boolean isRouterSuitable(DataSourceRouter router, DataSourceRouter.SqlType sqlType, List<String> availableDataSources) {
        if (router == null || availableDataSources.isEmpty()) {
            return false;
        }
        
        // 这里可以添加更多的适合性检查逻辑
        // 例如：检查路由器是否支持当前的数据源数量、类型等
        
        return true;
    }
    
    /**
     * 获取所有注册的路由器
     */
    public Map<String, DataSourceRouter> getAllRouters() {
        return new ConcurrentHashMap<>(routerRegistry);
    }
    
    /**
     * 获取路由器统计信息
     */
    public RoutingStats getRoutingStats() {
        return RoutingStats.builder()
                .totalRouters(routerRegistry.size())
                .routerNames(routerRegistry.keySet())
                .defaultStrategies(defaultStrategies)
                .build();
    }
    
    /**
     * 路由统计信息
     */
    @lombok.Builder
    @lombok.Data
    public static class RoutingStats {
        private int totalRouters;
        private java.util.Set<String> routerNames;
        private Map<DataSourceRouter.SqlType, String> defaultStrategies;
    }
}
