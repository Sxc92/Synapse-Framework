package com.indigo.databases.utils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.indigo.databases.dto.PageDTO;
import com.indigo.databases.dto.page.*;
import com.indigo.databases.dto.result.AggregationPageResult;
import com.indigo.databases.dto.result.EnhancedPageResult;
import com.indigo.databases.dto.result.PageResult;
import com.indigo.databases.dto.result.PerformancePageResult;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 增强查询构建器
 * 基于 MyBatis-Plus 已有功能，提供便捷的查询方法
 * 使用 IService 确保所有 MyBatis-Plus 功能正常工作
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Slf4j
public class EnhancedQueryBuilder {

    // ==================== 基础分页查询 ====================
    
    /**
     * 基础分页查询
     * 使用 IService 确保 MetaObjectHandler 等功能正常工作
     */
    public static <T> PageResult<T> pageWithCondition(IService<T> service, PageDTO pageDTO) {
        try {
            // 构建基础查询条件
            QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(pageDTO);
            
            // 使用 IService 的分页方法，确保所有功能正常工作
            Page<T> page = new Page<>(pageDTO.getPageNo(), pageDTO.getPageSize());
            Page<T> result = service.page(page, wrapper);
            
            return PageResult.fromIPage(result);
            
        } catch (Exception e) {
            log.error("基础分页查询失败", e);
            throw new RuntimeException("查询失败: " + e.getMessage(), e);
        }
    }
    
    // ==================== 聚合查询 ====================
    
