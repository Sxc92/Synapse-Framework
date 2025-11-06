package com.indigo.databases.dynamic;

import com.indigo.databases.routing.DataSourceRouter;
import com.indigo.databases.routing.FailoverRouter;
import com.indigo.databases.routing.ReadWriteRouter;
import com.indigo.databases.routing.SmartRouterSelector;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;

/**
 * 智能动态路由数据源
 * 支持读写分离、负载均衡、故障转移
 *
 * @author 史偕成
 * @date 2025/03/21
 */
@Slf4j
public class DynamicRoutingDataSource extends AbstractRoutingDataSource {
    
    // 数据源映射
    private final Map<String, DataSource> dataSourceMap = new ConcurrentHashMap<>();
    
    // 路由器列表
    private final List<DataSourceRouter> routers;
    
    // 故障转移路由器（特殊处理）
    private final FailoverRouter failoverRouter;
    
    // 智能路由器选择器
    private final SmartRouterSelector routerSelector;
    
    // 配置信息
    private final DataSourceConfig config;
    
    public DynamicRoutingDataSource(List<DataSourceRouter> routers, 
                                  SmartRouterSelector routerSelector,
                                  DataSourceConfig config) {
        this.routers = routers;
        this.routerSelector = routerSelector;
        this.config = config;
        
        // 找到故障转移路由器
        this.failoverRouter = routers.stream()
                .filter(router -> router instanceof FailoverRouter)
                .map(router -> (FailoverRouter) router)
                .findFirst()
                .orElse(null);
        
        // 注册路由器到选择器
        registerRouters();
        
        // 设置默认数据源
        setDefaultTargetDataSource(null);
        // 设置数据源映射
        setTargetDataSources(new HashMap<>(dataSourceMap));
        
        // 解决循环依赖：设置 FailoverRouter 的动态路由数据源引用
        if (failoverRouter != null) {
            failoverRouter.setDynamicRoutingDataSource(this);
        }
    }
    
    /**
     * 注册路由器到选择器
     */
    private void registerRouters() {
        // 注册所有路由器到选择器
        routers.forEach(router -> {
            String routerName = getRouterName(router);
            routerSelector.registerRouter(routerName, router);
            
            // 设置策略
            if (router instanceof ReadWriteRouter) {
                setReadWriteStrategies(routerName);
            } else if (router instanceof FailoverRouter) {
                setFailoverStrategies(routerName);
            }
        });
        
        log.info("成功注册 {} 个路由器到智能选择器", routers.size());
    }
    
    /**
     * 获取路由器名称
     */
    private String getRouterName(DataSourceRouter router) {
        if (router instanceof ReadWriteRouter) {
            return "read-write";
        } else if (router instanceof FailoverRouter) {
            return "failover";
        } else {
            return router.getStrategyName().toLowerCase();
        }
    }
    
    /**
     * 设置读写分离策略
     */
    private void setReadWriteStrategies(String routerName) {
        // 设置为主要策略
        routerSelector.setPrimaryStrategy(DataSourceRouter.SqlType.SELECT, routerName);
        routerSelector.setPrimaryStrategy(DataSourceRouter.SqlType.INSERT, routerName);
        routerSelector.setPrimaryStrategy(DataSourceRouter.SqlType.UPDATE, routerName);
        routerSelector.setPrimaryStrategy(DataSourceRouter.SqlType.DELETE, routerName);
        routerSelector.setPrimaryStrategy(DataSourceRouter.SqlType.MERGE, routerName);
        routerSelector.setPrimaryStrategy(DataSourceRouter.SqlType.CALL, routerName);
        routerSelector.setPrimaryStrategy(DataSourceRouter.SqlType.OTHER, routerName);
        log.debug("设置读写分离路由器 [{}] 为主要策略", routerName);
    }
    
