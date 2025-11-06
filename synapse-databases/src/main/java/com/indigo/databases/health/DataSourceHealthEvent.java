package com.indigo.databases.health;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 数据源健康状态事件
 * 用于在健康检查器和故障转移路由器之间传递健康状态变化
 *
 * @author 史偕成
 * @date 2025/01/15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceHealthEvent {
    
    /**
     * 数据源名称
     */
    private String dataSourceName;
    
    /**
     * 是否健康
     */
    private boolean healthy;
    
    /**
     * 事件时间戳
     */
    private long timestamp;
    
    /**
     * 健康状态变化原因
     */
    private String reason;
    
    /**
     * 健康检查尝试次数
     */
    private int attemptCount;
    
    /**
     * 最大重试次数
     */
    private int maxRetries;
    
    /**
     * 构造函数 - 简化版本
     */
    public DataSourceHealthEvent(String dataSourceName, boolean healthy, String reason) {
        this.dataSourceName = dataSourceName;
        this.healthy = healthy;
        this.reason = reason;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 构造函数 - 完整版本
     */
    public DataSourceHealthEvent(String dataSourceName, boolean healthy, String reason, 
                                int attemptCount, int maxRetries) {
        this.dataSourceName = dataSourceName;
        this.healthy = healthy;
        this.reason = reason;
        this.timestamp = System.currentTimeMillis();
        this.attemptCount = attemptCount;
        this.maxRetries = maxRetries;
    }
    
    @Override
    public String toString() {
        return String.format("DataSourceHealthEvent{dataSource='%s', healthy=%s, reason='%s', " +
                           "attempt=%d/%d, timestamp=%d}", 
                           dataSourceName, healthy, reason, attemptCount, maxRetries, timestamp);
    }
}
