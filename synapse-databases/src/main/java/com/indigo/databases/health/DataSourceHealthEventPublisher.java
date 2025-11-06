package com.indigo.databases.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 数据源健康状态事件发布器
 * 负责发布数据源健康状态变化事件
 *
 * @author 史偕成
 * @date 2025/01/15
 */
@Slf4j
@Component
public class DataSourceHealthEventPublisher {
    
    private final ApplicationEventPublisher eventPublisher;
    
    public DataSourceHealthEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * 发布数据源健康状态事件
     *
     * @param dataSourceName 数据源名称
     * @param healthy 是否健康
     * @param reason 状态变化原因
     */
    public void publishHealthStatus(String dataSourceName, boolean healthy, String reason) {
        DataSourceHealthEvent event = new DataSourceHealthEvent(dataSourceName, healthy, reason);
        publishEvent(event);
    }
    
    /**
     * 发布数据源健康状态事件（包含重试信息）
     *
     * @param dataSourceName 数据源名称
     * @param healthy 是否健康
     * @param reason 状态变化原因
     * @param attemptCount 当前尝试次数
     * @param maxRetries 最大重试次数
     */
    public void publishHealthStatus(String dataSourceName, boolean healthy, String reason, 
                                   int attemptCount, int maxRetries) {
        DataSourceHealthEvent event = new DataSourceHealthEvent(dataSourceName, healthy, reason, 
                                                               attemptCount, maxRetries);
        publishEvent(event);
    }
    
    /**
     * 发布数据源恢复事件
     *
     * @param dataSourceName 数据源名称
     */
    public void publishDataSourceRecovered(String dataSourceName) {
        DataSourceHealthEvent event = new DataSourceHealthEvent(dataSourceName, true, 
                                                               "Data source recovered from failure");
        publishEvent(event);
    }
    
    /**
     * 发布数据源故障事件
     *
     * @param dataSourceName 数据源名称
     * @param reason 故障原因
     */
    public void publishDataSourceFailure(String dataSourceName, String reason) {
        DataSourceHealthEvent event = new DataSourceHealthEvent(dataSourceName, false, reason);
        publishEvent(event);
    }
    
    /**
     * 发布健康检查开始事件
     *
     * @param dataSourceName 数据源名称
     */
    public void publishHealthCheckStarted(String dataSourceName) {
        DataSourceHealthEvent event = new DataSourceHealthEvent(dataSourceName, true, 
                                                               "Health check started");
        publishEvent(event);
    }
    
    /**
     * 内部方法：发布事件
     */
    private void publishEvent(DataSourceHealthEvent event) {
        try {
            eventPublisher.publishEvent(event);
            log.debug("发布数据源健康状态事件: 数据源={}, 健康状态={}, 原因={}", 
                    event.getDataSourceName(), event.isHealthy() ? "健康" : "故障", event.getReason());
        } catch (Exception e) {
            log.error("发布数据源健康状态事件失败: {}", event, e);
        }
    }
}
