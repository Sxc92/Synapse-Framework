package com.indigo.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * VO映射注解
 * 用于标记VO类与数据库表的映射关系
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface VoMapping {
    
    /**
     * 主表名
     */
    String table() default "";
    
    /**
     * 主表别名
     */
    String alias() default "t";
    
    /**
     * 关联表配置
     */
    Join[] joins() default {};
    
    /**
     * 字段映射配置
     */
    Field[] fields() default {};
    
    /**
     * 关联表配置
     */
    @interface Join {
        /**
         * 关联表名
         */
        String table();
        
        /**
         * 关联表别名
         */
        String alias();
        
        /**
         * 关联类型
         */
        JoinType type() default JoinType.LEFT;
        
        /**
         * 关联条件
         */
        String on();
    }
    
    /**
     * 字段映射配置
     */
    @interface Field {
        /**
         * 数据库字段（支持表别名.字段名）
         */
        String source();
        
        /**
         * VO字段名
         */
        String target() default "";
        
        /**
         * 字段类型
         */
        FieldType type() default FieldType.DIRECT;
        
        /**
         * 自定义SQL表达式（用于计算字段）
         */
        String expression() default "";
    }
    
    /**
     * 关联类型
     */
    enum JoinType {
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
    
    /**
     * 字段类型
     */
    enum FieldType {
        DIRECT,        // 直接映射
        EXPRESSION,    // 表达式计算
        ALIAS          // 别名映射
    }
}