    /**
     * 设置故障转移策略
     */
    private void setFailoverStrategies(String routerName) {
        // 设置为备用策略
        routerSelector.setFallbackStrategy(DataSourceRouter.SqlType.SELECT, routerName);
        routerSelector.setFallbackStrategy(DataSourceRouter.SqlType.INSERT, routerName);
        routerSelector.setFallbackStrategy(DataSourceRouter.SqlType.UPDATE, routerName);
        routerSelector.setFallbackStrategy(DataSourceRouter.SqlType.DELETE, routerName);
        routerSelector.setFallbackStrategy(DataSourceRouter.SqlType.MERGE, routerName);
        routerSelector.setFallbackStrategy(DataSourceRouter.SqlType.CALL, routerName);
        routerSelector.setFallbackStrategy(DataSourceRouter.SqlType.OTHER, routerName);
        log.debug("设置故障转移路由器 [{}] 为备用策略", routerName);
    }
    
    @Override
    protected Object determineCurrentLookupKey() {
        String dataSourceName = DynamicDataSourceContextHolder.getDataSource();
        
        // 如果明确指定了数据源，直接使用
        if (StringUtils.hasText(dataSourceName)) {
            if (isDataSourceHealthy(dataSourceName)) {
                log.debug("使用指定数据源: [{}]", dataSourceName);
                return dataSourceName;
            } else {
                log.warn("指定数据源 [{}] 不健康，尝试自动路由", dataSourceName);
                dataSourceName = null;
            }
        }
        
        // 自动路由策略
        return determineOptimalDataSource();
    }
    
    /**
     * 智能确定最优数据源
     */
    private String determineOptimalDataSource() {
        // 1. 获取当前SQL类型
        DataSourceRouter.SqlType sqlType = getCurrentSqlType();
        
        // 2. 根据SQL类型选择数据源
        String dataSourceName;
        if (sqlType.isReadOperation()) {
            dataSourceName = selectReadDataSource();
        } else {
            dataSourceName = selectWriteDataSource();
        }
        
        // 3. 验证数据源健康状态
        if (!isDataSourceHealthy(dataSourceName)) {
            dataSourceName = selectFallbackDataSource();
        }
        
        log.debug("自动路由到数据源: [{}], SQL类型: [{}]", dataSourceName, sqlType);
        return dataSourceName;
    }
    
    /**
     * 选择读数据源
     */
    private String selectReadDataSource() {
        // 使用路由策略管理器选择最佳路由器
        DataSourceRouter router = routerSelector.selectRouter(
                DataSourceRouter.SqlType.SELECT, 
                getAvailableDataSources()
        );
        
        if (router != null) {
            DataSourceRouter.RoutingContext context = createRoutingContext();
            return router.selectDataSource(getAvailableDataSources(), context);
        }
        
        // 兜底：使用主数据源
        return config.getPrimary();
    }
    
    /**
     * 选择写数据源
     */
    private String selectWriteDataSource() {
        // 使用路由策略管理器选择最佳路由器
        DataSourceRouter router = routerSelector.selectRouter(
                DataSourceRouter.SqlType.INSERT, 
                getAvailableDataSources()
        );
        
        if (router != null) {
            DataSourceRouter.RoutingContext context = createRoutingContext();
            return router.selectDataSource(getAvailableDataSources(), context);
        }
        
        // 兜底：使用主数据源
        return config.getPrimary();
    }
    
    /**
     * 选择降级数据源
     */
    private String selectFallbackDataSource() {
        if (failoverRouter != null) {
            List<String> availableDataSources = getAvailableDataSources();
            DataSourceRouter.RoutingContext context = createRoutingContext();
            return failoverRouter.selectDataSource(availableDataSources, context);
        }
        
        // 兜底：使用主数据源
        return config.getPrimary();
    }
    
    /**
     * 获取当前SQL类型
     */
    private DataSourceRouter.SqlType getCurrentSqlType() {
        // 这里可以通过ThreadLocal或其他方式获取当前执行的SQL类型
        // 暂时返回默认值，实际实现时需要集成SQL解析器
        return DataSourceRouter.SqlType.SELECT;
    }
    
    /**
     * 检查数据源健康状态
     */
    private boolean isDataSourceHealthy(String dataSourceName) {
        if (failoverRouter != null) {
            Map<String, Boolean> healthStatus = failoverRouter.getDataSourceHealthStatus();
            return healthStatus.getOrDefault(dataSourceName, true);
        }
        return true;
    }
    
