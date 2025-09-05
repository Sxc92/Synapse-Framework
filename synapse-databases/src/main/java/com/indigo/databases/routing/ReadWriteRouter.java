package com.indigo.databases.routing;

import com.indigo.databases.config.SynapseDataSourceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 读写分离路由器
 * 根据SQL类型自动选择读/写数据源
 *
 * @author 史偕成
 * @date 2025/01/19
 */
@Slf4j
@Component
public class ReadWriteRouter implements DataSourceRouter {
    
    private final SynapseDataSourceProperties properties;
    
    public ReadWriteRouter(SynapseDataSourceProperties properties) {
        this.properties = properties;
    }
    
    @Override
    public String selectDataSource(List<String> availableDataSources, RoutingContext context) {
        if (!properties.getReadWrite().isEnabled()) {
            return properties.getPrimary();
        }
        
        // 根据SQL类型选择数据源
        if (context.getSqlType().isReadOperation()) {
            return selectReadDataSource(availableDataSources, context);
        } else {
            return selectWriteDataSource(availableDataSources, context);
        }
    }
    
    /**
     * 选择读数据源
     */
    private String selectReadDataSource(List<String> availableDataSources, RoutingContext context) {
        List<String> readSources = properties.getReadWrite().getReadSources();
        
        if (readSources.isEmpty()) {
            log.debug("读数据源列表为空，使用主数据源");
            return properties.getPrimary();
        }
        
        // 过滤出可用的读数据源
        List<String> availableReadSources = readSources.stream()
                .filter(availableDataSources::contains)
                .toList();
        
        if (availableReadSources.isEmpty()) {
            log.warn("没有可用的读数据源，使用主数据源");
            return properties.getPrimary();
        }
        
        // 如果只有一个读数据源，直接返回
        if (availableReadSources.size() == 1) {
            return availableReadSources.get(0);
        }
        
        // 多个读数据源时，使用负载均衡策略
        return applyLoadBalance(availableReadSources, context);
    }
    
    /**
     * 选择写数据源
     */
    private String selectWriteDataSource(List<String> availableDataSources, RoutingContext context) {
        List<String> writeSources = properties.getReadWrite().getWriteSources();
        
        if (writeSources.isEmpty()) {
            log.debug("写数据源列表为空，使用主数据源");
            return properties.getPrimary();
        }
        
        // 过滤出可用的写数据源
        List<String> availableWriteSources = writeSources.stream()
                .filter(availableDataSources::contains)
                .toList();
        
        if (availableWriteSources.isEmpty()) {
            log.warn("没有可用的写数据源，使用主数据源");
            return properties.getPrimary();
        }
        
        // 写操作通常使用主数据源，如果有多个写数据源，选择第一个
        return availableWriteSources.get(0);
    }
    
    /**
     * 应用负载均衡策略
     */
    private String applyLoadBalance(List<String> dataSources, RoutingContext context) {
        SynapseDataSourceProperties.LoadBalanceConfig.LoadBalanceStrategy strategy = 
                properties.getLoadBalance().getStrategy();
        
        return switch (strategy) {
            case ROUND_ROBIN -> selectRoundRobin(dataSources, context);
            case WEIGHTED -> selectWeighted(dataSources, context);
            case RANDOM -> selectRandom(dataSources, context);
        };
    }
    
    /**
     * 轮询策略
     */
    private String selectRoundRobin(List<String> dataSources, RoutingContext context) {
        // 这里可以使用ThreadLocal或者用户ID来确保同一用户/会话的请求路由到同一数据源
        String userId = context.getUserId();
        if (userId != null) {
            int index = Math.abs(userId.hashCode()) % dataSources.size();
            return dataSources.get(index);
        }
        
        // 如果没有用户ID，使用简单的轮询
        long currentTime = System.currentTimeMillis();
        int index = (int) (currentTime / 1000) % dataSources.size();
        return dataSources.get(index);
    }
    
    /**
     * 权重策略
     */
    private String selectWeighted(List<String> dataSources, RoutingContext context) {
        Map<String, Integer> weights = properties.getLoadBalance().getWeights();
        
        // 如果没有配置权重，使用轮询
        if (weights.isEmpty()) {
            return selectRoundRobin(dataSources, context);
        }
        
        // 计算总权重
        int totalWeight = dataSources.stream()
                .mapToInt(ds -> weights.getOrDefault(ds, 100))
                .sum();
        
        if (totalWeight == 0) {
            return selectRoundRobin(dataSources, context);
        }
        
        // 根据权重选择数据源
        int random = (int) (Math.random() * totalWeight);
        int currentWeight = 0;
        
        for (String dataSource : dataSources) {
            currentWeight += weights.getOrDefault(dataSource, 100);
            if (random < currentWeight) {
                return dataSource;
            }
        }
        
        // 兜底，返回第一个数据源
        return dataSources.get(0);
    }
    
    /**
     * 随机策略
     */
    private String selectRandom(List<String> dataSources, RoutingContext context) {
        int index = (int) (Math.random() * dataSources.size());
        return dataSources.get(index);
    }
    
    @Override
    public String getStrategyName() {
        return "READ_WRITE_ROUTER";
    }
}
