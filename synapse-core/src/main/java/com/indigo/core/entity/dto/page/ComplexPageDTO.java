package com.indigo.core.entity.dto.page;

import com.indigo.core.entity.dto.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 复杂查询分页DTO
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ComplexPageDTO extends PageDTO {
    
    /**
     * 复杂查询SQL（当简单查询无法满足时使用）
     */
    private String customSql;
    
    /**
     * 复杂查询参数
     */
    private Map<String, Object> customSqlParams;
    
    /**
     * 是否启用复杂查询模式
     */
    private Boolean enableComplexQuery = false;
} 