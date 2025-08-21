package com.indigo.databases.dto.result;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * 增强分页结果DTO - 组合聚合查询和性能监控功能
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EnhancedPageResult<T> extends AggregationPageResult<T> {
    
    /**
     * 查询性能信息
     */
    private Long queryTime;
    
    /**
     * 执行计划
     */
    private String executionPlan;
    
    /**
     * 元数据信息
     */
    private Map<String, Object> metadata;
    
    /**
     * 分页统计
     */
    private PerformancePageResult.PageStatistics statistics;
    
    public EnhancedPageResult() {}
    
    public EnhancedPageResult(List<T> records, Long total, Long current, Long size) {
        super(records, total, current, size);
    }
    
    /**
     * 创建增强分页结果
     */
    public static <T> EnhancedPageResult<T> enhanced(List<T> records, Long total, Long current, Long size, 
                                                    Map<String, Object> aggregations, Long queryTime, String executionPlan) {
        EnhancedPageResult<T> result = new EnhancedPageResult<>(records, total, current, size);
        result.setAggregations(aggregations);
        result.setQueryTime(queryTime);
        result.setExecutionPlan(executionPlan);
        return result;
    }
} 