    /**
     * 获取可用数据源列表
     */
    private List<String> getAvailableDataSources() {
        return dataSourceMap.keySet().stream().toList();
    }
    
    /**
     * 创建路由上下文
     */
    private DataSourceRouter.RoutingContext createRoutingContext() {
        // 这里可以从ThreadLocal或其他地方获取用户信息
        String userId = getCurrentUserId();
        String tenantId = getCurrentTenantId();
        
        return new DataSourceRouter.RoutingContext(
                DynamicDataSourceContextHolder.getDataSource(),
                getCurrentSqlType(),
                userId,
                tenantId
        );
    }
    
    /**
     * 获取当前用户ID
     */
    private String getCurrentUserId() {
        // 这里可以从安全上下文或其他地方获取用户ID
        // 暂时返回null，实际实现时需要集成安全框架
        return null;
    }
    
    /**
     * 获取当前租户ID
     */
    private String getCurrentTenantId() {
        // 这里可以从租户上下文获取租户ID
        // 暂时返回null，实际实现时需要集成多租户框架
        return null;
    }
    
    /**
     * 添加数据源
     */
    public void addDataSource(String name, DataSource dataSource) {
        dataSourceMap.put(name, dataSource);
        updateTargetDataSources();
        
        log.debug("添加数据源: [{}]", name);
    }
    
    /**
     * 移除数据源
     */
    public void removeDataSource(String name) {
        dataSourceMap.remove(name);
        updateTargetDataSources();
        
        log.debug("移除数据源: [{}]", name);
    }
    
    /**
     * 更新目标数据源
     */
    private void updateTargetDataSources() {
        setTargetDataSources(new HashMap<>(dataSourceMap));
        afterPropertiesSet();
    }
    
    /**
     * 获取所有数据源
     */
    public Map<String, DataSource> getDataSources() {
        return new ConcurrentHashMap<>(dataSourceMap);
    }
    
    /**
     * 获取当前数据源
     */
    public String getCurrentDataSource() {
        return DynamicDataSourceContextHolder.getDataSource();
    }
    
    /**
     * 标记数据源故障
     */
    public void markDataSourceFailure(String dataSourceName) {
        if (failoverRouter != null) {
            failoverRouter.markDataSourceFailure(dataSourceName);
        }
    }
    
    /**
     * 标记数据源恢复
     */
    public void markDataSourceRecovered(String dataSourceName) {
        if (failoverRouter != null) {
            failoverRouter.markDataSourceRecovered(dataSourceName);
        }
    }
    
    /**
     * 切换默认数据源
     */
    public void switchDefaultDataSource(String dataSourceName) {
        DataSource targetDataSource = dataSourceMap.get(dataSourceName);
        if (targetDataSource != null) {
            setDefaultTargetDataSource(targetDataSource);
            log.info("默认数据源已切换到: {}", dataSourceName);
        } else {
            log.warn("无法切换到数据源 [{}]，数据源不存在", dataSourceName);
        }
    }
    
    /**
     * 获取数据源统计信息
     */
    public DataSourceStats getDataSourceStats() {
        return DataSourceStats.builder()
                .totalDataSources(dataSourceMap.size())
                .healthyDataSources((int) getAvailableDataSources().stream()
                        .filter(this::isDataSourceHealthy)
                        .count())
                .build();
    }
    
    /**
     * 获取路由策略统计信息
     */
    public SmartRouterSelector.RouterSelectionStats getRoutingStrategyStats() {
        return routerSelector.getSelectionStats();
    }

    /**
     * 数据源配置
     */
    @Getter
    public static class DataSourceConfig {
        private final String primary;

        public DataSourceConfig(String primary) {
            this.primary = primary;
        }

    }
    
    /**
     * 数据源统计信息
     */
    @lombok.Builder
    public static class DataSourceStats {
        private final int totalDataSources;
        private final int healthyDataSources;
    }
} 