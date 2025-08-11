package com.indigo.databases.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * 聚合查询分页结果DTO
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AggregationPageResult<T> extends PageResult<T> {
    
    /**
     * 聚合统计结果
     */
    private Map<String, Object> aggregations;
    
    /**
     * 分组统计
     */
    private Map<String, Long> groupCounts;
    
    public AggregationPageResult() {}
    
    public AggregationPageResult(List<T> records, Long total, Long current, Long size) {
        super(records, total, current, size);
    }
    
    /**
     * 创建带聚合结果的分页结果
     */
    public static <T> AggregationPageResult<T> withAggregations(List<T> records, Long total, Long current, Long size, 
                                                               Map<String, Object> aggregations) {
        AggregationPageResult<T> result = new AggregationPageResult<>(records, total, current, size);
        result.setAggregations(aggregations);
        return result;
    }
} 