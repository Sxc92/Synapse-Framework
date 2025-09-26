package com.indigo.core.entity.dto.page;

import com.indigo.core.entity.dto.PageDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * 复杂查询分页DTO
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
public class ComplexPageDTO<T> extends PageDTO<T> {
    
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