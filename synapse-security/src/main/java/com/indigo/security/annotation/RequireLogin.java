package com.indigo.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 要求登录注解
 * 标记在方法或类上，表示需要用户登录才能访问
 * 
 * <p><b>使用示例：</b>
 * <pre>
 * // 方法级使用
 * {@code @RequireLogin}
 * public Result&lt;UserVO&gt; getProfile() {
 *     // ...
 * }
 * 
 * // 类级使用（所有方法都需要登录）
 * {@code @RequireLogin}
 * {@code @RestController}
 * public class UserController {
 *     // ...
 * }
 * </pre>
 * 
 * @author 史偕成
 * @date 2025/01/XX
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireLogin {
    /**
     * 自定义错误消息
     * 
     * @return 错误消息
     */
    String message() default "需要登录";
}

