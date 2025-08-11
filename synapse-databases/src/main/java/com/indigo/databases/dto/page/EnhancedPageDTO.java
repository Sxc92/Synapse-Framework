package com.indigo.databases.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 增强分页DTO - 组合聚合查询和性能优化功能
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EnhancedPageDTO extends AggregationPageDTO {
    
    /**
     * 是否显示执行计划
     */
    private Boolean explain = false;
    
    /**
     * 指定返回字段
     */
    private List<String> selectFields;
    
    /**
     * 结果类型：ENTITY, DTO, MAP, VO
     */
    private String resultType = "ENTITY";
    
    /**
     * 是否使用缓存
     */
    private Boolean useCache = false;
    
    /**
     * 缓存时间（秒）
     */
    private Integer cacheTimeout = 300;
} 