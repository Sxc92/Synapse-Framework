package com.indigo.core.entity.dto.page;

import com.indigo.core.entity.dto.PageDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 性能优化分页DTO
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class PerformancePageDTO<T> extends PageDTO<T> {
    
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