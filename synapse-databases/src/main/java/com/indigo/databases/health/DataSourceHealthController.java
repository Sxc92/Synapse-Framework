package com.indigo.databases.health;

import com.indigo.databases.routing.FailoverRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据源健康状态监控控制器
 * 提供数据源健康状态查询接口
 *
 * @author 史偕成
 * @date 2025/01/15
 */
@Slf4j
@RestController
@RequestMapping("/api/datasource/health")
public class DataSourceHealthController {
    
    @Autowired
    private DataSourceHealthChecker healthChecker;
    
    @Autowired
    private FailoverRouter failoverRouter;
    
    /**
     * 获取所有数据源的健康状态
     */
    @GetMapping("/status")
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> result = new HashMap<>();
        
        // 健康检查器状态
        result.put("healthChecker", Map.of(
            "initialized", healthChecker.isInitialized(),
            "healthStatus", healthChecker.getHealthStatus(),
            "dataSourceTypes", healthChecker.getDataSourceTypes()
        ));
        
        // 故障转移路由器状态
        result.put("failoverRouter", Map.of(
            "healthStatus", failoverRouter.getDataSourceHealthStatus(),
            "failureStats", failoverRouter.getDataSourceFailureStats()
        ));
        
        return result;
    }
    
    /**
     * 获取数据源健康状态摘要
     */
    @GetMapping("/summary")
    public Map<String, Object> getHealthSummary() {
        Map<String, Boolean> healthStatus = healthChecker.getHealthStatus();
        Map<String, Integer> failureStats = failoverRouter.getDataSourceFailureStats();
        
        long healthyCount = healthStatus.values().stream().mapToLong(healthy -> healthy ? 1 : 0).sum();
        long totalCount = healthStatus.size();
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalDataSources", totalCount);
        summary.put("healthyDataSources", healthyCount);
        summary.put("unhealthyDataSources", totalCount - healthyCount);
        summary.put("healthPercentage", totalCount > 0 ? (healthyCount * 100.0 / totalCount) : 0);
        summary.put("healthStatus", healthStatus);
        summary.put("failureStats", failureStats);
        
        return summary;
    }
    
    /**
     * 手动触发健康检查
     */
    @GetMapping("/check")
    public Map<String, Object> triggerHealthCheck() {
        try {
            healthChecker.checkHealthAndWait();
            return Map.of(
                "success", true,
                "message", "Health check triggered successfully",
                "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            log.error("手动触发健康检查失败", e);
            return Map.of(
                "success", false,
                "message", "Health check failed: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
        }
    }
}
