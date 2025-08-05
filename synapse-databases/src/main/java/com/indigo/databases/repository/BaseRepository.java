package com.indigo.databases.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.indigo.databases.dto.*;
import com.indigo.databases.utils.EnhancedQueryBuilder;
import com.indigo.databases.utils.LambdaQueryBuilder;
import com.indigo.databases.utils.QueryConditionBuilder;

import java.util.List;

/**
 * 基础Repository接口
 * 继承MyBatis-Plus的IService，提供所有基础CRUD功能
 * 同时支持注解SQL查询和自动查询条件构建
 *
 * @author 史偕成
 * @date 2024/12/19
 */
public interface BaseRepository<T, M extends BaseMapper<T>> extends IService<T> {
    
    /**
     * 获取Mapper实例
     */
    M getMapper();
    
    // ==================== 自动查询条件构建方法 ====================
    
    /**
     * 分页查询 - 支持DTO查询条件（推荐使用）
     * 约定：所有分页请求参数都要继承 PageDTO
     */
    default PageResult<T> pageWithCondition(PageDTO queryDTO) {
        return EnhancedQueryBuilder.pageWithCondition(this, queryDTO);
    }
    
    /**
     * 列表查询 - 支持自动查询条件构建（实体类）
     */
    default List<T> listWithCondition(T queryEntity) {
        QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(queryEntity);
        return list(wrapper);
    }
    
    /**
     * 列表查询 - 支持DTO查询条件（推荐使用）
     */
    default <D> List<T> listWithDTO(D queryDTO) {
        @SuppressWarnings("unchecked")
        QueryWrapper<T> wrapper = (QueryWrapper<T>) QueryConditionBuilder.buildQueryWrapper(queryDTO);
        return list(wrapper);
    }
    
    /**
     * 单个查询 - 支持自动查询条件构建（实体类）
     */
    default T getOneWithCondition(T queryEntity) {
        QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(queryEntity);
        return getOne(wrapper);
    }
    
    /**
     * 单个查询 - 支持DTO查询条件（推荐使用）
     */
    default <D> T getOneWithDTO(D queryDTO) {
        @SuppressWarnings("unchecked")
        QueryWrapper<T> wrapper = (QueryWrapper<T>) QueryConditionBuilder.buildQueryWrapper(queryDTO);
        return getOne(wrapper);
    }
    
    /**
     * 统计查询 - 支持自动查询条件构建（实体类）
     */
    default long countWithCondition(T queryEntity) {
        QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(queryEntity);
        return count(wrapper);
    }
    
    /**
     * 统计查询 - 支持DTO查询条件（推荐使用）
     */
    default <D> long countWithDTO(D queryDTO) {
        @SuppressWarnings("unchecked")
        QueryWrapper<T> wrapper = (QueryWrapper<T>) QueryConditionBuilder.buildQueryWrapper(queryDTO);
        return count(wrapper);
    }
    
    // ==================== 增强查询方法 ====================
    
    /**
     * 聚合统计查询
     * 支持 COUNT, SUM, AVG, MAX, MIN 等聚合函数
     */
    default AggregationPageResult<T> pageWithAggregation(AggregationPageDTO queryDTO) {
        return EnhancedQueryBuilder.pageWithAggregation(this, queryDTO);
    }
    
    /**
     * 分组统计查询
     * 支持 GROUP BY 分组统计
     */
    default AggregationPageResult<T> pageWithGroupBy(AggregationPageDTO queryDTO) {
        return EnhancedQueryBuilder.pageWithGroupBy(this, queryDTO);
    }
    
    /**
     * 性能监控查询
     * 返回查询执行时间和执行计划
     */
    default PerformancePageResult<T> pageWithPerformance(PerformancePageDTO queryDTO) {
        return EnhancedQueryBuilder.pageWithPerformance(this, queryDTO);
    }
    
    /**
     * 缓存查询
     * 支持查询结果缓存
     */
    default PerformancePageResult<T> pageWithCache(PerformancePageDTO queryDTO) {
        return EnhancedQueryBuilder.pageWithCache(this, queryDTO);
    }
    
    /**
     * 字段选择查询
     * 支持指定返回字段，提高查询性能
     */
    default PerformancePageResult<T> pageWithSelectFields(PerformancePageDTO queryDTO) {
        return EnhancedQueryBuilder.pageWithSelectFields(this, queryDTO);
    }
    
    /**
     * 多表关联查询
     * 支持 INNER, LEFT, RIGHT, FULL JOIN
     */
    default PageResult<T> pageWithJoin(JoinPageDTO queryDTO) {
        return EnhancedQueryBuilder.pageWithJoin(this, queryDTO);
    }
    
    /**
     * 复杂查询
     * 支持自定义SQL查询
     */
    default PageResult<T> pageWithComplexQuery(ComplexPageDTO queryDTO) {
        return EnhancedQueryBuilder.pageWithComplexQuery(this, queryDTO);
    }
    
    /**
     * 增强查询（组合聚合和性能优化）
     */
    default EnhancedPageResult<T> pageWithEnhanced(EnhancedPageDTO queryDTO) {
        return EnhancedQueryBuilder.pageWithEnhanced(this, queryDTO);
    }
} 