package com.indigo.databases.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.indigo.databases.dto.PageDTO;
import com.indigo.databases.dto.result.PageResult;

import java.util.Map;

/**
 * Lambda查询构建器
 * 提供基于注解的自动查询条件构建功能
 * 注意：对于简单的链式查询，建议直接使用 MyBatis-Plus 的 LambdaQueryWrapper
 *
 * @author 史偕成
 * @date 2025/12/19
 */
public class LambdaQueryBuilder<T> {
    
    private final BaseMapper<T> mapper;
    private final LambdaQueryWrapper<T> queryWrapper;
    
    public LambdaQueryBuilder(BaseMapper<T> mapper) {
        this.mapper = mapper;
        this.queryWrapper = new LambdaQueryWrapper<>();
    }
    
    /**
     * 创建查询构建器
     * 注意：对于简单查询，建议直接使用 MyBatis-Plus 的 LambdaQueryWrapper
     */
    public static <T> LambdaQueryBuilder<T> of(BaseMapper<T> mapper) {
        return new LambdaQueryBuilder<>(mapper);
    }
    
    /**
     * 获取内部的 LambdaQueryWrapper
     * 可以直接使用 MyBatis-Plus 的所有方法
     */
    public LambdaQueryWrapper<T> getWrapper() {
        return queryWrapper;
    }
    
    /**
     * 查询列表
     */
    public java.util.List<T> list() {
        return mapper.selectList(queryWrapper);
    }
    
    /**
     * 查询单个
     */
    public T one() {
        return mapper.selectOne(queryWrapper);
    }
    
    /**
     * 查询数量
     */
    public Long count() {
        return mapper.selectCount(queryWrapper);
    }
    
    /**
     * 分页查询
     */
    public PageResult<T> page(PageDTO pageDTO) {
        Page<T> page = new Page<>(pageDTO.getPageNo(), pageDTO.getPageSize());
        Page<T> result = mapper.selectPage(page, queryWrapper);
        
        return new PageResult<>(
            result.getRecords(),
            result.getTotal(),
            result.getCurrent(),
            result.getSize()
        );
    }
    
    /**
     * 分页查询 - 支持DTO查询条件（基于注解）
     */
    public static <T> PageResult<T> pageWithCondition(BaseMapper<T> mapper, PageDTO pageDTO) {
        // 构建查询条件
        QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(pageDTO);
        
        // 执行分页查询
        Page<T> page = new Page<>(pageDTO.getPageNo(), pageDTO.getPageSize());
        Page<T> result = mapper.selectPage(page, wrapper);
        
        return new PageResult<>(
            result.getRecords(),
            result.getTotal(),
            result.getCurrent(),
            result.getSize()
        );
    }
    
    /**
     * 分页查询 - 支持实体查询条件（基于注解）
     */
    public static <T> PageResult<T> pageWithCondition(BaseMapper<T> mapper, Page<T> page, T queryEntity) {
        // 构建查询条件
        QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(queryEntity);
        
        // 执行分页查询
        Page<T> result = mapper.selectPage(page, wrapper);
        
        return new PageResult<>(
            result.getRecords(),
            result.getTotal(),
            result.getCurrent(),
            result.getSize()
        );
    }
    
    /**
     * 分页查询 - 支持实体和Map额外条件
     */
    public static <T> PageResult<T> pageWithCondition(BaseMapper<T> mapper, Page<T> page, T queryEntity, Map<String, Object> extraConditions) {
        // 构建查询条件
        QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(queryEntity, extraConditions);
        
        // 执行分页查询
        Page<T> result = mapper.selectPage(page, wrapper);
        
        return new PageResult<>(
            result.getRecords(),
            result.getTotal(),
            result.getCurrent(),
            result.getSize()
        );
    }
    
    /**
     * 分页查询 - 仅使用Map条件
     */
    public static <T> PageResult<T> pageWithCondition(BaseMapper<T> mapper, Page<T> page, Map<String, Object> conditions) {
        // 构建查询条件
        QueryWrapper<T> wrapper = QueryConditionBuilder.buildQueryWrapper(conditions);
        
        // 执行分页查询
        Page<T> result = mapper.selectPage(page, wrapper);
        
        return new PageResult<>(
            result.getRecords(),
            result.getTotal(),
            result.getCurrent(),
            result.getSize()
        );
    }
    
    /**
     * 获取QueryWrapper
     * @deprecated 建议使用 getWrapper() 方法
     */
    @Deprecated
    public LambdaQueryWrapper<T> getQueryWrapper() {
        return queryWrapper;
    }
} 