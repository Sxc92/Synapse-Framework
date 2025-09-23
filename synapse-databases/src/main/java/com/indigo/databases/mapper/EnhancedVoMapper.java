package com.indigo.databases.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.indigo.core.entity.dto.PageDTO;
import com.indigo.core.entity.dto.QueryDTO;
import com.indigo.core.entity.vo.BaseVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 增强VO映射Mapper
 * 支持多表关联查询和直接VO映射
 *
 * @author 史偕成
 * @date 2025/12/19
 */
public interface EnhancedVoMapper<T, V extends BaseVO> extends BaseMapper<T> {
    
    /**
     * 分页查询 - 直接映射到VO（支持多表关联）
     * 返回Map类型，需要手动映射
     */
    @Select("${param1}")
    IPage<Map<String, Object>> selectPageAsVo(Page<Map<String, Object>> page, @Param("param1") String sql);
    
    /**
     * 列表查询 - 直接映射到VO（支持多表关联）
     * 使用动态SQL避免参数绑定问题
     */
    @Select("${sql}")
    List<V> selectListAsVo(@Param("sql") String sql);
    
    /**
     * 列表查询 - 使用动态SQL（支持多表关联）
     * 用于复杂多表查询
     */
    @Select("${sql}")
    List<V> selectListAsVoWithSql(@Param("sql") String sql);
    
    /**
     * 单个查询 - 直接映射到VO（支持多表关联）
     * 使用动态SQL避免参数绑定问题
     */
    @Select("${sql}")
    V selectOneAsVo(@Param("sql") String sql);
    
    /**
     * 单个查询 - 使用动态SQL（支持多表关联）
     * 用于复杂多表查询
     */
    @Select("${sql}")
    V selectOneAsVoWithSql(@Param("sql") String sql);
    
    /**
     * 统计查询
     */
    @Select("${param1}")
    Long selectCountAsVo(@Param("param1") String sql);
}
