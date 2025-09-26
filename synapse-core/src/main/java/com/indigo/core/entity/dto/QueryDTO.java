package com.indigo.core.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.List;

/**
 * 基础查询DTO
 * 用于非分页查询，只包含查询条件和排序
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Data
@SuperBuilder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class QueryDTO<T> implements Serializable {

    private T id;

    /**
     * 排序字段列表
     */
    private List<OrderBy> orderByList;

    /**
     * 排序字段
     */
    @Data
    public static class OrderBy {
        /**
         * 字段名
         */
        private String field;

        /**
         * 排序方向：ASC, DESC
         */
        private String direction = "ASC";

        public OrderBy() {
        }

        public OrderBy(String field) {
            this.field = field;
        }

        public OrderBy(String field, String direction) {
            this.field = field;
            this.direction = direction;
        }
    }
}
