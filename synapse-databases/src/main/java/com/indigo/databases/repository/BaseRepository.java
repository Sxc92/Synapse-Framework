package com.indigo.databases.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.indigo.core.entity.dto.BaseDTO;
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

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
 *   <li><strong>异步查询</strong>：基于CompletableFuture的异步查询支持（实验性功能）</li>
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
 *     PageResult<ProductMultiTableVO> pageProductsWithJoin(JoinPageDTO joinPageDTO);
 *     
 *     // 聚合查询
 *     AggregationPageResult<ProductVO> getProductStatistics(AggregationPageDTO aggregationPageDTO);
 *     
 *     // 便捷查询 - 支持@VoMapping注解的多表关联
 *     PageResult<ProductMultiTableVO> quickPageProducts(ProductPageQueryDTO queryDTO);
 *     
 *     // 异步查询（实验性功能）
 *     CompletableFuture<PageResult<ProductVO>> asyncPageProducts(ProductPageQueryDTO queryDTO);
 * }
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
    default PageResult<T> pageWithCondition(PageDTO<?> queryDTO) {
        return EnhancedQueryBuilder.pageWithCondition(this, queryDTO);
    }
    
    /**
     * 分页查询 - 返回VO对象（推荐使用）
     * 约定：所有分页请求参数都要继承 PageDTO
     * 通过智能字段选择直接映射到VO，避免内存转换
     */
    default <V extends BaseVO<?>> PageResult<V> pageWithCondition(PageDTO<?> queryDTO, Class<V> voClass) {
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
    default List<T> listWithDTO(QueryDTO<?> queryDTO) {
        QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(queryDTO);
        return list(wrapper);
    }
    
    /**
     * 列表查询 - 返回VO对象（推荐使用）
     */
    default <V extends BaseVO<?>> List<V> listWithDTO(QueryDTO<?> queryDTO, Class<V> voClass) {
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
    default T getOneWithDTO(QueryDTO<?> queryDTO) {
        QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(queryDTO);
        return getOne(wrapper);
    }
    
    /**
     * 单个查询 - 返回VO对象（推荐使用）
     */
    default <V extends BaseVO<?>> V getOneWithDTO(QueryDTO<?> queryDTO, Class<V> voClass) {
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
    default long countWithDTO(QueryDTO<?> queryDTO) {
        QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(queryDTO);
        return count(wrapper);
    }
    
    // ==================== 增强查询方法 ====================
    
    /**
     * 聚合统计查询 - 支持VO映射
     * 支持 COUNT, SUM, AVG, MAX, MIN 等聚合函数
     */
    default <V extends BaseVO<?>> AggregationPageResult<V> pageWithAggregation(AggregationPageDTO<?> queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.pageWithAggregation(this, queryDTO, voClass);
    }
    
    /**
     * 分组统计查询 - 支持VO映射
     * 支持 GROUP BY 分组统计
     */
    default <V extends BaseVO<?>> AggregationPageResult<V> pageWithGroupBy(AggregationPageDTO<?> queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.pageWithGroupBy(this, queryDTO, voClass);
    }
    
    /**
     * 性能监控查询 - 支持VO映射
     * 返回查询执行时间和执行计划
     */
    default <V extends BaseVO<?>> PerformancePageResult<V> pageWithPerformance(PerformancePageDTO<?> queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.pageWithPerformance(this, queryDTO, voClass);
    }
    
    /**
     * 字段选择查询 - 支持VO映射
     * 支持指定返回字段，提高查询性能
     */
    default <V extends BaseVO<?>> PerformancePageResult<V> pageWithSelectFields(PerformancePageDTO<?> queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.pageWithSelectFields(this, queryDTO, voClass);
    }

    /**
     * 多表关联查询 - 基于@VoMapping注解
     * 自动根据VO类的@VoMapping注解配置进行多表关联查询
     * 这是推荐的多表查询方式
     */
    default <V extends BaseVO<?>> PageResult<V> pageWithVoMapping(PageDTO<?> queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.pageWithCondition(this, queryDTO, voClass);
    }
    
    /**
     * 复杂查询 - 支持VO映射
     * 支持自定义SQL查询
     */
    default <V extends BaseVO<?>> PageResult<V> pageWithComplexQuery(ComplexPageDTO<?> queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.pageWithComplexQuery(this, queryDTO, voClass);
    }
    
    /**
     * 增强查询（组合聚合和性能优化）- 支持VO映射
     */
    default <V extends BaseVO<?>> EnhancedPageResult<V> pageWithEnhanced(EnhancedPageDTO<?> queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.pageWithEnhanced(this, queryDTO, voClass);
    }
    
    // ==================== 便捷查询方法 ====================
    
    /**
     * 快速分页查询 - 支持VO映射
     * 简化版分页查询，自动处理单表/多表查询
     * 支持@VoMapping注解配置的多表关联查询
     */
    default <V extends BaseVO<?>> PageResult<V> quickPage(PageDTO<?> pageDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.quickPage(this, pageDTO, voClass);
    }
    
    // ==================== 唯一性检查方法 ====================
    
    /**
     * 检查关键字段唯一性
     * 根据ID是否为null判断新增或更新场景：
     * - id == null：检查整个表中是否存在相同的关键字段值（新增场景）
     * - id != null：检查除当前记录外是否存在相同的关键字段值（更新场景）
     * 
     * @param entity 要检查的实体对象
     * @param keyFields 关键字段名数组，支持多个字段组合唯一性检查
     * @return true表示存在重复，false表示不重复
     * 
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 检查用户名是否重复
     * boolean isDuplicate = userRepository.checkKeyUniqueness(user, "username");
     * 
     * // 检查邮箱是否重复
     * boolean isDuplicate = userRepository.checkKeyUniqueness(user, "email");
     * 
     * // 检查用户名+邮箱组合是否重复
     * boolean isDuplicate = userRepository.checkKeyUniqueness(user, "username", "email");
     * 
     * // 在Service中使用
     * if (userRepository.checkKeyUniqueness(user, "username")) {
     *     throw new BusinessException("用户名已存在");
     * }
     * }</pre>
     */
    default boolean checkKeyUniqueness(T entity, String... keyFields) {
        if (entity == null || keyFields == null || keyFields.length == 0) {
            return false;
        }
        
        QueryWrapper<T> wrapper = buildQueryWrapper(entity, keyFields);
        Object entityId = getEntityId(entity);
        
        return checkUniquenessWithId(wrapper, entityId);
    }
    
    /**
     * 检查关键字段唯一性（基于BaseDTO）
     * 支持BaseDTO类型的参数，自动处理ID字段
     * 
     * @param dto 要检查的BaseDTO对象
     * @param keyFields 关键字段名数组，支持多个字段组合唯一性检查
     * @return true表示存在重复，false表示不重复
     * 
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 检查用户名是否重复
     * boolean isDuplicate = userRepository.checkKeyUniqueness(userDTO, "username");
     * 
     * // 检查邮箱是否重复
     * boolean isDuplicate = userRepository.checkKeyUniqueness(userDTO, "email");
     * 
     * // 检查用户名+邮箱组合是否重复
     * boolean isDuplicate = userRepository.checkKeyUniqueness(userDTO, "username", "email");
     * }</pre>
     */
    default boolean checkKeyUniqueness(BaseDTO<?> dto, String... keyFields) {
        if (dto == null || keyFields == null || keyFields.length == 0) {
            return false;
        }
        
        QueryWrapper<T> wrapper = buildQueryWrapper(dto, keyFields);
        Object dtoId = dto.getId();
        
        return checkUniquenessWithId(wrapper, dtoId);
    }
    
    /**
     * 检查关键字段唯一性（支持自定义字段名映射）
     * 当数据库字段名与实体字段名不同时使用
     * 
     * @param entity 要检查的实体对象
     * @param fieldMappings 字段映射数组，格式为 "实体字段名:数据库字段名"
     * @return true表示存在重复，false表示不重复
     * 
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 实体字段名为userName，数据库字段名为user_name
     * boolean isDuplicate = userRepository.checkKeyUniquenessWithMapping(user, "userName:user_name");
     * 
     * // 多个字段映射
     * boolean isDuplicate = userRepository.checkKeyUniquenessWithMapping(user, 
     *     "userName:user_name", "emailAddress:email_address");
     * }</pre>
     */
    default boolean checkKeyUniquenessWithMapping(T entity, String... fieldMappings) {
        if (entity == null || fieldMappings == null || fieldMappings.length == 0) {
            return false;
        }

        QueryWrapper<T> wrapper = buildQueryWrapperWithMapping(entity, fieldMappings);
        Object entityId = getEntityId(entity);
        
        return checkUniquenessWithId(wrapper, entityId);
    }

    /**
     * 执行唯一性检查的核心逻辑
     * 根据ID是否为null决定查询策略
     * 
     * @param wrapper 查询条件包装器
     * @param id ID值
     * @return true表示存在重复，false表示不重复
     */
    private boolean checkUniquenessWithId(QueryWrapper<T> wrapper, Object id) {
        // 根据ID是否为null决定查询策略
        if (id != null) {
            // 更新场景：排除当前记录
            wrapper.ne("id", id);
        }
        // 新增场景：不需要额外条件
        
        // 执行查询，如果存在记录则说明有重复
        return count(wrapper) > 0;
    }
    
    /**
     * 构建查询条件包装器（实体对象）
     * 
     * @param entity 实体对象
     * @param keyFields 关键字段名数组
     * @return QueryWrapper
     */
    private QueryWrapper<T> buildQueryWrapper(T entity, String[] keyFields) {
        QueryWrapper<T> wrapper = createQueryWrapper();
        
        // 构建关键字段的查询条件
        for (String field : keyFields) {
            addFieldCondition(wrapper, entity, field, field);
        }
        
        return wrapper;
    }
    
    /**
     * 构建查询条件包装器（BaseDTO对象）
     * 
     * @param dto BaseDTO对象
     * @param keyFields 关键字段名数组
     * @return QueryWrapper
     */
    private QueryWrapper<T> buildQueryWrapper(com.indigo.core.entity.dto.BaseDTO<?> dto, String[] keyFields) {
        QueryWrapper<T> wrapper = createQueryWrapper();
        
        // 构建关键字段的查询条件
        for (String field : keyFields) {
            addFieldCondition(wrapper, dto, field, field);
        }
        
        return wrapper;
    }
    
    /**
     * 构建查询条件包装器（支持字段映射）
     * 
     * @param entity 实体对象
     * @param fieldMappings 字段映射数组
     * @return QueryWrapper
     */
    private QueryWrapper<T> buildQueryWrapperWithMapping(T entity, String[] fieldMappings) {
        QueryWrapper<T> wrapper = createQueryWrapper();

        // 构建关键字段的查询条件
        for (String mapping : fieldMappings) {
            String[] parts = mapping.split(":");
            if (parts.length != 2) {
                continue; // 跳过格式不正确的映射
            }

            String entityFieldName = parts[0].trim();
            String dbFieldName = parts[1].trim();
            
            addFieldCondition(wrapper, entity, entityFieldName, dbFieldName);
        }
        return wrapper;
    }
    
    /**
     * 创建QueryWrapper实例
     * 
     * @return QueryWrapper实例
     */
    private QueryWrapper<T> createQueryWrapper() {
        return new QueryWrapper<>();
    }
    
    /**
     * 添加字段查询条件
     * 
     * @param wrapper 查询包装器
     * @param object 对象实例
     * @param fieldName 字段名
     * @param dbFieldName 数据库字段名
     */
    private void addFieldCondition(QueryWrapper<T> wrapper, Object object, String fieldName, String dbFieldName) {
        try {
            // 使用反射获取字段值
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object fieldValue = field.get(object);
            
            if (fieldValue != null) {
                wrapper.eq(dbFieldName, fieldValue);
            } else {
                // 如果字段值为null，检查数据库中该字段为null的记录
                wrapper.isNull(dbFieldName);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 如果字段不存在或无法访问，跳过该字段
        }
    }
    
    /**
     * 获取实体对象的ID字段值
     * 
     * @param entity 实体对象
     * @return ID值
     */
    private Object getEntityId(T entity) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            return idField.get(entity);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 如果ID字段不存在，返回null
            return null;
        }
    }

    /**
     * 快速列表查询 - 支持VO映射
     * 简化版列表查询，自动处理单表/多表查询
     * 支持@VoMapping注解配置的多表关联查询
     */
    default <V extends BaseVO<?>> List<V> quickList(QueryDTO<?> queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.quickList(this, queryDTO, voClass);
    }
    
    /**
     * 快速单个查询 - 支持VO映射
     * 简化版单个查询，自动处理单表/多表查询
     * 支持@VoMapping注解配置的多表关联查询
     */
    default <V extends BaseVO<?>> V quickGetOne(QueryDTO<?> queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.quickGetOne(this, queryDTO, voClass);
    }
    
    /**
     * 统计查询 - 支持VO映射
     * 返回符合条件的记录数量
     */
    default <V extends BaseVO<?>> Long countWithCondition(QueryDTO<?> queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.countWithCondition(this, queryDTO, voClass);
    }
    
    /**
     * 存在性查询 - 支持VO映射
     * 检查是否存在符合条件的记录
     */
    default <V extends BaseVO<?>> boolean existsWithCondition(QueryDTO<?> queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.existsWithCondition(this, queryDTO, voClass);
    }
    
    // ==================== 异步查询方法（实验性功能） ====================
    
    /**
     * 异步分页查询 - 支持VO映射
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
     */
    default <V extends BaseVO<?>> CompletableFuture<PageResult<V>> pageWithConditionAsync(PageDTO<?> pageDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.pageWithConditionAsync(this, pageDTO, voClass);
    }
    
    /**
     * 异步列表查询 - 支持VO映射
     * 适用于不需要分页的列表查询场景
     * 
     * <p><strong>⚠️ 实验性功能</strong>：此方法目前处于实验阶段，API可能会在后续版本中调整。</p>
     * 
     */
    default <V extends BaseVO<?>> CompletableFuture<List<V>> listWithConditionAsync(QueryDTO<?> queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.listWithConditionAsync(this, queryDTO, voClass);
    }
    
    /**
     * 异步单个查询 - 支持VO映射
     * 适用于查询单条记录的场景
     * 
     * <p><strong>⚠️ 实验性功能</strong>：此方法目前处于实验阶段，API可能会在后续版本中调整。</p>
     * 
     */
    default <V extends BaseVO<?>> CompletableFuture<V> getOneWithConditionAsync(QueryDTO<?> queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.getOneWithConditionAsync(this, queryDTO, voClass);
    }
    
    /**
     * 异步性能监控查询 - 支持VO映射
     * 适用于需要性能监控的查询场景
     * 
     * <p><strong>⚠️ 实验性功能</strong>：此方法目前处于实验阶段，API可能会在后续版本中调整。</p>
     * 
     */
    default <V extends BaseVO<?>> CompletableFuture<PerformancePageResult<V>> pageWithPerformanceAsync(PerformancePageDTO<?> pageDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.pageWithPerformanceAsync(this, pageDTO, voClass);
    }
    
    /**
     * 异步聚合查询 - 支持VO映射
     * 适用于需要聚合统计的查询场景
     * 
     * <p><strong>⚠️ 实验性功能</strong>：此方法目前处于实验阶段，API可能会在后续版本中调整。</p>
     * 
     */
    default <V extends BaseVO<?>> CompletableFuture<AggregationPageResult<V>> pageWithAggregationAsync(AggregationPageDTO<?> pageDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.pageWithAggregationAsync(this, pageDTO, voClass);
    }
    
    /**
     * 异步增强查询 - 支持VO映射
     * 适用于需要组合功能的复杂查询场景
     * 
     * <p><strong>⚠️ 实验性功能</strong>：此方法目前处于实验阶段，API可能会在后续版本中调整。</p>
     * 
     */
    default <V extends BaseVO<?>> CompletableFuture<EnhancedPageResult<V>> pageWithEnhancedAsync(EnhancedPageDTO<?> pageDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.pageWithEnhancedAsync(this, pageDTO, voClass);
    }
    
    /**
     * 异步统计查询 - 支持VO映射
     * 适用于只需要记录数量的查询场景
     * 
     * <p><strong>⚠️ 实验性功能</strong>：此方法目前处于实验阶段，API可能会在后续版本中调整。</p>
     * 
     */
    default <V extends BaseVO<?>> CompletableFuture<Long> countWithConditionAsync(QueryDTO<?> queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.countWithConditionAsync(this, queryDTO, voClass);
    }
    
    /**
     * 异步存在性查询 - 支持VO映射
     * 适用于检查记录是否存在的场景
     * 
     * <p><strong>⚠️ 实验性功能</strong>：此方法目前处于实验阶段，API可能会在后续版本中调整。</p>
     * 
     */
    default <V extends BaseVO<?>> CompletableFuture<Boolean> existsWithConditionAsync(QueryDTO<?> queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.existsWithConditionAsync(this, queryDTO, voClass);
    }
    
    /**
     * 异步快速分页查询 - 支持VO映射
     * 简化版异步分页查询，自动处理单表/多表查询
     * 
     * <p><strong>⚠️ 实验性功能</strong>：此方法目前处于实验阶段，API可能会在后续版本中调整。</p>
     * 
     */
    default <V extends BaseVO<?>> CompletableFuture<PageResult<V>> quickPageAsync(PageDTO<?> pageDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.pageWithConditionAsync(this, pageDTO, voClass);
    }
    
    /**
     * 异步快速列表查询 - 支持VO映射
     * 简化版异步列表查询，自动处理单表/多表查询
     * 
     * <p><strong>⚠️ 实验性功能</strong>：此方法目前处于实验阶段，API可能会在后续版本中调整。</p>
     * 
     */
    default <V extends BaseVO<?>> CompletableFuture<List<V>> quickListAsync(QueryDTO<?> queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.listWithConditionAsync(this, queryDTO, voClass);
    }
    
    /**
     * 异步快速单个查询 - 支持VO映射
     * 简化版异步单个查询，自动处理单表/多表查询
     * 
     * <p><strong>⚠️ 实验性功能</strong>：此方法目前处于实验阶段，API可能会在后续版本中调整。</p>
     * 
     */
    default <V extends BaseVO<?>> CompletableFuture<V> quickGetOneAsync(QueryDTO<?> queryDTO, Class<V> voClass) {
        return EnhancedQueryBuilder.getOneWithConditionAsync(this, queryDTO, voClass);
    }
} 