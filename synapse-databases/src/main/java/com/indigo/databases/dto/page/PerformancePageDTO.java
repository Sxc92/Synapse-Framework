package com.indigo.databases.dto.page;

import com.indigo.databases.dto.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 性能优化分页DTO
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PerformancePageDTO extends PageDTO {
    
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