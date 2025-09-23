package com.indigo.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段映射注解
 * 用于指定VO字段与数据库字段的映射关系
 * 
 * <p>使用示例：</p>
 * <pre>
 * public class UserVO extends BaseVO {
 *     @FieldMapping("user_name")
 *     private String userName;
 *     
 *     @FieldMapping("last_login_time")
 *     private LocalDateTime lastDateTime;
 * }
 * </pre>
 * 
 * @author 史偕成
 * @date 2025/12/19
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldMapping {
    
    /**
     * 数据库字段名
     * 如果不指定，则使用默认的驼峰转下划线规则
     */
    String value() default "";
    
    /**
     * 是否忽略该字段的映射
     * 当设置为true时，该字段不会被映射
     */
    boolean ignore() default false;
}