package com.indigo.core.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * 基础分页查询DTO
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
public class PageDTO<T> extends QueryDTO<T> {

    /**
     * 当前页码，从1开始
     */
    private long pageNo = 1;

    /**
     * 每页大小
     */
    private long pageSize = 10;
}
