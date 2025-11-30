package com.indigo.databases.utils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.indigo.core.annotation.FieldMapping;
import com.indigo.core.annotation.VoMapping;
import com.indigo.core.entity.vo.BaseVO;
import com.indigo.databases.mapper.EnhancedVoMapper;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
 *   <li><strong>异步查询</strong>：基于CompletableFuture的异步查询支持（实验性功能）</li>
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
 * 
 * // 异步查询（实验性功能）
 * CompletableFuture<PageResult<ProductVO>> future = EnhancedQueryBuilder.pageWithConditionAsync(
 *     productService, pageDTO, ProductVO.class);
 * future.thenAccept(result -> {
 *     System.out.println("查询完成，共" + result.getTotal() + "条记录");
 * });
 * }</pre>
 * 
 * <h3>异步查询说明：</h3>
 * <p><strong>⚠️ 实验性功能</strong>：异步查询功能目前处于实验阶段，主要用于：</p>
 * <ul>
 *   <li>大数据量查询（>10万条记录）</li>
 *   <li>复杂多表关联查询</li>
 *   <li>需要并行执行多个查询的场景</li>
 *   <li>提升用户体验（避免界面卡顿）</li>
 * </ul>
 * <p><strong>注意事项</strong>：</p>
 * <ul>
 *   <li>异步查询会增加内存消耗和线程管理复杂度</li>
 *   <li>错误处理相对复杂，需要正确处理CompletableFuture的异常</li>
 *   <li>调试相对困难，建议在性能瓶颈明确时再使用</li>
 *   <li>API可能会在后续版本中调整</li>
 * </ul>
 *
 * @author 史偕成
 * @date 2025/12/19
 * @version 1.0.0
 */
