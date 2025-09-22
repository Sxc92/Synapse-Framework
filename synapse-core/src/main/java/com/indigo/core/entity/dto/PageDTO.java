package com.indigo.core.entity.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 基础分页查询DTO
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PageDTO extends QueryDTO {
    
    /**
     * 当前页码，从1开始
     */
    private long pageNo = 1;
    
    /**
     * 每页大小
     */
    private long pageSize = 10;
}
