package com.indigo.databases.utils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.indigo.core.entity.dto.PageDTO;
import com.indigo.core.entity.dto.QueryDTO;
import com.indigo.core.entity.dto.page.*;
import com.indigo.core.entity.result.AggregationPageResult;
import com.indigo.core.entity.result.EnhancedPageResult;
import com.indigo.core.entity.result.PageResult;
import com.indigo.core.entity.result.PerformancePageResult;
import com.indigo.core.entity.vo.BaseVO;
import com.indigo.databases.mapper.EnhancedVoMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;

/**
 * 增强查询构建器
 * 基于 MyBatis-Plus 已有功能，提供便捷的查询方法
 * 支持单表查询、多表关联查询、聚合查询、性能监控等功能
 * 使用 IService 确保所有 MyBatis-Plus 功能正常工作
 * 
 * <h3>主要功能：</h3>
 * <ul>
 *   <li><strong>基础查询</strong>：分页、列表、单个查询，支持VO映射</li>
 *   <li><strong>多表关联</strong>：支持INNER、LEFT、RIGHT、FULL JOIN</li>
 *   <li><strong>聚合查询</strong>：COUNT、SUM、AVG、MAX、MIN等聚合函数</li>
 *   <li><strong>性能监控</strong>：查询时间、执行计划、性能评级</li>
 *   <li><strong>便捷方法</strong>：快速查询、统计查询、存在性查询</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 基础分页查询
 * PageResult<ProductVO> result = EnhancedQueryBuilder.pageWithCondition(
 *     productService, pageDTO, ProductVO.class);
 * 
 * // 多表关联查询
 * PageResult<ProductMultiTableVO> result = EnhancedQueryBuilder.pageWithJoin(
 *     productService, joinPageDTO, ProductMultiTableVO.class);
 * 
 * // 聚合查询
 * AggregationPageResult<ProductVO> result = EnhancedQueryBuilder.pageWithAggregation(
 *     productService, aggregationPageDTO, ProductVO.class);
 * 
 * // 性能监控查询
 * PerformancePageResult<ProductVO> result = EnhancedQueryBuilder.pageWithPerformance(
 *     productService, performancePageDTO, ProductVO.class);
 * 
 * // 便捷查询
 * PageResult<ProductVO> result = EnhancedQueryBuilder.quickPage(
 *     productService, pageDTO, ProductVO.class);
 * }</pre>
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
            
            return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
            
        } catch (Exception e) {
            log.error("基础分页查询失败", e);
            throw new RuntimeException("查询失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 基础分页查询 - 直接映射到VO
     * 通过字段选择避免内存转换，提升性能
     * 支持多表关联查询
     */
    public static <T, V extends BaseVO> PageResult<V> pageWithCondition(IService<T> service, PageDTO pageDTO, Class<V> voClass) {
        try {
            // 检查是否需要多表查询
            if (EnhancedVoFieldSelector.hasJoinQuery(voClass)) {
                return pageWithMultiTableQuery(service, pageDTO, voClass);
            } else {
                return pageWithSingleTableQuery(service, pageDTO, voClass);
            }
            
        } catch (Exception e) {
            log.error("VO分页查询失败", e);
            throw new RuntimeException("查询失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 单表查询 - 使用智能映射
     * 注意：MyBatis-Plus的IService.page()有泛型限制，无法直接返回Page<V>
     * 这里我们使用EnhancedVoMapper来实现真正的智能映射
     */
    private static <T, V extends BaseVO> PageResult<V> pageWithSingleTableQuery(IService<T> service, PageDTO pageDTO, Class<V> voClass) {
        // 构建基础查询条件
        QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(pageDTO);
        
        // 根据VO类自动选择字段 - 智能映射
        String[] selectFields = VoFieldSelector.getSelectFields(voClass);
        wrapper.select(selectFields);
        
        // 构建SQL查询
        String sql = wrapper.getSqlSegment();
        String fullSql = "SELECT " + String.join(", ", selectFields) + " FROM " + getTableName(service) + " WHERE " + sql;
        
        // 创建分页对象
        Page<V> page = new Page<>(pageDTO.getPageNo(), pageDTO.getPageSize());
        
        // 使用EnhancedVoMapper执行查询，实现智能映射
        @SuppressWarnings("unchecked")
        EnhancedVoMapper<T, V> mapper = (EnhancedVoMapper<T, V>) service.getBaseMapper();
        IPage<V> result = mapper.selectPageAsVo(page, fullSql);
        
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }
    
    /**
     * 多表查询
     */
    private static <T, V extends BaseVO> PageResult<V> pageWithMultiTableQuery(IService<T> service, PageDTO pageDTO, Class<V> voClass) {
        // 构建多表查询SQL
        String sql = MultiTableQueryBuilder.buildMultiTableSql(pageDTO, voClass);
        
        // 创建分页对象
        Page<V> page = new Page<>(pageDTO.getPageNo(), pageDTO.getPageSize());
        
        // 执行查询（需要强制转换为EnhancedVoMapper）
        @SuppressWarnings("unchecked")
        EnhancedVoMapper<T, V> mapper = (EnhancedVoMapper<T, V>) service.getBaseMapper();
        IPage<V> result = mapper.selectPageAsVo(page, sql);
        
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }
    
    /**
     * 列表查询 - 直接映射到VO
     * 通过字段选择避免内存转换，提升性能
     * 支持多表关联查询
     */
    public static <T, V extends BaseVO> List<V> listWithCondition(IService<T> service, QueryDTO queryDTO, Class<V> voClass) {
        try {
            // 检查是否需要多表查询
            if (EnhancedVoFieldSelector.hasJoinQuery(voClass)) {
                return listWithMultiTableQuery(service, queryDTO, voClass);
            } else {
                return listWithSingleTableQuery(service, queryDTO, voClass);
            }
            
        } catch (Exception e) {
            log.error("VO列表查询失败", e);
            throw new RuntimeException("查询失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 单表列表查询 - 使用智能映射
     */
    private static <T, V extends BaseVO> List<V> listWithSingleTableQuery(IService<T> service, QueryDTO queryDTO, Class<V> voClass) {
        // 构建基础查询条件
        QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(queryDTO);
        
        // 根据VO类自动选择字段 - 智能映射
        String[] selectFields = VoFieldSelector.getSelectFields(voClass);
        wrapper.select(selectFields);
        
        // 构建SQL查询
        String sql = wrapper.getSqlSegment();
        String fullSql = "SELECT " + String.join(", ", selectFields) + " FROM " + getTableName(service) + " WHERE " + sql;
        
        // 使用EnhancedVoMapper执行查询，实现智能映射
        @SuppressWarnings("unchecked")
        EnhancedVoMapper<T, V> mapper = (EnhancedVoMapper<T, V>) service.getBaseMapper();
        return mapper.selectListAsVo(fullSql);
    }
    
    /**
     * 多表列表查询
     */
    private static <T, V extends BaseVO> List<V> listWithMultiTableQuery(IService<T> service, QueryDTO queryDTO, Class<V> voClass) {
        // 构建多表查询SQL
        String sql = MultiTableQueryBuilder.buildMultiTableSql(queryDTO, voClass);
        
        // 执行查询
        @SuppressWarnings("unchecked")
        EnhancedVoMapper<T, V> mapper = (EnhancedVoMapper<T, V>) service.getBaseMapper();
        List<V> result = mapper.selectListAsVo(sql);
        
        return result;
    }
    
    /**
     * 单个查询 - 直接映射到VO
     * 通过字段选择避免内存转换，提升性能
     * 支持多表关联查询
     */
    public static <T, V extends BaseVO> V getOneWithCondition(IService<T> service, QueryDTO queryDTO, Class<V> voClass) {
        try {
            // 检查是否需要多表查询
            if (EnhancedVoFieldSelector.hasJoinQuery(voClass)) {
                return getOneWithMultiTableQuery(service, queryDTO, voClass);
            } else {
                return getOneWithSingleTableQuery(service, queryDTO, voClass);
            }
            
        } catch (Exception e) {
            log.error("VO单个查询失败", e);
            throw new RuntimeException("查询失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 单表单个查询 - 使用智能映射
     */
    private static <T, V extends BaseVO> V getOneWithSingleTableQuery(IService<T> service, QueryDTO queryDTO, Class<V> voClass) {
        // 构建基础查询条件
        QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(queryDTO);
        
        // 根据VO类自动选择字段 - 智能映射
        String[] selectFields = VoFieldSelector.getSelectFields(voClass);
        wrapper.select(selectFields);
        
        // 构建SQL查询
        String sql = wrapper.getSqlSegment();
        String fullSql = "SELECT " + String.join(", ", selectFields) + " FROM " + getTableName(service) + " WHERE " + sql + " LIMIT 1";
        
        // 使用EnhancedVoMapper执行查询，实现智能映射
        @SuppressWarnings("unchecked")
        EnhancedVoMapper<T, V> mapper = (EnhancedVoMapper<T, V>) service.getBaseMapper();
        return mapper.selectOneAsVo(fullSql);
    }
    
    /**
     * 多表单个查询
     */
    private static <T, V extends BaseVO> V getOneWithMultiTableQuery(IService<T> service, QueryDTO queryDTO, Class<V> voClass) {
        // 构建多表查询SQL
        String sql = MultiTableQueryBuilder.buildMultiTableSql(queryDTO, voClass);
        
        // 执行查询
        @SuppressWarnings("unchecked")
        EnhancedVoMapper<T, V> mapper = (EnhancedVoMapper<T, V>) service.getBaseMapper();
        V result = mapper.selectOneAsVo(sql);
        
        return result;
    }
    
    // ==================== 聚合查询 ====================
    
    /**
     * 聚合查询 - 支持VO映射
     * 使用 IService 的 list 方法
     */
    public static <T, V extends BaseVO> AggregationPageResult<V> pageWithAggregation(IService<T> service, AggregationPageDTO pageDTO, Class<V> voClass) {
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
                List<T> entities = service.list(wrapper);
                
                // 构建聚合结果 - 直接使用实体数据
                Map<String, Object> aggregations = buildAggregationResults(pageDTO.getAggregations(), entities);
                
                // 聚合查询返回单条记录，包含所有聚合值
                return AggregationPageResult.withAggregations(
                    List.of(), // 聚合查询通常不返回具体记录
                    (long) entities.size(),
                    1L,
                    (long) entities.size(),
                    aggregations
                );
            } else {
                // 如果没有聚合字段，执行普通分页查询 - 使用智能映射
                String[] selectFields = VoFieldSelector.getSelectFields(voClass);
                wrapper.select(selectFields);
                
                // 构建SQL查询
                String sql = wrapper.getSqlSegment();
                String fullSql = "SELECT " + String.join(", ", selectFields) + " FROM " + getTableName(service) + " WHERE " + sql;
                
                // 创建分页对象
                Page<V> page = new Page<>(pageDTO.getPageNo(), pageDTO.getPageSize());
                
                // 使用EnhancedVoMapper执行查询，实现智能映射
                @SuppressWarnings("unchecked")
                EnhancedVoMapper<T, V> mapper = (EnhancedVoMapper<T, V>) service.getBaseMapper();
                IPage<V> result = mapper.selectPageAsVo(page, fullSql);
                
                return new AggregationPageResult<>(result.getRecords(), result.getTotal(), 
                                                 result.getCurrent(), result.getSize());
            }
            
        } catch (Exception e) {
            log.error("聚合查询失败", e);
            throw new RuntimeException("聚合查询失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 分组查询 - 支持VO映射
     * 使用 IService 的 list 方法
     */
    public static <T, V extends BaseVO> AggregationPageResult<V> pageWithGroupBy(IService<T> service, AggregationPageDTO pageDTO, Class<V> voClass) {
        try {
            // 构建基础查询条件
            QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(pageDTO);
            
            // 构建聚合字段
            if (pageDTO.getAggregations() != null && !pageDTO.getAggregations().isEmpty()) {
                String[] selectFields = pageDTO.getAggregations().stream()
                    .map(EnhancedQueryBuilder::buildAggregationField)
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
            List<V> records = (List<V>) service.list(wrapper);
            
            // 分组查询返回多条记录，每条记录代表一个分组
            return new AggregationPageResult<>(records, (long) records.size(), 1L, (long) records.size());
            
        } catch (Exception e) {
            log.error("分组查询失败", e);
            throw new RuntimeException("分组查询失败: " + e.getMessage(), e);
        }
    }
    
    // ==================== 性能优化查询 ====================
    
    /**
     * 性能监控查询 - 支持VO映射
     * 返回查询执行时间和执行计划
     */
    public static <T, V extends BaseVO> PerformancePageResult<V> pageWithPerformance(IService<T> service, PerformancePageDTO pageDTO, Class<V> voClass) {
        try {
            long startTime = System.currentTimeMillis();
            
            // 构建基础查询条件
            QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(pageDTO);
            
            // 根据VO类自动选择字段
            String[] selectFields = VoFieldSelector.getSelectFields(voClass);
            wrapper.select(selectFields);
            
            // 执行查询 - 使用智能映射
            String sql = wrapper.getSqlSegment();
            String fullSql = "SELECT " + String.join(", ", selectFields) + " FROM " + getTableName(service) + " WHERE " + sql;
            
            // 创建分页对象
            Page<V> page = new Page<>(pageDTO.getPageNo(), pageDTO.getPageSize());
            
            // 使用EnhancedVoMapper执行查询，实现智能映射
            @SuppressWarnings("unchecked")
            EnhancedVoMapper<T, V> mapper = (EnhancedVoMapper<T, V>) service.getBaseMapper();
            IPage<V> result = mapper.selectPageAsVo(page, fullSql);
            
            long endTime = System.currentTimeMillis();
            long queryTime = endTime - startTime;
            
            // 获取执行计划
            String executionPlan = getExecutionPlan(wrapper);
            
            // 构建性能统计
            PerformancePageResult.PageStatistics statistics = buildStatistics(pageDTO, queryTime, executionPlan);
            
            PerformancePageResult<V> performanceResult = new PerformancePageResult<>(result.getRecords(), result.getTotal(), 
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
     * 字段选择查询 - 支持VO映射
     * 支持指定返回字段，提高查询性能
     */
    public static <T, V extends BaseVO> PerformancePageResult<V> pageWithSelectFields(IService<T> service, PerformancePageDTO pageDTO, Class<V> voClass) {
        try {
            // 构建基础查询条件
            QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(pageDTO);
            
            // 设置选择字段
            String[] selectFields;
            if (pageDTO.getSelectFields() != null && !pageDTO.getSelectFields().isEmpty()) {
                selectFields = pageDTO.getSelectFields().toArray(new String[0]);
                wrapper.select(selectFields);
            } else {
                // 使用VO字段选择器
                selectFields = VoFieldSelector.getSelectFields(voClass);
                wrapper.select(selectFields);
            }
            
            // 执行查询 - 使用智能映射
            String sql = wrapper.getSqlSegment();
            String fullSql = "SELECT " + String.join(", ", selectFields) + " FROM " + getTableName(service) + " WHERE " + sql;
            
            // 创建分页对象
            Page<V> page = new Page<>(pageDTO.getPageNo(), pageDTO.getPageSize());
            
            // 使用EnhancedVoMapper执行查询，实现智能映射
            @SuppressWarnings("unchecked")
            EnhancedVoMapper<T, V> mapper = (EnhancedVoMapper<T, V>) service.getBaseMapper();
            IPage<V> result = mapper.selectPageAsVo(page, fullSql);
            
            return new PerformancePageResult<>(result.getRecords(), result.getTotal(), 
                                             result.getCurrent(), result.getSize());
            
        } catch (Exception e) {
            log.error("字段选择查询失败", e);
            throw new RuntimeException("字段选择查询失败: " + e.getMessage(), e);
        }
    }
    
    // ==================== 多表关联查询 ====================
    
    /**
     * 多表关联查询 - 支持VO映射
     * 支持 INNER, LEFT, RIGHT, FULL JOIN
     * 
     * @deprecated 推荐使用 {@link #pageWithCondition(IService, PageDTO, Class)}
     *             基于@VoMapping注解的方式更简洁、维护性更好
     */
    @Deprecated(since = "1.0.0", forRemoval = true)
    public static <T, V extends BaseVO> PageResult<V> pageWithJoin(IService<T> service, JoinPageDTO pageDTO, Class<V> voClass) {
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
            
            // 执行查询 - 使用智能映射
            String[] selectFields = VoFieldSelector.getSelectFields(voClass);
            wrapper.select(selectFields);
            
            String sql = wrapper.getSqlSegment();
            String fullSql = "SELECT " + String.join(", ", selectFields) + " FROM " + getTableName(service) + " WHERE " + sql;
            
            // 创建分页对象
            Page<V> page = new Page<>(pageDTO.getPageNo(), pageDTO.getPageSize());
            
            // 使用EnhancedVoMapper执行查询，实现智能映射
            @SuppressWarnings("unchecked")
            EnhancedVoMapper<T, V> mapper = (EnhancedVoMapper<T, V>) service.getBaseMapper();
            IPage<V> result = mapper.selectPageAsVo(page, fullSql);
            
            return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
            
        } catch (Exception e) {
            log.error("多表关联查询失败", e);
            throw new RuntimeException("多表关联查询失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 复杂查询 - 支持VO映射
     * 支持自定义SQL查询
     */
    public static <T, V extends BaseVO> PageResult<V> pageWithComplexQuery(IService<T> service, ComplexPageDTO pageDTO, Class<V> voClass) {
        try {
            // 构建基础查询条件
            QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(pageDTO);
            
            // 添加自定义SQL
            if (pageDTO.getEnableComplexQuery() != null && pageDTO.getEnableComplexQuery() && pageDTO.getCustomSql() != null) {
                wrapper.apply(pageDTO.getCustomSql(), pageDTO.getCustomSqlParams());
            }
            
            // 执行查询 - 使用智能映射
            String[] selectFields = VoFieldSelector.getSelectFields(voClass);
            wrapper.select(selectFields);
            
            String sql = wrapper.getSqlSegment();
            String fullSql = "SELECT " + String.join(", ", selectFields) + " FROM " + getTableName(service) + " WHERE " + sql;
            
            // 创建分页对象
            Page<V> page = new Page<>(pageDTO.getPageNo(), pageDTO.getPageSize());
            
            // 使用EnhancedVoMapper执行查询，实现智能映射
            @SuppressWarnings("unchecked")
            EnhancedVoMapper<T, V> mapper = (EnhancedVoMapper<T, V>) service.getBaseMapper();
            IPage<V> result = mapper.selectPageAsVo(page, fullSql);
            
            return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
            
        } catch (Exception e) {
            log.error("复杂查询失败", e);
            throw new RuntimeException("复杂查询失败: " + e.getMessage(), e);
        }
    }
    
    // ==================== 增强查询（组合功能） ====================
    
    /**
     * 增强查询（组合聚合和性能优化）- 支持VO映射
     */
    public static <T, V extends BaseVO> EnhancedPageResult<V> pageWithEnhanced(IService<T> service, EnhancedPageDTO pageDTO, Class<V> voClass) {
        try {
            long startTime = System.currentTimeMillis();
            
            // 构建基础查询条件
            QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(pageDTO);
            
            // 设置选择字段
            if (pageDTO.getSelectFields() != null && !pageDTO.getSelectFields().isEmpty()) {
                wrapper.select(pageDTO.getSelectFields().toArray(new String[0]));
            } else {
                // 使用VO字段选择器
                String[] selectFields = VoFieldSelector.getSelectFields(voClass);
                wrapper.select(selectFields);
            }
            
            // 添加聚合字段
            if (pageDTO.getAggregations() != null && !pageDTO.getAggregations().isEmpty()) {
                String[] selectFields = pageDTO.getAggregations().stream()
                    .map(agg -> buildAggregationField(agg))
                    .toArray(String[]::new);
                wrapper.select(selectFields);
            }
            
            // 执行查询 - 使用智能映射
            String sql = wrapper.getSqlSegment();
            String fullSql = "SELECT * FROM " + getTableName(service) + " WHERE " + sql;
            
            // 创建分页对象
            Page<V> page = new Page<>(pageDTO.getPageNo(), pageDTO.getPageSize());
            
            // 使用EnhancedVoMapper执行查询，实现智能映射
            @SuppressWarnings("unchecked")
            EnhancedVoMapper<T, V> mapper = (EnhancedVoMapper<T, V>) service.getBaseMapper();
            IPage<V> result = mapper.selectPageAsVo(page, fullSql);
            
            long endTime = System.currentTimeMillis();
            long queryTime = endTime - startTime;
            
            // 获取执行计划
            String executionPlan = getExecutionPlan(wrapper);
            
            // 构建聚合结果
            Map<String, Object> aggregations = new HashMap<>();
            if (pageDTO.getAggregations() != null && !pageDTO.getAggregations().isEmpty()) {
                aggregations = buildAggregationResults(pageDTO.getAggregations(), result.getRecords());
            }
            
            EnhancedPageResult<V> enhancedResult = new EnhancedPageResult<>(result.getRecords(), result.getTotal(), 
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
    
    // ==================== 便捷查询方法 ====================
    
    /**
     * 快速分页查询 - 支持VO映射
     * 简化版分页查询，自动处理单表/多表查询
     */
    public static <T, V extends BaseVO> PageResult<V> quickPage(IService<T> service, PageDTO pageDTO, Class<V> voClass) {
        return pageWithCondition(service, pageDTO, voClass);
    }
    
    /**
     * 快速列表查询 - 支持VO映射
     * 简化版列表查询，自动处理单表/多表查询
     */
    public static <T, V extends BaseVO> List<V> quickList(IService<T> service, QueryDTO queryDTO, Class<V> voClass) {
        return listWithCondition(service, queryDTO, voClass);
    }
    
    /**
     * 快速单个查询 - 支持VO映射
     * 简化版单个查询，自动处理单表/多表查询
     */
    public static <T, V extends BaseVO> V quickGetOne(IService<T> service, QueryDTO queryDTO, Class<V> voClass) {
        return getOneWithCondition(service, queryDTO, voClass);
    }
    
    /**
     * 统计查询 - 支持VO映射
     * 返回符合条件的记录数量
     */
    public static <T, V extends BaseVO> Long countWithCondition(IService<T> service, QueryDTO queryDTO, Class<V> voClass) {
        try {
            // 构建基础查询条件
            QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(queryDTO);
            
            // 执行统计查询
            return service.count(wrapper);
            
        } catch (Exception e) {
            log.error("统计查询失败", e);
            throw new RuntimeException("统计查询失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 存在性查询 - 支持VO映射
     * 检查是否存在符合条件的记录
     */
    public static <T, V extends BaseVO> boolean existsWithCondition(IService<T> service, QueryDTO queryDTO, Class<V> voClass) {
        try {
            // 构建基础查询条件
            QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(queryDTO);
            
            // 执行存在性查询
            return service.count(wrapper) > 0;
            
        } catch (Exception e) {
            log.error("存在性查询失败", e);
            throw new RuntimeException("存在性查询失败: " + e.getMessage(), e);
        }
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 构建性能统计信息
     */
    private static PerformancePageResult.PageStatistics buildStatistics(PerformancePageDTO pageDTO, long queryTime, String executionPlan) {
        PerformancePageResult.PageStatistics statistics = new PerformancePageResult.PageStatistics();
        
        // 设置查询时间
        statistics.setQueryTime(queryTime);
        
        // 设置执行计划
        statistics.setExecutionPlan(executionPlan);
        
        // 设置查询类型
        statistics.setQueryType(pageDTO.getResultType());
        
        // 设置缓存状态
        statistics.setCacheEnabled(pageDTO.getUseCache());
        
        // 设置字段数量
        if (pageDTO.getSelectFields() != null) {
            statistics.setFieldCount(pageDTO.getSelectFields().size());
        }
        
        // 性能评级
        if (queryTime < 100) {
            statistics.setPerformanceGrade("优秀");
        } else if (queryTime < 500) {
            statistics.setPerformanceGrade("良好");
        } else if (queryTime < 1000) {
            statistics.setPerformanceGrade("一般");
        } else {
            statistics.setPerformanceGrade("需要优化");
        }
        
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
    
    /**
     * 获取表名
     */
    private static <T> String getTableName(IService<T> service) {
        // 这里应该从实体类注解中获取表名
        // 暂时返回一个默认值，实际应该通过反射获取
        return "t_" + service.getClass().getSimpleName().toLowerCase().replace("service", "");
    }
} 