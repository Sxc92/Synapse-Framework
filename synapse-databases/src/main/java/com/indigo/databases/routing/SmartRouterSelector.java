package com.indigo.databases.routing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能路由器选择器
 * 完全基于配置和策略来选择路由器，避免硬编码类型判断
 *
 * @author 史偕成
 * @date 2025/01/19
 */
@Slf4j
@Component
public class SmartRouterSelector {
    
    private final Map<String, DataSourceRouter> routerRegistry = new ConcurrentHashMap<>();
    private final Map<DataSourceRouter.SqlType, String> primaryStrategies = new ConcurrentHashMap<>();
    private final Map<DataSourceRouter.SqlType, String> fallbackStrategies = new ConcurrentHashMap<>();
    
    /**
     * 注册路由器
     */
    public void registerRouter(String name, DataSourceRouter router) {
        routerRegistry.put(name, router);
        log.debug("智能路由器选择器注册路由器: [{}] -> [{}]", name, router.getStrategyName());
    }
    
    /**
     * 设置主要策略
     */
    public void setPrimaryStrategy(DataSourceRouter.SqlType sqlType, String strategyName) {
        primaryStrategies.put(sqlType, strategyName);
        log.debug("设置主要策略: [{}] -> [{}]", sqlType, strategyName);
    }
    
    /**
     * 设置备用策略
     */
    public void setFallbackStrategy(DataSourceRouter.SqlType sqlType, String strategyName) {
        fallbackStrategies.put(sqlType, strategyName);
        log.debug("设置备用策略: [{}] -> [{}]", sqlType, strategyName);
    }
    
    /**
     * 智能选择路由器
     */
    public DataSourceRouter selectRouter(DataSourceRouter.SqlType sqlType, List<String> availableDataSources) {
        // 1. 尝试使用主要策略
        String primaryStrategy = primaryStrategies.get(sqlType);
        if (primaryStrategy != null) {
            DataSourceRouter router = routerRegistry.get(primaryStrategy);
            if (isRouterAvailable(router, availableDataSources)) {
                log.debug("使用主要策略 [{}] 处理 [{}] 操作", primaryStrategy, sqlType);
                return router;
            }
        }
        
        // 2. 尝试使用备用策略
        String fallbackStrategy = fallbackStrategies.get(sqlType);
        if (fallbackStrategy != null) {
            DataSourceRouter router = routerRegistry.get(fallbackStrategy);
            if (isRouterAvailable(router, availableDataSources)) {
                log.debug("使用备用策略 [{}] 处理 [{}] 操作", fallbackStrategy, sqlType);
                return router;
            }
        }
        
        // 3. 智能兜底选择
        DataSourceRouter fallbackRouter = selectFallbackRouter(sqlType, availableDataSources);
        if (fallbackRouter != null) {
            log.debug("使用兜底路由器 [{}] 处理 [{}] 操作", 
                    fallbackRouter.getStrategyName(), sqlType);
        }
        
        return fallbackRouter;
    }
    
    /**
     * 选择兜底路由器
     */
    private DataSourceRouter selectFallbackRouter(DataSourceRouter.SqlType sqlType, List<String> availableDataSources) {
        // 优先选择故障转移路由器
        DataSourceRouter failoverRouter = routerRegistry.get("failover");
        if (isRouterAvailable(failoverRouter, availableDataSources)) {
            return failoverRouter;
        }
        
        // 然后选择读写分离路由器
        DataSourceRouter readWriteRouter = routerRegistry.get("read-write");
        if (isRouterAvailable(readWriteRouter, availableDataSources)) {
            return readWriteRouter;
        }
        
        // 最后选择第一个可用的路由器
        return routerRegistry.values().stream()
                .filter(router -> isRouterAvailable(router, availableDataSources))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 检查路由器是否可用
     */
    private boolean isRouterAvailable(DataSourceRouter router, List<String> availableDataSources) {
        if (router == null || availableDataSources.isEmpty()) {
            return false;
        }
        
        // 这里可以添加更多的可用性检查逻辑
        // 例如：检查路由器是否支持当前的数据源数量、类型等
        
        return true;
    }
    
    /**
     * 获取路由器
     */
    public DataSourceRouter getRouter(String name) {
        return routerRegistry.get(name);
    }
    
    /**
     * 获取所有路由器
     */
    public Map<String, DataSourceRouter> getAllRouters() {
        return new ConcurrentHashMap<>(routerRegistry);
    }
    
    /**
     * 获取策略统计信息
     */
    public RouterSelectionStats getSelectionStats() {
        return RouterSelectionStats.builder()
                .totalRouters(routerRegistry.size())
                .routerNames(routerRegistry.keySet())
                .primaryStrategies(primaryStrategies)
                .fallbackStrategies(fallbackStrategies)
                .build();
    }
    
    /**
     * 路由器选择统计信息
     */
    @lombok.Builder
    @lombok.Data
    public static class RouterSelectionStats {
        private int totalRouters;
        private java.util.Set<String> routerNames;
        private Map<DataSourceRouter.SqlType, String> primaryStrategies;
        private Map<DataSourceRouter.SqlType, String> fallbackStrategies;
    }
}
