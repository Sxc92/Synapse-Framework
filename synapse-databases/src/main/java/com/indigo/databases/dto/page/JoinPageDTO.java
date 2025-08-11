package com.indigo.databases.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * 多表关联分页DTO
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JoinPageDTO extends PageDTO {
    
    /**
     * 多表关联配置
     */
    private List<TableJoin> tableJoins;
    
    /**
     * 表关联配置
     */
    @Data
    public static class TableJoin {
        /**
         * 关联表名
         */
        private String tableName;
        
        /**
         * 关联表别名
         */
        private String tableAlias;
        
        /**
         * 关联类型：INNER, LEFT, RIGHT, FULL
         */
        private JoinType joinType = JoinType.INNER;
        
        /**
         * 关联条件
         */
        private String joinCondition;
        
        /**
         * 关联表的选择字段
         */
        private List<String> selectFields;
        
        public TableJoin() {}
        
        public TableJoin(String tableName, String tableAlias, JoinType joinType, String joinCondition) {
            this.tableName = tableName;
            this.tableAlias = tableAlias;
            this.joinType = joinType;
            this.joinCondition = joinCondition;
        }
    }
    
    /**
     * 关联类型
     */
    public enum JoinType {
        INNER("INNER JOIN"),
        LEFT("LEFT JOIN"),
        RIGHT("RIGHT JOIN"),
        FULL("FULL JOIN");
        
        private final String sqlKeyword;
        
        JoinType(String sqlKeyword) {
            this.sqlKeyword = sqlKeyword;
        }
        
        public String getSqlKeyword() {
            return sqlKeyword;
        }
    }
} 