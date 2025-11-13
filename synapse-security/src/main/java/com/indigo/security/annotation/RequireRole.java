package com.indigo.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 要求角色注解
 * 标记在方法或类上，表示需要指定角色才能访问
 * 
 * <p><b>使用示例：</b>
 * <pre>
 * // 要求单个角色
 * {@code @RequireRole("admin")}
 * public Result&lt;Void&gt; deleteUser(String id) {
 *     // ...
 * }
 * 
 * // 要求多个角色（OR 逻辑：任一角色即可）
 * {@code @RequireRole(value = {"admin", "super_admin"}, logical = Logical.OR)}
 * public Result&lt;Void&gt; manageSystem() {
 *     // ...
 * }
 * 
 * // 要求多个角色（AND 逻辑：所有角色都需要）
 * {@code @RequireRole(value = {"admin", "manager"}, logical = Logical.AND)}
 * public Result&lt;Void&gt; criticalOperation() {
 *     // ...
 * }
 * </pre>
 * 
 * @author 史偕成
 * @date 2025/01/XX
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    /**
     * 需要的角色列表
     * 
     * @return 角色列表
     */
    String[] value();
    
    /**
     * 逻辑运算符
     * AND：需要所有角色
     * OR：需要任一角色（默认）
     * 
     * @return 逻辑运算符
     */
    Logical logical() default Logical.OR;
    
    /**
     * 自定义错误消息
     * 
     * @return 错误消息
     */
    String message() default "权限不足";
}