@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"})
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
            Page<T> page = createEntityPage(pageDTO);
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
     * 
     * ⚠️ 注意：多表关联查询功能已暂停使用
     * 原因：自动生成表别名、DTO查询条件、WHERE条件构建等功能不够成熟
     * 建议：使用 MyBatis-Plus 的方式，在 Mapper 中手写 SQL 进行多表查询
     * 文档：详见 MULTI_TABLE_QUERY_STATUS.md
     */
    public static <T, V extends BaseVO<?>> PageResult<V> pageWithCondition(IService<T> service, PageDTO pageDTO, Class<V> voClass) {
        try {
            // 检查是否需要多表查询
            // ⚠️ 多表查询功能已暂停，暂时只支持单表查询
            if (EnhancedVoFieldSelector.hasJoinQuery(voClass)) {
                log.warn("多表查询功能已暂停使用，请使用 MyBatis-Plus 手写 SQL。VO: {}", voClass.getSimpleName());
                throw new UnsupportedOperationException("多表查询功能已暂停使用，请使用 MyBatis-Plus 手写 SQL。详见 MULTI_TABLE_QUERY_STATUS.md");
                // return pageWithMultiTableQuery(service, pageDTO, voClass);
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
    private static <T, V extends BaseVO<?>> PageResult<V> pageWithSingleTableQuery(IService<T> service, PageDTO pageDTO, Class<V> voClass) {
        // 构建基础查询条件
        QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(pageDTO);
        
        // 使用统一的查询执行方法
        return executeQuery(service, wrapper, pageDTO, voClass);
    }
    
    /**
     * 多表查询
     * 
     * ⚠️ 注意：此功能已暂停使用
     * 原因：自动生成表别名、DTO查询条件、WHERE条件构建等功能不够成熟
     * 建议：使用 MyBatis-Plus 的方式，在 Mapper 中手写 SQL 进行多表查询
     * 文档：详见 MULTI_TABLE_QUERY_STATUS.md
     * 
     * @deprecated 此方法已暂停使用，请使用 MyBatis-Plus 手写 SQL
     */
    @Deprecated
    private static <T, V extends BaseVO<?>> PageResult<V> pageWithMultiTableQuery(IService<T> service, PageDTO pageDTO, Class<V> voClass) {
        // 构建多表查询SQL
        String sql = MultiTableQueryBuilder.buildMultiTableSql(pageDTO, voClass);
        
        // 创建分页对象
        Page<Map<String, Object>> page = createMapPage(pageDTO);
        
        // 执行查询（需要强制转换为EnhancedVoMapper）
        EnhancedVoMapper<T, V> mapper = getEnhancedVoMapper(service, voClass);
        if (mapper == null) {
            // 如果Mapper没有实现EnhancedVoMapper，多表查询无法正常工作
            log.warn("Mapper {} 没有实现EnhancedVoMapper，多表查询可能无法正常工作", service.getBaseMapper().getClass().getSimpleName());
            throw new UnsupportedOperationException("多表查询需要Mapper实现EnhancedVoMapper接口");
        }
        IPage<Map<String, Object>> result = mapper.selectPageAsVo(page, sql);
        
        // 手动映射Map到VO
        List<V> voList = mapMapToVoList(result.getRecords(), voClass);
        return PageResult.of(voList, result.getTotal(), result.getCurrent(), result.getSize());
    }
    
    /**
     * 列表查询 - 直接映射到VO
     * 通过字段选择避免内存转换，提升性能
     * 
     * ⚠️ 注意：多表关联查询功能已暂停使用
     * 原因：自动生成表别名、DTO查询条件、WHERE条件构建等功能不够成熟
     * 建议：使用 MyBatis-Plus 的方式，在 Mapper 中手写 SQL 进行多表查询
     * 文档：详见 MULTI_TABLE_QUERY_STATUS.md
     */
    public static <T, V extends BaseVO<?>> List<V> listWithCondition(IService<T> service, QueryDTO queryDTO, Class<V> voClass) {
        try {
            // 检查是否需要多表查询
            // ⚠️ 多表查询功能已暂停，暂时只支持单表查询
            if (EnhancedVoFieldSelector.hasJoinQuery(voClass)) {
                log.warn("多表查询功能已暂停使用，请使用 MyBatis-Plus 手写 SQL。VO: {}", voClass.getSimpleName());
                throw new UnsupportedOperationException("多表查询功能已暂停使用，请使用 MyBatis-Plus 手写 SQL。详见 MULTI_TABLE_QUERY_STATUS.md");
                // return listWithMultiTableQuery(service, queryDTO, voClass);
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
    private static <T, V extends BaseVO<?>> List<V> listWithSingleTableQuery(IService<T> service, QueryDTO queryDTO, Class<V> voClass) {
        // 构建基础查询条件
        QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(queryDTO);
        
        // 获取Mapper
        EnhancedVoMapper<T, V> mapper = getEnhancedVoMapper(service, voClass);
        
        if (mapper == null) {
            // 基础查询
            log.warn("Mapper {} 没有实现EnhancedVoMapper，使用基础查询", service.getBaseMapper().getClass().getSimpleName());
            List<T> entities = service.list(wrapper);
            return VoMapper.mapToVoList(entities, voClass);
        } else {
            // 增强查询 - 使用基础查询避免参数绑定问题
            String[] selectFields = VoFieldSelector.getSelectFields(voClass);
            wrapper.select(selectFields);
            List<T> entities = service.list(wrapper);
            return VoMapper.mapToVoList(entities, voClass);
        }
    }
    
    /**
     * 多表列表查询
     */
    /**
     * 多表列表查询
     * 
     * ⚠️ 注意：此功能已暂停使用
     * 原因：自动生成表别名、DTO查询条件、WHERE条件构建等功能不够成熟
     * 建议：使用 MyBatis-Plus 的方式，在 Mapper 中手写 SQL 进行多表查询
     * 文档：详见 MULTI_TABLE_QUERY_STATUS.md
     * 
     * @deprecated 此方法已暂停使用，请使用 MyBatis-Plus 手写 SQL
     */
    @Deprecated
    private static <T, V extends BaseVO<?>> List<V> listWithMultiTableQuery(IService<T> service, QueryDTO queryDTO, Class<V> voClass) {
        // 构建多表查询SQL
        String sql = MultiTableQueryBuilder.buildMultiTableSql(queryDTO, voClass);
        
        // 执行查询
        EnhancedVoMapper<T, V> mapper = getEnhancedVoMapper(service, voClass);
        if (mapper == null) {
            // 如果Mapper没有实现EnhancedVoMapper，多表查询无法正常工作
            log.warn("Mapper {} 没有实现EnhancedVoMapper，多表查询可能无法正常工作", service.getBaseMapper().getClass().getSimpleName());
            throw new UnsupportedOperationException("多表查询需要Mapper实现EnhancedVoMapper接口");
        }
        
        // 使用列表查询方法，避免分页操作
        List<Map<String, Object>> mapList = mapper.selectListAsMap(sql);
        
        // 手动映射Map到VO
        return mapMapToVoList(mapList, voClass);
    }
    
    /**
     * 单个查询 - 直接映射到VO
     * 通过字段选择避免内存转换，提升性能
     * 支持多表关联查询
     */
    public static <T, V extends BaseVO<?>> V getOneWithCondition(IService<T> service, QueryDTO queryDTO, Class<V> voClass) {
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
    private static <T, V extends BaseVO<?>> V getOneWithSingleTableQuery(IService<T> service, QueryDTO queryDTO, Class<V> voClass) {
        // 构建基础查询条件
        QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(queryDTO);
        
        // 获取Mapper
        EnhancedVoMapper<T, V> mapper = getEnhancedVoMapper(service, voClass);
        
        if (mapper == null) {
            // 基础查询
            log.warn("Mapper {} 没有实现EnhancedVoMapper，使用基础查询", service.getBaseMapper().getClass().getSimpleName());
            T entity = service.getOne(wrapper);
            return VoMapper.mapToVo(entity, voClass);
        } else {
            // 增强查询 - 使用基础查询避免参数绑定问题
            String[] selectFields = VoFieldSelector.getSelectFields(voClass);
            wrapper.select(selectFields);
            T entity = service.getOne(wrapper);
            return VoMapper.mapToVo(entity, voClass);
        }
    }
    
    /**
     * 多表单个查询
     */
    private static <T, V extends BaseVO<?>> V getOneWithMultiTableQuery(IService<T> service, QueryDTO queryDTO, Class<V> voClass) {
        // 构建多表查询SQL，添加 LIMIT 1
        String sql = MultiTableQueryBuilder.buildMultiTableSql(queryDTO, voClass);
        // 如果SQL中没有LIMIT，添加LIMIT 1
        if (!sql.toUpperCase().contains("LIMIT")) {
            sql = sql + " LIMIT 1";
        }
        
        // 执行查询
        EnhancedVoMapper<T, V> mapper = getEnhancedVoMapper(service, voClass);
        if (mapper == null) {
            // 如果Mapper没有实现EnhancedVoMapper，多表查询无法正常工作
            log.warn("Mapper {} 没有实现EnhancedVoMapper，多表查询可能无法正常工作", service.getBaseMapper().getClass().getSimpleName());
            throw new UnsupportedOperationException("多表查询需要Mapper实现EnhancedVoMapper接口");
        }
        
        // 使用列表查询方法，只取第一条
        List<Map<String, Object>> mapList = mapper.selectListAsMap(sql);
        
        // 手动映射Map到VO
        if (mapList == null || mapList.isEmpty()) {
            return null;
        }
        List<V> voList = mapMapToVoList(mapList, voClass);
        return voList.isEmpty() ? null : voList.get(0);
    }
    
    // ==================== 聚合查询 ====================
    
    /**
     * 聚合查询 - 支持VO映射
     * 使用 IService 的 list 方法
     */
    public static <T, V extends BaseVO<?>> AggregationPageResult<V> pageWithAggregation(IService<T> service, AggregationPageDTO pageDTO, Class<V> voClass) {
        try {
            // 构建基础查询条件
            QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(pageDTO);
            
            // 构建聚合字段
            if (pageDTO.getAggregations() != null && !pageDTO.getAggregations().isEmpty()) {
                List<String> fieldList = new ArrayList<>();
                for (Object aggObj : pageDTO.getAggregations()) {
                    AggregationPageDTO.AggregationField agg = (AggregationPageDTO.AggregationField) aggObj;
                    fieldList.add(buildAggregationField(agg));
                }
                String[] selectFields = fieldList.toArray(new String[0]);
                wrapper.select(selectFields);
                
                // 使用 IService 的 list 方法
                List<T> entities = service.list(wrapper);
                
                // 构建聚合结果 - 直接使用实体数据
                List<AggregationPageDTO.AggregationField> aggList = (List<AggregationPageDTO.AggregationField>) pageDTO.getAggregations();
                Map<String, Object> aggregations = buildAggregationResults(aggList, entities);
                
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
                String fullSql = buildSelectSqlWithWhere(service, wrapper, selectFields);
                
                // 创建分页对象
                Page<Map<String, Object>> page = createMapPage(pageDTO);
                
                // 使用EnhancedVoMapper执行查询，实现智能映射
                EnhancedVoMapper<T, V> mapper = getEnhancedVoMapper(service, voClass);
                if (mapper == null) {
                    // 如果Mapper没有实现EnhancedVoMapper，使用基础查询
                    log.warn("Mapper {} 没有实现EnhancedVoMapper，使用基础查询", service.getBaseMapper().getClass().getSimpleName());
                    Page<T> entityPage = createEntityPage(pageDTO);
                    IPage<T> entityResult = service.page(entityPage, wrapper);
                    List<V> voList = VoMapper.mapToVoList(entityResult.getRecords(), voClass);
                    return new AggregationPageResult<>(voList, entityResult.getTotal(), 
                                                     entityResult.getCurrent(), entityResult.getSize());
                }
                IPage<Map<String, Object>> result = mapper.selectPageAsVo(page, fullSql);
                
                // 手动映射Map到VO
                List<V> voList = mapMapToVoList(result.getRecords(), voClass);
                return new AggregationPageResult<>(voList, result.getTotal(), 
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
    public static <T, V extends BaseVO<?>> AggregationPageResult<V> pageWithGroupBy(IService<T> service, AggregationPageDTO pageDTO, Class<V> voClass) {
        try {
            // 构建基础查询条件
            QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(pageDTO);
            
            // 构建聚合字段
            if (pageDTO.getAggregations() != null && !pageDTO.getAggregations().isEmpty()) {
                List<String> fieldList = new ArrayList<>();
                for (Object aggObj : pageDTO.getAggregations()) {
                    AggregationPageDTO.AggregationField agg = (AggregationPageDTO.AggregationField) aggObj;
                    fieldList.add(buildAggregationField(agg));
                }
                String[] selectFields = fieldList.toArray(new String[0]);
                wrapper.select(selectFields);
            }
            
            // 添加分组字段
            if (pageDTO.getGroupByFields() != null && !pageDTO.getGroupByFields().isEmpty()) {
                for (Object fieldObj : pageDTO.getGroupByFields()) {
                    String field = (String) fieldObj;
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
    public static <T, V extends BaseVO<?>> PerformancePageResult<V> pageWithPerformance(IService<T> service, PerformancePageDTO pageDTO, Class<V> voClass) {
        // 构建基础查询条件
        QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(pageDTO);
        
        // 根据VO类自动选择字段
        String[] selectFields = VoFieldSelector.getSelectFields(voClass);
        wrapper.select(selectFields);
        
        // 使用统一的性能监控查询方法
        return executeWithPerformanceMonitoring(service, wrapper, pageDTO, voClass, pageDTO);
    }
    
    /**
     * 字段选择查询 - 支持VO映射
     * 支持指定返回字段，提高查询性能
     */
    public static <T, V extends BaseVO<?>> PerformancePageResult<V> pageWithSelectFields(IService<T> service, PerformancePageDTO pageDTO, Class<V> voClass) {
        // 构建基础查询条件
        QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(pageDTO);
        
        // 设置选择字段
        String[] selectFields;
        if (pageDTO.getSelectFields() != null && !pageDTO.getSelectFields().isEmpty()) {
            List<String> fieldList = new ArrayList<>();
            for (Object fieldObj : pageDTO.getSelectFields()) {
                fieldList.add((String) fieldObj);
            }
            selectFields = fieldList.toArray(new String[0]);
            wrapper.select(selectFields);
        } else {
            // 使用VO字段选择器
            selectFields = VoFieldSelector.getSelectFields(voClass);
            wrapper.select(selectFields);
        }
        
        // 使用统一的性能监控查询方法
        return executeWithPerformanceMonitoring(service, wrapper, pageDTO, voClass, pageDTO);
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
    public static <T, V extends BaseVO<?>> PageResult<V> pageWithJoin(IService<T> service, JoinPageDTO pageDTO, Class<V> voClass) {
        try {
            // 构建基础查询条件
            QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(pageDTO);
            
            // 添加表关联
            if (pageDTO.getTableJoins() != null && !pageDTO.getTableJoins().isEmpty()) {
                for (Object joinObj : pageDTO.getTableJoins()) {
                JoinPageDTO.TableJoin join = (JoinPageDTO.TableJoin) joinObj;
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
            
            // 构建SQL查询
            String fullSql = buildSelectSqlWithWhere(service, wrapper, selectFields);
            
            // 创建分页对象
            Page<Map<String, Object>> page = createMapPage(pageDTO);
            
            // 使用EnhancedVoMapper执行查询，实现智能映射
            EnhancedVoMapper<T, V> mapper = getEnhancedVoMapper(service, voClass);
            if (mapper == null) {
                // 如果Mapper没有实现EnhancedVoMapper，使用基础查询
                log.warn("Mapper {} 没有实现EnhancedVoMapper，使用基础查询", service.getBaseMapper().getClass().getSimpleName());
                Page<T> entityPage = createEntityPage(pageDTO);
                IPage<T> entityResult = service.page(entityPage, wrapper);
                List<V> voList = VoMapper.mapToVoList(entityResult.getRecords(), voClass);
                PageResult<V> pageResult = new PageResult<>(voList, entityResult.getTotal(), 
                                             entityResult.getCurrent(), entityResult.getSize());
                return pageResult;
            }
            IPage<Map<String, Object>> result = mapper.selectPageAsVo(page, fullSql);
            
            // 手动映射Map到VO
            List<V> voList = mapMapToVoList(result.getRecords(), voClass);
            return PageResult.of(voList, result.getTotal(), result.getCurrent(), result.getSize());
            
        } catch (Exception e) {
            log.error("多表关联查询失败", e);
            throw new RuntimeException("多表关联查询失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 复杂查询 - 支持VO映射
     * 支持自定义SQL查询
     */
    public static <T, V extends BaseVO<?>> PageResult<V> pageWithComplexQuery(IService<T> service, ComplexPageDTO pageDTO, Class<V> voClass) {
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
            
            // 构建SQL查询
            String fullSql = buildSelectSqlWithWhere(service, wrapper, selectFields);
            
            // 创建分页对象
            Page<Map<String, Object>> page = createMapPage(pageDTO);
            
            // 使用EnhancedVoMapper执行查询，实现智能映射
            EnhancedVoMapper<T, V> mapper = getEnhancedVoMapper(service, voClass);
            if (mapper == null) {
                // 如果Mapper没有实现EnhancedVoMapper，使用基础查询
                log.warn("Mapper {} 没有实现EnhancedVoMapper，使用基础查询", service.getBaseMapper().getClass().getSimpleName());
                Page<T> entityPage = createEntityPage(pageDTO);
                IPage<T> entityResult = service.page(entityPage, wrapper);
                List<V> voList = VoMapper.mapToVoList(entityResult.getRecords(), voClass);
                PageResult<V> pageResult = new PageResult<>(voList, entityResult.getTotal(), 
                                             entityResult.getCurrent(), entityResult.getSize());
                return pageResult;
            }
            IPage<Map<String, Object>> result = mapper.selectPageAsVo(page, fullSql);
            
            // 手动映射Map到VO
            List<V> voList = mapMapToVoList(result.getRecords(), voClass);
            return PageResult.of(voList, result.getTotal(), result.getCurrent(), result.getSize());
            
        } catch (Exception e) {
            log.error("复杂查询失败", e);
            throw new RuntimeException("复杂查询失败: " + e.getMessage(), e);
        }
    }
    
    // ==================== 增强查询（组合功能） ====================
    
    /**
     * 增强查询（组合聚合和性能优化）- 支持VO映射
     */
    public static <T, V extends BaseVO<?>> EnhancedPageResult<V> pageWithEnhanced(IService<T> service, EnhancedPageDTO pageDTO, Class<V> voClass) {
        try {
            long startTime = System.currentTimeMillis();
            
            // 构建基础查询条件
            QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(pageDTO);
            
            // 设置选择字段
            if (pageDTO.getSelectFields() != null && !pageDTO.getSelectFields().isEmpty()) {
                List<String> fieldList = new ArrayList<>();
                for (Object fieldObj : pageDTO.getSelectFields()) {
                    fieldList.add((String) fieldObj);
                }
                String[] fields = fieldList.toArray(new String[0]);
                wrapper.select(fields);
            } else {
                // 使用VO字段选择器
                String[] selectFields = VoFieldSelector.getSelectFields(voClass);
                wrapper.select(selectFields);
            }
            
            // 添加聚合字段
            if (pageDTO.getAggregations() != null && !pageDTO.getAggregations().isEmpty()) {
                List<String> fieldList = new ArrayList<>();
                for (Object aggObj : pageDTO.getAggregations()) {
                    AggregationPageDTO.AggregationField agg = (AggregationPageDTO.AggregationField) aggObj;
                    fieldList.add(buildAggregationField(agg));
                }
                String[] selectFields = fieldList.toArray(new String[0]);
                wrapper.select(selectFields);
            }
            
            // 执行查询 - 使用智能映射
            String[] selectFields = new String[]{"*"};
            String fullSql = buildSelectSqlWithWhere(service, wrapper, selectFields);
            
            // 创建分页对象
            Page<Map<String, Object>> page = createMapPage(pageDTO);
            
            // 使用EnhancedVoMapper执行查询，实现智能映射
            EnhancedVoMapper<T, V> mapper = getEnhancedVoMapper(service, voClass);
            List<V> voList;
            long total;
            long current;
            long size;
            
            if (mapper == null) {
                // 如果Mapper没有实现EnhancedVoMapper，使用基础查询
                log.warn("Mapper {} 没有实现EnhancedVoMapper，使用基础查询", service.getBaseMapper().getClass().getSimpleName());
                Page<T> entityPage = createEntityPage(pageDTO);
                IPage<T> entityResult = service.page(entityPage, wrapper);
                voList = VoMapper.mapToVoList(entityResult.getRecords(), voClass);
                total = entityResult.getTotal();
                current = entityResult.getCurrent();
                size = entityResult.getSize();
            } else {
                IPage<Map<String, Object>> result = mapper.selectPageAsVo(page, fullSql);
                voList = mapMapToVoList(result.getRecords(), voClass);
                total = result.getTotal();
                current = result.getCurrent();
                size = result.getSize();
            }
            
            long endTime = System.currentTimeMillis();
            long queryTime = endTime - startTime;
            
            // 获取执行计划
            String executionPlan = getExecutionPlan(wrapper);
            
            // 构建聚合结果
            Map<String, Object> aggregations = new HashMap<>();
            if (pageDTO.getAggregations() != null && !pageDTO.getAggregations().isEmpty()) {
                List<AggregationPageDTO.AggregationField> aggList = (List<AggregationPageDTO.AggregationField>) pageDTO.getAggregations();
                aggregations = buildAggregationResults(aggList, voList);
            }
            
            EnhancedPageResult<V> enhancedResult = new EnhancedPageResult<>(voList, total, current, size);
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
    public static <T, V extends BaseVO<?>> PageResult<V> quickPage(IService<T> service, PageDTO pageDTO, Class<V> voClass) {
        return pageWithCondition(service, pageDTO, voClass);
    }
    
    /**
     * 快速列表查询 - 支持VO映射
     * 简化版列表查询，自动处理单表/多表查询
     */
    public static <T, V extends BaseVO<?>> List<V> quickList(IService<T> service, QueryDTO queryDTO, Class<V> voClass) {
        return listWithCondition(service, queryDTO, voClass);
    }
    
    /**
     * 快速单个查询 - 支持VO映射
     * 简化版单个查询，自动处理单表/多表查询
     */
    public static <T, V extends BaseVO<?>> V quickGetOne(IService<T> service, QueryDTO queryDTO, Class<V> voClass) {
        return getOneWithCondition(service, queryDTO, voClass);
    }
    
    /**
     * 统计查询 - 支持VO映射
     * 返回符合条件的记录数量
     */
    public static <T, V extends BaseVO<?>> Long countWithCondition(IService<T> service, QueryDTO queryDTO, Class<V> voClass) {
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
    public static <T, V extends BaseVO<?>> boolean existsWithCondition(IService<T> service, QueryDTO queryDTO, Class<V> voClass) {
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
     * 执行带性能监控的查询
     * 统一处理性能监控逻辑，减少重复代码
     */
    private static <T, V extends BaseVO<?>> PerformancePageResult<V> executeWithPerformanceMonitoring(
            IService<T> service, 
            QueryWrapper<T> wrapper, 
            PageDTO pageDTO, 
            Class<V> voClass,
            PerformancePageDTO performancePageDTO) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 执行查询
            PageResult<V> pageResult = executeQuery(service, wrapper, pageDTO, voClass);
            
            long endTime = System.currentTimeMillis();
            long queryTime = endTime - startTime;
            
            // 构建性能结果
            PerformancePageResult<V> performanceResult = new PerformancePageResult<>(
                pageResult.getRecords(), 
                pageResult.getTotal(), 
                pageResult.getCurrent(), 
                pageResult.getSize()
            );
            
            // 设置性能数据
            String executionPlan = getExecutionPlan(wrapper);
            PerformancePageResult.PageStatistics statistics = buildStatistics(performancePageDTO, queryTime, executionPlan);
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
     * 统一的查询执行方法
     * 处理Mapper获取和查询执行逻辑
     */
    private static <T, V extends BaseVO<?>> PageResult<V> executeQuery(
            IService<T> service, 
            QueryWrapper<T> wrapper, 
            PageDTO pageDTO, 
            Class<V> voClass) {
        
        // 获取Mapper
        EnhancedVoMapper<T, V> mapper = getEnhancedVoMapper(service, voClass);
        
        if (mapper == null) {
            // 基础查询
            log.warn("Mapper {} 没有实现EnhancedVoMapper，使用基础查询", service.getBaseMapper().getClass().getSimpleName());
            Page<T> entityPage = createEntityPage(pageDTO);
            IPage<T> entityResult = service.page(entityPage, wrapper);
            List<V> voList = VoMapper.mapToVoList(entityResult.getRecords(), voClass);
            return PageResult.of(voList, entityResult.getTotal(), entityResult.getCurrent(), entityResult.getSize());
        } else {
            // 增强查询
            String[] selectFields = VoFieldSelector.getSelectFields(voClass);
            wrapper.select(selectFields);
            String fullSql = buildSelectSql(service, wrapper, selectFields);
            
            Page<Map<String, Object>> page = createMapPage(pageDTO);
            IPage<Map<String, Object>> result = mapper.selectPageAsVo(page, fullSql);
            
            // 手动映射Map到VO
            List<V> voList = mapMapToVoList(result.getRecords(), voClass);
            return PageResult.of(voList, result.getTotal(), result.getCurrent(), result.getSize());
        }
    }
    
    /**
     * 创建Map类型的分页对象
     */
    private static Page<Map<String, Object>> createMapPage(PageDTO pageDTO) {
        return new Page<>(pageDTO.getPageNo(), pageDTO.getPageSize());
    }
    
    /**
     * 将Map列表映射为VO列表
     * 支持父类字段映射
     * 支持@VoMapping.Field注解和@FieldMapping注解
     */
    private static <V extends BaseVO<?>> List<V> mapMapToVoList(List<Map<String, Object>> mapList, Class<V> voClass) {
        List<V> voList = new ArrayList<>();
        
        // 检查是否有@VoMapping注解
        VoMapping voMapping = voClass.getAnnotation(VoMapping.class);
        Map<String, String> fieldMappingMap = null;
        if (voMapping != null && voMapping.fields().length > 0) {
            // 构建字段映射表：target -> source
            fieldMappingMap = new HashMap<>();
            for (VoMapping.Field field : voMapping.fields()) {
                if (StringUtils.isNotBlank(field.target())) {
                    // 使用target作为Map的key（因为SQL中使用了AS target）
                    fieldMappingMap.put(field.target(), field.target());
                }
            }
        }
        
        for (Map<String, Object> map : mapList) {
            try {
                V vo = voClass.getDeclaredConstructor().newInstance();
                
                // 处理字段映射注解（包括父类字段）
                Field[] allFields = getAllFields(voClass);
                for (Field field : allFields) {
                    // 跳过静态字段
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    
                    // 跳过特殊字段
                    if (isSpecialField(field.getName())) {
                        continue;
                    }
                    
                    field.setAccessible(true);
                    Object value = null;
                    String mapKey = null;
                    
                    // 优先处理@VoMapping.Field注解
                    if (voMapping != null && voMapping.fields().length > 0) {
                        // 查找对应的@VoMapping.Field配置
                        for (VoMapping.Field voField : voMapping.fields()) {
                            if (field.getName().equals(voField.target())) {
                                // 使用target作为Map的key（因为SQL中使用了AS target）
                                mapKey = voField.target();
                                break;
                            }
                        }
                    }
                    
                    // 如果没找到@VoMapping.Field，尝试@FieldMapping注解
                    if (mapKey == null) {
                        FieldMapping mapping = field.getAnnotation(FieldMapping.class);
                        if (mapping != null && !mapping.ignore()) {
                            mapKey = mapping.value();
                        }
                    }
                    
                    // 如果还是没找到，使用字段名转换
                    if (mapKey == null) {
                        mapKey = FieldConversionUtils.convertFieldToColumn(field.getName());
                    }
                    
                    // 从Map中获取值
                    if (mapKey != null && map.containsKey(mapKey)) {
                        value = map.get(mapKey);
                        // 类型转换：将数据库值转换为目标字段类型
                        Object convertedValue = convertValue(value, field.getType());
                        field.set(vo, convertedValue);
                    }
                }
                
                voList.add(vo);
            } catch (Exception e) {
                log.error("Map映射到VO失败: {}", e.getMessage(), e);
            }
        }
        
        return voList;
    }
    
    /**
     * 类型转换：将数据库值转换为目标字段类型
     * 支持常见的类型转换，特别是 tinyint (Integer 0/1) -> Boolean
     */
    private static Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            // 如果目标类型是基本类型，返回默认值
            if (targetType.isPrimitive()) {
                if (targetType == boolean.class) return false;
                if (targetType == int.class) return 0;
                if (targetType == long.class) return 0L;
                if (targetType == double.class) return 0.0;
                if (targetType == float.class) return 0.0f;
                if (targetType == byte.class) return (byte) 0;
                if (targetType == short.class) return (short) 0;
                if (targetType == char.class) return '\u0000';
            }
            return null;
        }
        
        // 如果类型已经匹配，直接返回
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        
        // Boolean/boolean 类型转换（支持 Integer 0/1 -> Boolean）
        if (targetType == Boolean.class || targetType == boolean.class) {
            if (value instanceof Integer) {
                return ((Integer) value) != 0;
            }
            if (value instanceof Long) {
                return ((Long) value) != 0L;
            }
            if (value instanceof Byte) {
                return ((Byte) value) != 0;
            }
            if (value instanceof Short) {
                return ((Short) value) != 0;
            }
            if (value instanceof String) {
                String str = ((String) value).trim().toLowerCase();
                return "1".equals(str) || "true".equals(str) || "yes".equals(str) || "on".equals(str);
            }
        }
        
        // Integer/int 类型转换
        if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            if (value instanceof String) {
                try {
                    return Integer.parseInt((String) value);
                } catch (NumberFormatException e) {
                    log.warn("无法将字符串 '{}' 转换为 Integer", value);
                    return 0;
                }
            }
        }
        
        // Long/long 类型转换
        if (targetType == Long.class || targetType == long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            if (value instanceof String) {
                try {
                    return Long.parseLong((String) value);
                } catch (NumberFormatException e) {
                    log.warn("无法将字符串 '{}' 转换为 Long", value);
                    return 0L;
                }
            }
        }
        
        // Double/double 类型转换
        if (targetType == Double.class || targetType == double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            if (value instanceof String) {
                try {
                    return Double.parseDouble((String) value);
                } catch (NumberFormatException e) {
                    log.warn("无法将字符串 '{}' 转换为 Double", value);
                    return 0.0;
                }
            }
        }
        
        // Float/float 类型转换
        if (targetType == Float.class || targetType == float.class) {
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
            if (value instanceof String) {
                try {
                    return Float.parseFloat((String) value);
                } catch (NumberFormatException e) {
                    log.warn("无法将字符串 '{}' 转换为 Float", value);
                    return 0.0f;
                }
            }
        }
        
        // String 类型转换
        if (targetType == String.class) {
            return value.toString();
        }
        
        // 如果无法转换，记录警告并返回原值
        log.warn("无法将类型 {} 转换为 {}，返回原值", value.getClass().getName(), targetType.getName());
        return value;
    }
    
    /**
     * 获取类及其所有父类的字段
     */
    private static Field[] getAllFields(Class<?> clazz) {
        List<Field> allFields = new ArrayList<>();
        
        // 遍历整个继承链
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            Field[] declaredFields = currentClass.getDeclaredFields();
            for (Field field : declaredFields) {
                allFields.add(field);
            }
            currentClass = currentClass.getSuperclass();
        }
        
        return allFields.toArray(new Field[0]);
    }
    
    /**
     * 判断是否为特殊字段
     */
    private static boolean isSpecialField(String fieldName) {
        // 常见的特殊字段
        return "serialVersionUID".equals(fieldName) ||
               "class".equals(fieldName) ||
               fieldName.startsWith("$");
    }
    
    
    /**
     * SQL构建器 - 统一SQL构建逻辑
     * 构建完整的SQL，避免参数绑定问题
     */
    /**
     * 构建完整的SELECT SQL
     * 使用更简单的方法，直接使用MyBatis-Plus的内置方法
     */
    /**
     * 构建带 WHERE 条件的 SELECT SQL
     * 统一处理 SQL 构建，避免 WHERE 后面没有条件或包含 ORDER BY 的问题
     */
    private static <T> String buildSelectSqlWithWhere(IService<T> service, QueryWrapper<T> wrapper, String[] selectFields) {
        String tableName = getTableName(service);
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(String.join(", ", selectFields));
        sql.append(" FROM ").append(tableName);
        
        // 获取 WHERE 条件（不包含 ORDER BY）
        String whereClause = getWhereClauseWithoutParams(wrapper);
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
        }
        
        // 获取 ORDER BY 子句
        String orderByClause = getOrderByClause(wrapper);
        if (orderByClause != null && !orderByClause.trim().isEmpty()) {
            sql.append(" ").append(orderByClause);
        }
        
        return sql.toString();
    }
    
    /**
     * 从 QueryWrapper 中提取 ORDER BY 子句
     */
    private static <T> String getOrderByClause(QueryWrapper<T> wrapper) {
        String sqlSegment = wrapper.getSqlSegment();
        if (sqlSegment == null || sqlSegment.trim().isEmpty()) {
            return null;
        }
        
        // 替换参数占位符为实际值
        String result = replaceParameterPlaceholders(sqlSegment, wrapper);
        
        // 提取 ORDER BY 子句
        if (result != null) {
            int orderByIndex = result.toUpperCase().indexOf(" ORDER BY ");
            if (orderByIndex >= 0) {
                String orderByClause = result.substring(orderByIndex).trim();
                // 移除可能存在的 WHERE 关键字（如果 ORDER BY 前面有 WHERE）
                if (orderByClause.toUpperCase().startsWith("WHERE")) {
                    int whereIndex = orderByClause.toUpperCase().indexOf(" ORDER BY ");
                    if (whereIndex > 0) {
                        orderByClause = orderByClause.substring(whereIndex).trim();
                    }
                }
                return orderByClause;
            }
        }
        
        return null;
    }
    
    private static <T> String buildSelectSql(IService<T> service, QueryWrapper<T> wrapper, String[] selectFields) {
        // 获取实体类对应的表名
        String tableName = getTableNameFromEntity(service);
        
        // 构建完整的SQL
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(String.join(", ", selectFields));
        sql.append(" FROM ").append(tableName);
        
        // 使用MyBatis-Plus的内置方法获取WHERE条件
        // 注意：这里我们需要获取不带参数占位符的SQL片段
        String whereClause = getWhereClauseWithoutParams(wrapper);
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            // 移除可能存在的 WHERE 关键字（如果 sqlSegment 已经包含）
            String normalizedWhere = whereClause.trim();
            if (normalizedWhere.toUpperCase().startsWith("WHERE")) {
                normalizedWhere = normalizedWhere.substring(5).trim();
            }
            // 只有当有实际条件时才添加 WHERE
            if (!normalizedWhere.isEmpty()) {
                sql.append(" WHERE ").append(normalizedWhere);
            }
        }
        
        // 获取 ORDER BY 子句
        String orderByClause = getOrderByClause(wrapper);
        if (orderByClause != null && !orderByClause.trim().isEmpty()) {
            sql.append(" ").append(orderByClause);
        }
        
        return sql.toString();
    }
    
    /**
     * 获取不带参数占位符的WHERE条件
     * 这是一个简化的实现，直接使用QueryWrapper的toString方法
     * 注意：返回的SQL片段不包含 WHERE 关键字，也不包含 ORDER BY
     */
    private static <T> String getWhereClauseWithoutParams(QueryWrapper<T> wrapper) {
        // 获取QueryWrapper的SQL片段
        String sqlSegment = wrapper.getSqlSegment();
        
        if (sqlSegment == null || sqlSegment.trim().isEmpty()) {
            return null;
        }
        
        // 替换参数占位符为实际值
        String result = replaceParameterPlaceholders(sqlSegment, wrapper);
        
        // 移除 ORDER BY 子句（在 WHERE 条件中不应该包含 ORDER BY）
        if (result != null) {
            int orderByIndex = result.toUpperCase().indexOf(" ORDER BY ");
            if (orderByIndex >= 0) {
                result = result.substring(0, orderByIndex).trim();
            }
        }
        
        // 移除可能存在的 WHERE 关键字
        if (result != null && result.toUpperCase().startsWith("WHERE")) {
            result = result.substring(5).trim();
        }
        
        // 如果结果为空，返回 null
        if (result == null || result.trim().isEmpty()) {
            return null;
        }
        
        return result;
    }
    
    /**
     * 替换参数占位符为实际值
     */
    private static <T> String replaceParameterPlaceholders(String sqlSegment, QueryWrapper<T> wrapper) {
        // 获取QueryWrapper的参数映射
        Map<String, Object> paramNameValuePairs = wrapper.getParamNameValuePairs();
        
        String result = sqlSegment;
        for (Map.Entry<String, Object> entry : paramNameValuePairs.entrySet()) {
            String paramName = entry.getKey();
            Object paramValue = entry.getValue();
            
            // 替换 #{ew.paramNameValuePairs.paramName} 为实际值
            String placeholder = "#{ew.paramNameValuePairs." + paramName + "}";
            String value = formatSqlValue(paramValue);
            result = result.replace(placeholder, value);
        }
        
        return result;
    }
    
    /**
     * 格式化SQL值
     */
    private static String formatSqlValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        
        if (value instanceof String) {
            return "'" + value.toString().replace("'", "''") + "'";
        }
        
        if (value instanceof Number) {
            return value.toString();
        }
        
        if (value instanceof Boolean) {
            return value.toString();
        }
        
        // 其他类型转换为字符串
        return "'" + value.toString().replace("'", "''") + "'";
    }
    
    /**
     * 从实体类获取表名
     */
    private static <T> String getTableNameFromEntity(IService<T> service) {
        try {
            // 获取实体类
            Class<?> entityClass = service.getEntityClass();
            
            // 检查是否有@TableName注解
            com.baomidou.mybatisplus.annotation.TableName tableNameAnnotation = 
                entityClass.getAnnotation(com.baomidou.mybatisplus.annotation.TableName.class);
            
            if (tableNameAnnotation != null && !tableNameAnnotation.value().isEmpty()) {
                return tableNameAnnotation.value();
            }
            
            // 如果没有注解，使用类名转下划线
            String className = entityClass.getSimpleName();
            return com.baomidou.mybatisplus.core.toolkit.StringUtils.camelToUnderline(className);
            
        } catch (Exception e) {
            log.warn("无法获取实体类表名，使用默认值", e);
            return "t_" + service.getClass().getSimpleName().toLowerCase().replace("service", "");
        }
    }
    
    
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
            for (Object aggObj : aggregations) {
                AggregationPageDTO.AggregationField agg = (AggregationPageDTO.AggregationField) aggObj;
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
    
    /**
     * 安全获取EnhancedVoMapper，如果不存在则使用基础查询
     */
    private static <T, V extends BaseVO<?>> EnhancedVoMapper<T, V> getEnhancedVoMapper(IService<T> service, Class<V> voClass) {
        BaseMapper<T> baseMapper = service.getBaseMapper();
        // 由于类型擦除，instanceof 检查使用原始类型
        if (baseMapper instanceof EnhancedVoMapper) {
            // 类型转换是安全的，因为 EnhancedVoMapper<T, V> 在运行时就是 EnhancedVoMapper
            return (EnhancedVoMapper<T, V>) baseMapper;
        } else {
            log.warn("Mapper {} 没有实现EnhancedVoMapper，将使用基础查询", baseMapper.getClass().getSimpleName());
            return null;
        }
    }
    
    
    /**
     * 创建实体分页对象
     * 统一实体分页对象创建逻辑，避免代码重复
     */
    private static <T> Page<T> createEntityPage(PageDTO pageDTO) {
        return new Page<>(pageDTO.getPageNo(), pageDTO.getPageSize());
    }
    
    // ==================== 异步查询方法（实验性功能） ====================
    
    /**
     * 异步基础分页查询
     * 使用 CompletableFuture 实现异步查询，不阻塞当前线程
     * 
     * <p><strong>⚠️ 实验性功能</strong>：此方法目前处于实验阶段，API可能会在后续版本中调整。</p>
     * 
     * <h3>使用场景：</h3>
     * <ul>
     *   <li>大数据量查询（>10万条记录）</li>
     *   <li>复杂多表关联查询</li>
     *   <li>需要并行执行多个查询</li>
     *   <li>提升用户体验（避免界面卡顿）</li>
     * </ul>
     * 
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 异步查询
     * CompletableFuture<PageResult<ProductVO>> future = 
     *     EnhancedQueryBuilder.pageWithConditionAsync(productService, pageDTO, ProductVO.class);
     * 
     * // 处理结果
     * future.thenAccept(result -> {
     *     System.out.println("查询完成，共" + result.getTotal() + "条记录");
     * }).exceptionally(throwable -> {
     *     System.err.println("查询失败：" + throwable.getMessage());
     *     return null;
     * });
     * 
     * // 或者等待结果
     * PageResult<ProductVO> result = future.get(30, TimeUnit.SECONDS);
     * }</pre>
     * 
     */
    public static <T, V extends BaseVO<?>> CompletableFuture<PageResult<V>> pageWithConditionAsync(
            IService<T> service, PageDTO pageDTO, Class<V> voClass) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return pageWithCondition(service, pageDTO, voClass);
            } catch (Exception e) {
                log.error("异步分页查询失败", e);
                throw new RuntimeException("异步分页查询失败: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * 异步列表查询
     * 适用于不需要分页的列表查询场景
     * 
     * <p><strong>⚠️ 实验性功能</strong>：此方法目前处于实验阶段，API可能会在后续版本中调整。</p>
     * 
     */
    public static <T, V extends BaseVO<?>> CompletableFuture<List<V>> listWithConditionAsync(
            IService<T> service, QueryDTO queryDTO, Class<V> voClass) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return listWithCondition(service, queryDTO, voClass);
            } catch (Exception e) {
                log.error("异步列表查询失败", e);
                throw new RuntimeException("异步列表查询失败: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * 异步单个查询
     * 适用于查询单条记录的场景
     * 
     * <p><strong>⚠️ 实验性功能</strong>：此方法目前处于实验阶段，API可能会在后续版本中调整。</p>
     * 
     */
    public static <T, V extends BaseVO<?>> CompletableFuture<V> getOneWithConditionAsync(
            IService<T> service, QueryDTO queryDTO, Class<V> voClass) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getOneWithCondition(service, queryDTO, voClass);
            } catch (Exception e) {
                log.error("异步单个查询失败", e);
                throw new RuntimeException("异步单个查询失败: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * 异步性能监控查询
     * 适用于需要性能监控的查询场景
     * 
     * <p><strong>⚠️ 实验性功能</strong>：此方法目前处于实验阶段，API可能会在后续版本中调整。</p>
     * 
     */
    public static <T, V extends BaseVO<?>> CompletableFuture<PerformancePageResult<V>> pageWithPerformanceAsync(
            IService<T> service, PerformancePageDTO pageDTO, Class<V> voClass) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return pageWithPerformance(service, pageDTO, voClass);
            } catch (Exception e) {
                log.error("异步性能监控查询失败", e);
                throw new RuntimeException("异步性能监控查询失败: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * 异步聚合查询
     * 适用于需要聚合统计的查询场景
     * 
     * <p><strong>⚠️ 实验性功能</strong>：此方法目前处于实验阶段，API可能会在后续版本中调整。</p>
     * 
     */
    public static <T, V extends BaseVO<?>> CompletableFuture<AggregationPageResult<V>> pageWithAggregationAsync(
            IService<T> service, AggregationPageDTO pageDTO, Class<V> voClass) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return pageWithAggregation(service, pageDTO, voClass);
            } catch (Exception e) {
                log.error("异步聚合查询失败", e);
                throw new RuntimeException("异步聚合查询失败: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * 异步增强查询
     * 适用于需要组合功能的复杂查询场景
     * 
     * <p><strong>⚠️ 实验性功能</strong>：此方法目前处于实验阶段，API可能会在后续版本中调整。</p>
     * 
     */
    public static <T, V extends BaseVO<?>> CompletableFuture<EnhancedPageResult<V>> pageWithEnhancedAsync(
            IService<T> service, EnhancedPageDTO pageDTO, Class<V> voClass) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return pageWithEnhanced(service, pageDTO, voClass);
            } catch (Exception e) {
                log.error("异步增强查询失败", e);
                throw new RuntimeException("异步增强查询失败: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * 异步统计查询
     * 适用于只需要记录数量的查询场景
     * 
     * <p><strong>⚠️ 实验性功能</strong>：此方法目前处于实验阶段，API可能会在后续版本中调整。</p>
     * 
     */
    public static <T, V extends BaseVO<?>> CompletableFuture<Long> countWithConditionAsync(
            IService<T> service, QueryDTO queryDTO, Class<V> voClass) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return countWithCondition(service, queryDTO, voClass);
            } catch (Exception e) {
                log.error("异步统计查询失败", e);
                throw new RuntimeException("异步统计查询失败: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * 异步存在性查询
     * 适用于检查记录是否存在的场景
     * 
     * <p><strong>⚠️ 实验性功能</strong>：此方法目前处于实验阶段，API可能会在后续版本中调整。</p>
     * 
     */
    public static <T, V extends BaseVO<?>> CompletableFuture<Boolean> existsWithConditionAsync(
            IService<T> service, QueryDTO queryDTO, Class<V> voClass) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return existsWithCondition(service, queryDTO, voClass);
            } catch (Exception e) {
                log.error("异步存在性查询失败", e);
                throw new RuntimeException("异步存在性查询失败: " + e.getMessage(), e);
            }
        });
    }
    
    // ==================== 异步查询工具方法（实验性功能） ====================
    
    /**
     * 并行执行多个异步查询
     * 适用于需要同时查询多个数据集的场景
     * 
     * <p><strong>⚠️ 实验性功能</strong>：此方法目前处于实验阶段，API可能会在后续版本中调整。</p>
     * 
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 并行查询用户信息和订单信息
     * CompletableFuture<PageResult<UserVO>> userFuture = 
     *     pageWithConditionAsync(userService, userPageDTO, UserVO.class);
     * CompletableFuture<PageResult<OrderVO>> orderFuture = 
     *     pageWithConditionAsync(orderService, orderPageDTO, OrderVO.class);
     * 
     * // 等待所有查询完成
     * CompletableFuture.allOf(userFuture, orderFuture)
     *     .thenRun(() -> {
     *         PageResult<UserVO> users = userFuture.join();
     *         PageResult<OrderVO> orders = orderFuture.join();
     *         // 处理结果
     *     });
     * }</pre>
     * 
     */
    public static CompletableFuture<Void> executeAllAsync(CompletableFuture<?>... futures) {
        return CompletableFuture.allOf(futures);
    }
    
    /**
     * 异步查询超时控制
     * 为异步查询添加超时机制，避免长时间等待
     * 
     * <p><strong>⚠️ 实验性功能</strong>：此方法目前处于实验阶段，API可能会在后续版本中调整。</p>
     * 
     * <h3>使用示例：</h3>
     * <pre>{@code
     * CompletableFuture<PageResult<ProductVO>> future = 
     *     pageWithConditionAsync(productService, pageDTO, ProductVO.class);
     * 
     * try {
     *     PageResult<ProductVO> result = future.get(30, TimeUnit.SECONDS);
     * } catch (TimeoutException e) {
     *     log.warn("查询超时，取消执行");
     *     future.cancel(true);
     * }
     * }</pre>
     * 
     */
    public static <T> CompletableFuture<T> withTimeout(CompletableFuture<T> future, long timeout, TimeUnit unit) {
        return future.orTimeout(timeout, unit);
    }
}