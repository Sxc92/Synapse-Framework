package com.indigo.databases.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.indigo.core.entity.dto.PageDTO;
import com.indigo.core.entity.dto.QueryDTO;
import com.indigo.core.entity.dto.page.*;
import com.indigo.core.entity.result.AggregationPageResult;
import com.indigo.core.entity.result.EnhancedPageResult;
import com.indigo.core.entity.result.PageResult;
import com.indigo.core.entity.result.PerformancePageResult;
import com.indigo.core.entity.vo.BaseVO;
import com.indigo.databases.utils.EnhancedQueryBuilder;
import com.indigo.databases.utils.QueryConditionBuilder;

import java.util.List;

/**
 * 基础Repository接口
 * 继承MyBatis-Plus的IService，提供所有基础CRUD功能
 * 同时支持注解SQL查询和自动查询条件构建
 * 
 * <h3>主要功能：</h3>
 * <ul>
 *   <li><strong>基础CRUD</strong>：继承MyBatis-Plus的IService所有功能</li>
 *   <li><strong>自动查询条件</strong>：支持@QueryCondition注解自动构建查询条件</li>
 *   <li><strong>VO映射</strong>：所有查询方法都支持直接映射到VO对象</li>
 *   <li><strong>多表关联</strong>：支持复杂的多表关联查询</li>
 *   <li><strong>聚合查询</strong>：支持COUNT、SUM、AVG等聚合函数</li>
 *   <li><strong>性能监控</strong>：提供查询性能监控和优化建议</li>
 *   <li><strong>便捷方法</strong>：提供快速查询、统计查询等便捷方法</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @AutoRepository
 * public interface ProductRepository extends BaseRepository<Product, ProductMapper> {
 *     
 *     // 基础分页查询 - 支持@VoMapping注解的多表关联
 *     PageResult<ProductMultiTableVO> pageProducts(ProductPageQueryDTO queryDTO);
 *     
 *     // 多表关联查询 - 基于@VoMapping注解（推荐）
 *     PageResult<ProductMultiTableVO> pageProductsWithBrand(ProductPageQueryDTO queryDTO);
 *     
 *     // 多表关联查询 - 基于JoinPageDTO配置（已过时）
 *     @Deprecated(since = "1.0.0", forRemoval = true)
 *     PageResult<ProductMultiTableVO> pageProductsWithJoin(JoinPageDTO joinPageDTO);
 *     
 *     // 聚合查询
 *     AggregationPageResult<ProductVO> getProductStatistics(AggregationPageDTO aggregationPageDTO);
 *     
 *     // 便捷查询 - 支持@VoMapping注解的多表关联
 *     PageResult<ProductMultiTableVO> quickPageProducts(ProductPageQueryDTO queryDTO);
 * }
 * }</pre>
 *
 * @author 史偕成
 * @date 2025/12/19
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
     * 分页查询 - 返回VO对象（推荐使用）
     * 约定：所有分页请求参数都要继承 PageDTO
     * 通过智能字段选择直接映射到VO，避免内存转换
     */
    default <V extends BaseVO> PageResult<V> pageWithCondition(PageDTO queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.pageWithCondition(this, queryDTO, voClass);
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
    default List<T> listWithDTO(QueryDTO queryDTO) {
        QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(queryDTO);
        return list(wrapper);
    }
    
    /**
     * 列表查询 - 返回VO对象（推荐使用）
     */
    default <V extends BaseVO> List<V> listWithDTO(QueryDTO queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.listWithCondition(this, queryDTO, voClass);
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
    default T getOneWithDTO(QueryDTO queryDTO) {
        QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(queryDTO);
        return getOne(wrapper);
    }
    
    /**
     * 单个查询 - 返回VO对象（推荐使用）
     */
    default <V extends BaseVO> V getOneWithDTO(QueryDTO queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.getOneWithCondition(this, queryDTO, voClass);
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
    default long countWithDTO(QueryDTO queryDTO) {
        QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(queryDTO);
        return count(wrapper);
    }
    
    // ==================== 增强查询方法 ====================
    
    /**
     * 聚合统计查询 - 支持VO映射
     * 支持 COUNT, SUM, AVG, MAX, MIN 等聚合函数
     */
    default <V extends BaseVO> AggregationPageResult<V> pageWithAggregation(AggregationPageDTO queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.pageWithAggregation(this, queryDTO, voClass);
    }
    
    /**
     * 分组统计查询 - 支持VO映射
     * 支持 GROUP BY 分组统计
     */
    default <V extends BaseVO> AggregationPageResult<V> pageWithGroupBy(AggregationPageDTO queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.pageWithGroupBy(this, queryDTO, voClass);
    }
    
    /**
     * 性能监控查询 - 支持VO映射
     * 返回查询执行时间和执行计划
     */
    default <V extends BaseVO> PerformancePageResult<V> pageWithPerformance(PerformancePageDTO queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.pageWithPerformance(this, queryDTO, voClass);
    }
    
    /**
     * 字段选择查询 - 支持VO映射
     * 支持指定返回字段，提高查询性能
     */
    default <V extends BaseVO> PerformancePageResult<V> pageWithSelectFields(PerformancePageDTO queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.pageWithSelectFields(this, queryDTO, voClass);
    }
    
    /**
     * 多表关联查询 - 支持VO映射
     * 支持 INNER, LEFT, RIGHT, FULL JOIN
     * 
     * @deprecated 推荐使用 {@link #pageWithVoMapping(PageDTO, Class)} 或 {@link #pageWithCondition(PageDTO, Class)}
     *             基于@VoMapping注解的方式更简洁、维护性更好
     */
//    @Deprecated(since = "1.0.0", forRemoval = true)
//    default <V extends BaseVO> PageResult<V> pageWithJoin(JoinPageDTO queryDTO, Class<V> voClass) {
//        return EnhancedQueryBuilder.pageWithJoin(this, queryDTO, voClass);
//    }
    
    /**
     * 多表关联查询 - 基于@VoMapping注解
     * 自动根据VO类的@VoMapping注解配置进行多表关联查询
     * 这是推荐的多表查询方式
     */
    default <V extends BaseVO> PageResult<V> pageWithVoMapping(PageDTO queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.pageWithCondition(this, queryDTO, voClass);
    }
    
    /**
     * 复杂查询 - 支持VO映射
     * 支持自定义SQL查询
     */
    default <V extends BaseVO> PageResult<V> pageWithComplexQuery(ComplexPageDTO queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.pageWithComplexQuery(this, queryDTO, voClass);
    }
    
    /**
     * 增强查询（组合聚合和性能优化）- 支持VO映射
     */
    default <V extends BaseVO> EnhancedPageResult<V> pageWithEnhanced(EnhancedPageDTO queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.pageWithEnhanced(this, queryDTO, voClass);
    }
    
    // ==================== 便捷查询方法 ====================
    
    /**
     * 快速分页查询 - 支持VO映射
     * 简化版分页查询，自动处理单表/多表查询
     * 支持@VoMapping注解配置的多表关联查询
     */
    default <V extends BaseVO> PageResult<V> quickPage(PageDTO pageDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.quickPage(this, pageDTO, voClass);
    }
    
    /**
     * 快速列表查询 - 支持VO映射
     * 简化版列表查询，自动处理单表/多表查询
     * 支持@VoMapping注解配置的多表关联查询
     */
    default <V extends BaseVO> List<V> quickList(QueryDTO queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.quickList(this, queryDTO, voClass);
    }
    
    /**
     * 快速单个查询 - 支持VO映射
     * 简化版单个查询，自动处理单表/多表查询
     * 支持@VoMapping注解配置的多表关联查询
     */
    default <V extends BaseVO> V quickGetOne(QueryDTO queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.quickGetOne(this, queryDTO, voClass);
    }
    
    /**
     * 统计查询 - 支持VO映射
     * 返回符合条件的记录数量
     */
    default <V extends BaseVO> Long countWithCondition(QueryDTO queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.countWithCondition(this, queryDTO, voClass);
    }
    
    /**
     * 存在性查询 - 支持VO映射
     * 检查是否存在符合条件的记录
     */
    default <V extends BaseVO> boolean existsWithCondition(QueryDTO queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.existsWithCondition(this, queryDTO, voClass);
    }
} 