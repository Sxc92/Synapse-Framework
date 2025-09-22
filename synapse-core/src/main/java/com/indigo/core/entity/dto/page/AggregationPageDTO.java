package com.indigo.core.entity.dto.page;

import com.indigo.core.entity.dto.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 聚合查询分页DTO
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AggregationPageDTO extends PageDTO {
    
    /**
     * 聚合统计字段
     */
    private List<AggregationField> aggregations;
    
    /**
     * 分组字段
     */
    private List<String> groupByFields;
    
    /**
     * 聚合字段
     */
    @Data
    public static class AggregationField {
        /**
         * 字段名
         */
        private String field;
        
        /**
         * 聚合类型
         */
        private AggregationType type;
        
        /**
         * 别名
         */
        private String alias;
        
        public AggregationField() {}
        
        public AggregationField(String field, AggregationType type) {
            this.field = field;
            this.type = type;
            this.alias = type.name().toLowerCase() + "_" + field;
        }
        
        public AggregationField(String field, AggregationType type, String alias) {
            this.field = field;
            this.type = type;
            this.alias = alias;
        }
    }
    
    /**
     * 聚合类型枚举
     */
    public enum AggregationType {
        COUNT,      // 计数
        SUM,        // 求和
        AVG,        // 平均值
        MAX,        // 最大值
        MIN,        // 最小值
        COUNT_DISTINCT // 去重计数
    }
} 