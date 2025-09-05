package com.indigo.databases.routing;

import com.indigo.databases.config.SynapseDataSourceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 故障转移路由器
 * 实现数据源故障时的自动切换
 *
 * @author 史偕成
 * @date 2025/01/19
 */
@Slf4j
@Component
public class FailoverRouter implements DataSourceRouter {
    
    private final SynapseDataSourceProperties properties;
    
    // 数据源健康状态缓存
    private final Map<String, Boolean> dataSourceHealth = new ConcurrentHashMap<>();
    
    // 数据源故障计数
    private final Map<String, AtomicInteger> failureCount = new ConcurrentHashMap<>();
    
    // 数据源最后故障时间
    private final Map<String, Long> lastFailureTime = new ConcurrentHashMap<>();
    
    public FailoverRouter(SynapseDataSourceProperties properties) {
        this.properties = properties;
    }
    
    @Override
    public String selectDataSource(List<String> availableDataSources, RoutingContext context) {
        if (!properties.getFailover().isEnabled()) {
            return availableDataSources.get(0);
        }
        
        // 过滤出健康的数据源
        List<String> healthyDataSources = availableDataSources.stream()
                .filter(this::isDataSourceHealthy)
                .toList();
        
        if (healthyDataSources.isEmpty()) {
            log.warn("没有健康的数据源可用，尝试使用所有数据源");
            return selectFromAllDataSources(availableDataSources, context);
        }
        
        return selectFromHealthyDataSources(healthyDataSources, context);
    }
    
    /**
     * 从健康数据源中选择
     */
    private String selectFromHealthyDataSources(List<String> healthyDataSources, RoutingContext context) {
        SynapseDataSourceProperties.FailoverConfig.FailoverStrategy strategy = 
                properties.getFailover().getStrategy();
        
        return switch (strategy) {
            case PRIMARY_FIRST -> selectPrimaryFirst(healthyDataSources);
            case HEALTHY_FIRST -> selectHealthyFirst(healthyDataSources, context);
            case ROUND_ROBIN -> selectRoundRobin(healthyDataSources, context);
        };
    }
    
    /**
     * 主数据源优先策略
     */
    private String selectPrimaryFirst(List<String> healthyDataSources) {
        String primary = properties.getPrimary();
        
        if (healthyDataSources.contains(primary)) {
            return primary;
        }
        
        // 如果主数据源不健康，选择第一个健康的
        return healthyDataSources.get(0);
    }
    
    /**
     * 健康数据源优先策略
     */
    private String selectHealthyFirst(List<String> healthyDataSources, RoutingContext context) {
        // 根据数据源的健康评分选择
        return healthyDataSources.stream()
                .max((ds1, ds2) -> {
                    int score1 = calculateHealthScore(ds1);
                    int score2 = calculateHealthScore(ds2);
                    return Integer.compare(score1, score2);
                })
                .orElse(healthyDataSources.get(0));
    }
    
    /**
     * 轮询故障转移策略
     */
    private String selectRoundRobin(List<String> healthyDataSources, RoutingContext context) {
        String userId = context.getUserId();
        if (userId != null) {
            int index = Math.abs(userId.hashCode()) % healthyDataSources.size();
            return healthyDataSources.get(index);
        }
        
        long currentTime = System.currentTimeMillis();
        int index = (int) (currentTime / 1000) % healthyDataSources.size();
        return healthyDataSources.get(index);
    }
    
    /**
     * 从所有数据源中选择（兜底策略）
     */
    private String selectFromAllDataSources(List<String> allDataSources, RoutingContext context) {
        // 尝试选择故障次数最少的数据源
        return allDataSources.stream()
                .min((ds1, ds2) -> {
                    int failures1 = getFailureCount(ds1);
                    int failures2 = getFailureCount(ds2);
                    return Integer.compare(failures1, failures2);
                })
                .orElse(allDataSources.get(0));
    }
    
    /**
     * 计算数据源健康评分
     */
    private int calculateHealthScore(String dataSourceName) {
        int baseScore = 100;
        
        // 根据故障次数扣分
        int failures = getFailureCount(dataSourceName);
        baseScore -= failures * 10;
        
        // 根据最后故障时间加分（故障时间越久，加分越多）
        Long lastFailure = lastFailureTime.get(dataSourceName);
        if (lastFailure != null) {
            long timeSinceLastFailure = System.currentTimeMillis() - lastFailure;
            long minutesSinceLastFailure = timeSinceLastFailure / (1000 * 60);
            baseScore += Math.min(minutesSinceLastFailure, 50); // 最多加50分
        }
        
        return Math.max(baseScore, 0);
    }
    
    /**
     * 检查数据源是否健康
     */
    private boolean isDataSourceHealthy(String dataSourceName) {
        Boolean health = dataSourceHealth.get(dataSourceName);
        if (health == null) {
            // 如果还没有健康状态记录，默认认为是健康的
            return true;
        }
        return health;
    }
    
    /**
     * 获取故障次数
     */
    private int getFailureCount(String dataSourceName) {
        AtomicInteger count = failureCount.get(dataSourceName);
        return count != null ? count.get() : 0;
    }
    
    /**
     * 标记数据源故障
     */
    public void markDataSourceFailure(String dataSourceName) {
        dataSourceHealth.put(dataSourceName, false);
        failureCount.computeIfAbsent(dataSourceName, k -> new AtomicInteger(0)).incrementAndGet();
        lastFailureTime.put(dataSourceName, System.currentTimeMillis());
        
        log.warn("数据源 [{}] 标记为故障状态，故障次数: {}", 
                dataSourceName, getFailureCount(dataSourceName));
    }
    
    /**
     * 标记数据源恢复
     */
    public void markDataSourceRecovered(String dataSourceName) {
        dataSourceHealth.put(dataSourceName, true);
        // 故障计数保持不变，用于历史参考
        
        log.info("数据源 [{}] 标记为恢复状态", dataSourceName);
    }
    
    /**
     * 重置数据源故障状态
     */
    public void resetDataSourceFailure(String dataSourceName) {
        dataSourceHealth.put(dataSourceName, true);
        failureCount.remove(dataSourceName);
        lastFailureTime.remove(dataSourceName);
        
        log.info("数据源 [{}] 故障状态已重置", dataSourceName);
    }
    
    /**
     * 获取数据源健康状态
     */
    public Map<String, Boolean> getDataSourceHealthStatus() {
        return new ConcurrentHashMap<>(dataSourceHealth);
    }
    
    /**
     * 获取数据源故障统计
     */
    public Map<String, Integer> getDataSourceFailureStats() {
        Map<String, Integer> stats = new ConcurrentHashMap<>();
        failureCount.forEach((name, count) -> stats.put(name, count.get()));
        return stats;
    }
    
    @Override
    public String getStrategyName() {
        return "FAILOVER_ROUTER";
    }
}
