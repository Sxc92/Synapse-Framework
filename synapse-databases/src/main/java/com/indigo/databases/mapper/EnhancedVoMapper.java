package com.indigo.databases.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.indigo.core.entity.dto.PageDTO;
import com.indigo.core.entity.dto.QueryDTO;
import com.indigo.core.entity.vo.BaseVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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
     */
    @Select("${sql}")
    IPage<V> selectPageAsVo(Page<V> page, @Param("sql") String sql);
    
    /**
     * 列表查询 - 直接映射到VO（支持多表关联）
     */
    @Select("${sql}")
    List<V> selectListAsVo(@Param("sql") String sql);
    
    /**
     * 单个查询 - 直接映射到VO（支持多表关联）
     */
    @Select("${sql}")
    V selectOneAsVo(@Param("sql") String sql);
    
    /**
     * 统计查询
     */
    @Select("${sql}")
    Long selectCountAsVo(@Param("sql") String sql);
}