    /**
     * 聚合查询 - 使用 IService 的 list 方法
     */
    public static <T> AggregationPageResult<T> pageWithAggregation(IService<T> service, AggregationPageDTO pageDTO) {
        try {
            // 构建基础查询条件
            QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(pageDTO);
            
            // 构建聚合字段
            if (pageDTO.getAggregations() != null && !pageDTO.getAggregations().isEmpty()) {
                String[] selectFields = pageDTO.getAggregations().stream()
                    .map(agg -> buildAggregationField(agg))
                    .toArray(String[]::new);
                wrapper.select(selectFields);
                
                // 使用 IService 的 list 方法
                List<T> records = service.list(wrapper);
                
                // 构建聚合结果
                Map<String, Object> aggregations = buildAggregationResults(pageDTO.getAggregations(), records);
                
                // 聚合查询返回单条记录，包含所有聚合值
                return AggregationPageResult.withAggregations(
                    records,
                    (long) records.size(),
                    1L,
                    (long) records.size(),
                    aggregations
                );
            } else {
                // 如果没有聚合字段，执行普通分页查询
                Page<T> page = new Page<>(pageDTO.getPageNo(), pageDTO.getPageSize());
                Page<T> result = service.page(page, wrapper);
                
                return new AggregationPageResult<>(result.getRecords(), result.getTotal(), 
                                                 result.getCurrent(), result.getSize());
            }
            
        } catch (Exception e) {
            log.error("聚合查询失败", e);
            throw new RuntimeException("聚合查询失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 分组查询 - 使用 IService 的 list 方法
     */
    public static <T> AggregationPageResult<T> pageWithGroupBy(IService<T> service, AggregationPageDTO pageDTO) {
        try {
            // 构建基础查询条件
            QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(pageDTO);
            
            // 构建聚合字段
            if (pageDTO.getAggregations() != null && !pageDTO.getAggregations().isEmpty()) {
                String[] selectFields = pageDTO.getAggregations().stream()
                    .map(agg -> buildAggregationField(agg))
                    .toArray(String[]::new);
                wrapper.select(selectFields);
            }
            
            // 添加分组字段
            if (pageDTO.getGroupByFields() != null && !pageDTO.getGroupByFields().isEmpty()) {
                for (String field : pageDTO.getGroupByFields()) {
                    wrapper.groupBy(field);
                }
            }
            
            // 使用 IService 的 list 方法
            List<T> records = service.list(wrapper);
            
            // 分组查询返回多条记录，每条记录代表一个分组
            return new AggregationPageResult<>(records, (long) records.size(), 1L, (long) records.size());
            
        } catch (Exception e) {
            log.error("分组查询失败", e);
            throw new RuntimeException("分组查询失败: " + e.getMessage(), e);
        }
    }
    
    // ==================== 性能优化查询 ====================
    
    /**
     * 性能监控查询
     * 返回查询执行时间和执行计划
     */
    public static <T> PerformancePageResult<T> pageWithPerformance(IService<T> service, PerformancePageDTO pageDTO) {
        try {
            long startTime = System.currentTimeMillis();
            
            // 构建基础查询条件
            QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(pageDTO);
            
            // 执行查询
            Page<T> page = new Page<>(pageDTO.getPageNo(), pageDTO.getPageSize());
            Page<T> result = service.page(page, wrapper);
            
            long endTime = System.currentTimeMillis();
            long queryTime = endTime - startTime;
            
            // 获取执行计划
            String executionPlan = getExecutionPlan(wrapper);
            
            // 构建性能统计
            PerformancePageResult.PageStatistics statistics = buildStatistics(pageDTO, queryTime, executionPlan);
            
            PerformancePageResult<T> performanceResult = new PerformancePageResult<>(result.getRecords(), result.getTotal(), 
                                             result.getCurrent(), result.getSize());
            performanceResult.setQueryTime(queryTime);
            performanceResult.setExecutionPlan(executionPlan);
            performanceResult.setStatistics(statistics);
            
            return performanceResult;
            
        } catch (Exception e) {
            log.error("性能监控查询失败", e);
            throw new RuntimeException("性能监控查询失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 缓存查询
     * 支持查询结果缓存
     */
    public static <T> PerformancePageResult<T> pageWithCache(IService<T> service, PerformancePageDTO pageDTO) {
        try {
            // 这里应该实现缓存逻辑
            log.info("缓存查询功能待实现");
            
            // 暂时直接调用性能监控查询
            return pageWithPerformance(service, pageDTO);
            
        } catch (Exception e) {
            log.error("缓存查询失败", e);
            throw new RuntimeException("缓存查询失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 字段选择查询
     * 支持指定返回字段，提高查询性能
     */
    public static <T> PerformancePageResult<T> pageWithSelectFields(IService<T> service, PerformancePageDTO pageDTO) {
        try {
            // 构建基础查询条件
            QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(pageDTO);
            
            // 设置选择字段
            if (pageDTO.getSelectFields() != null && !pageDTO.getSelectFields().isEmpty()) {
                wrapper.select(pageDTO.getSelectFields().toArray(new String[0]));
            }
            
            // 执行查询
            Page<T> page = new Page<>(pageDTO.getPageNo(), pageDTO.getPageSize());
            Page<T> result = service.page(page, wrapper);
            
            return new PerformancePageResult<>(result.getRecords(), result.getTotal(), 
                                             result.getCurrent(), result.getSize());
            
        } catch (Exception e) {
            log.error("字段选择查询失败", e);
            throw new RuntimeException("字段选择查询失败: " + e.getMessage(), e);
        }
    }
    
    // ==================== 多表关联查询 ====================
    
    /**
     * 多表关联查询
     * 支持 INNER, LEFT, RIGHT, FULL JOIN
     */
    public static <T> PageResult<T> pageWithJoin(IService<T> service, JoinPageDTO pageDTO) {
        try {
            // 构建基础查询条件
            QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(pageDTO);
            
            // 添加表关联
            if (pageDTO.getTableJoins() != null && !pageDTO.getTableJoins().isEmpty()) {
                for (JoinPageDTO.TableJoin join : pageDTO.getTableJoins()) {
                    // 使用 MyBatis-Plus 的 apply 方法添加自定义 SQL
                    String joinSql = String.format("%s %s %s ON %s", 
                        join.getJoinType().getSqlKeyword(), join.getTableName(), join.getTableAlias(), join.getJoinCondition());
                    wrapper.apply(joinSql);
                    
                    // 设置选择字段
                    if (join.getSelectFields() != null && !join.getSelectFields().isEmpty()) {
                        String[] fields = join.getSelectFields().toArray(new String[0]);
                        wrapper.select(fields);
                    }
                }
            }
            
            // 执行查询
            Page<T> page = new Page<>(pageDTO.getPageNo(), pageDTO.getPageSize());
            Page<T> result = service.page(page, wrapper);
            
            return PageResult.fromIPage(result);
            
        } catch (Exception e) {
            log.error("多表关联查询失败", e);
            throw new RuntimeException("多表关联查询失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 复杂查询
     * 支持自定义SQL查询
     */
    public static <T> PageResult<T> pageWithComplexQuery(IService<T> service, ComplexPageDTO pageDTO) {
        try {
            // 构建基础查询条件
            QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(pageDTO);
            
            // 添加自定义SQL
            if (pageDTO.getEnableComplexQuery() != null && pageDTO.getEnableComplexQuery() && pageDTO.getCustomSql() != null) {
                wrapper.apply(pageDTO.getCustomSql(), pageDTO.getCustomSqlParams());
            }
            
            // 执行查询
            Page<T> page = new Page<>(pageDTO.getPageNo(), pageDTO.getPageSize());
            Page<T> result = service.page(page, wrapper);
            
            return PageResult.fromIPage(result);
            
        } catch (Exception e) {
            log.error("复杂查询失败", e);
            throw new RuntimeException("复杂查询失败: " + e.getMessage(), e);
        }
    }
    
    // ==================== 增强查询（组合功能） ====================
    
    /**
     * 增强查询（组合聚合和性能优化）
     */
    public static <T> EnhancedPageResult<T> pageWithEnhanced(IService<T> service, EnhancedPageDTO pageDTO) {
        try {
            long startTime = System.currentTimeMillis();
            
            // 构建基础查询条件
            QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(pageDTO);
            
            // 设置选择字段
            if (pageDTO.getSelectFields() != null && !pageDTO.getSelectFields().isEmpty()) {
                wrapper.select(pageDTO.getSelectFields().toArray(new String[0]));
            }
            
            // 添加聚合字段
            if (pageDTO.getAggregations() != null && !pageDTO.getAggregations().isEmpty()) {
                String[] selectFields = pageDTO.getAggregations().stream()
                    .map(agg -> buildAggregationField(agg))
                    .toArray(String[]::new);
                wrapper.select(selectFields);
            }
            
            // 执行查询
            Page<T> page = new Page<>(pageDTO.getPageNo(), pageDTO.getPageSize());
            Page<T> result = service.page(page, wrapper);
            
            long endTime = System.currentTimeMillis();
            long queryTime = endTime - startTime;
            
            // 获取执行计划
            String executionPlan = getExecutionPlan(wrapper);
            
            // 构建聚合结果
            Map<String, Object> aggregations = new HashMap<>();
            if (pageDTO.getAggregations() != null && !pageDTO.getAggregations().isEmpty()) {
                aggregations = buildAggregationResults(pageDTO.getAggregations(), result.getRecords());
            }
            
            EnhancedPageResult<T> enhancedResult = new EnhancedPageResult<>(result.getRecords(), result.getTotal(), 
                                          result.getCurrent(), result.getSize());
            enhancedResult.setQueryTime(queryTime);
            enhancedResult.setExecutionPlan(executionPlan);
            enhancedResult.setAggregations(aggregations);
            
            return enhancedResult;
            
        } catch (Exception e) {
            log.error("增强查询失败", e);
            throw new RuntimeException("增强查询失败: " + e.getMessage(), e);
        }
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 构建性能统计信息
     */
    private static PerformancePageResult.PageStatistics buildStatistics(PerformancePageDTO pageDTO, long queryTime, String executionPlan) {
        PerformancePageResult.PageStatistics statistics = new PerformancePageResult.PageStatistics();
        // 这里可以添加更多的统计信息
        return statistics;
    }

    /**
     * 构建聚合字段
     */
    private static String buildAggregationField(AggregationPageDTO.AggregationField agg) {
        String function = getAggregationFunction(agg.getType());
        return String.format("%s(%s) as %s", function, agg.getField(), agg.getAlias());
    }

    /**
     * 获取聚合函数名
     */
    private static String getAggregationFunction(AggregationPageDTO.AggregationType type) {
        return switch (type) {
            case COUNT -> "COUNT";
            case SUM -> "SUM";
            case AVG -> "AVG";
            case MAX -> "MAX";
            case MIN -> "MIN";
            case COUNT_DISTINCT -> "COUNT(DISTINCT";
        };
    }

    /**
     * 构建聚合结果
     */
    private static Map<String, Object> buildAggregationResults(List<AggregationPageDTO.AggregationField> aggregations, List<?> records) {
        Map<String, Object> results = new HashMap<>();
        
        if (records != null && !records.isEmpty() && aggregations != null) {
            // 从第一条记录中提取聚合值
            Object firstRecord = records.get(0);
            for (AggregationPageDTO.AggregationField agg : aggregations) {
                try {
                    // 这里需要根据实际的记录结构来提取值
                    // 暂时使用模拟数据
                    if (agg.getType() == AggregationPageDTO.AggregationType.COUNT) {
                        results.put(agg.getAlias(), records.size());
                    } else {
                        results.put(agg.getAlias(), 0); // 模拟值
                    }
                } catch (Exception e) {
                    log.warn("无法提取聚合值: {}", agg.getAlias(), e);
                    results.put(agg.getAlias(), 0);
                }
            }
        }
        
        return results;
    }

    /**
     * 获取执行计划
     */
    private static String getExecutionPlan(QueryWrapper<?> wrapper) {
        // 这里应该执行 EXPLAIN 查询
        // 暂时返回模拟的执行计划
        return "EXPLAIN " + wrapper.getSqlSegment();
    }
} 