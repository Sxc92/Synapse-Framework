package com.indigo.databases.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * 性能监控分页结果DTO
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PerformancePageResult<T> extends PageResult<T> {
    
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
    private PageStatistics statistics;
    
    public PerformancePageResult() {}
    
    public PerformancePageResult(List<T> records, Long total, Long current, Long size) {
        super(records, total, current, size);
    }
    
    /**
     * 创建带性能信息的分页结果
     */
    public static <T> PerformancePageResult<T> withPerformance(List<T> records, Long total, Long current, Long size, 
                                                              Long queryTime, String executionPlan) {
        PerformancePageResult<T> result = new PerformancePageResult<>(records, total, current, size);
        result.setQueryTime(queryTime);
        result.setExecutionPlan(executionPlan);
        return result;
    }
    
    /**
     * 分页统计信息
     */
    @Data
    public static class PageStatistics {
        /**
         * 总记录数
         */
        private Long totalRecords;
        
        /**
         * 过滤后记录数
         */
        private Long filteredRecords;
        
        /**
         * 字段统计
         */
        private Map<String, FieldStatistics> fieldStatistics;
        
        public PageStatistics() {}
        
        public PageStatistics(Long totalRecords, Long filteredRecords) {
            this.totalRecords = totalRecords;
            this.filteredRecords = filteredRecords;
        }
    }
    
    /**
     * 字段统计信息
     */
    @Data
    public static class FieldStatistics {
        /**
         * 字段名
         */
        private String fieldName;
        
        /**
         * 最小值
         */
        private Object minValue;
        
        /**
         * 最大值
         */
        private Object maxValue;
        
        /**
         * 平均值
         */
        private Double avgValue;
        
        /**
         * 去重数量
         */
        private Long distinctCount;
        
        /**
         * 空值数量
         */
        private Long nullCount;
        
        public FieldStatistics() {}
        
        public FieldStatistics(String fieldName) {
            this.fieldName = fieldName;
        }
    }
} 