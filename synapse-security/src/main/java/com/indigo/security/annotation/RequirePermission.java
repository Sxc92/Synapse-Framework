package com.indigo.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 要求权限注解
 * 标记在方法或类上，表示需要指定权限才能访问
 * 
 * <p><b>使用示例：</b>
 * <pre>
 * // 要求单个权限
 * {@code @RequirePermission("user:read")}
 * public Result&lt;UserVO&gt; getUser(String id) {
 *     // ...
 * }
 * 
 * // 要求多个权限（OR 逻辑：任一权限即可）
 * {@code @RequirePermission(value = {"user:read", "user:write"}, logical = Logical.OR)}
 * public Result&lt;Void&gt; manageUser() {
 *     // ...
 * }
 * 
 * // 要求多个权限（AND 逻辑：所有权限都需要）
 * {@code @RequirePermission(value = {"user:read", "user:write"}, logical = Logical.AND)}
 * public Result&lt;Void&gt; fullAccessUser() {
 *     // ...
 * }
 * </pre>
 * 
 * @author 史偕成
 * @date 2025/01/XX
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    /**
     * 需要的权限列表
     * 
     * @return 权限列表
     */
    String[] value();
    
    /**
     * 逻辑运算符
     * AND：需要所有权限
     * OR：需要任一权限（默认）
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

