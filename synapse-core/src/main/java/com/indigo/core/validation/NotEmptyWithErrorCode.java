package com.indigo.core.validation;

import com.indigo.core.constants.ErrorCode;
import com.indigo.core.validation.validator.NotEmptyWithErrorCodeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 自定义验证注解：非空验证（支持自定义错误码）
 * 验证失败时抛出框架的统一异常，使用指定的错误码
 * 
 * <p>使用示例：</p>
 * <pre>
 * {@code
 * @NotEmptyWithErrorCode(
 *     errorCodeEnum = IamError.class,
 *     errorCodeName = "SYSTEM_IDS_EMPTY"
 * )
 * private List<String> systemIds;
 * }
 * </pre>
 * 
 * <p><b>为什么不能直接使用 ErrorCode 接口？</b></p>
 * <p>Java 注解属性的类型必须是编译时常量，只能是：</p>
 * <ul>
 *   <li>基本类型（int, long, boolean 等）</li>
 *   <li>String</li>
 *   <li>Class 或 Class&lt;?&gt;</li>
 *   <li>枚举类型（但不能是泛型枚举，如 Enum&lt;? extends ErrorCode&gt;）</li>
 *   <li>注解类型</li>
 *   <li>以上类型的数组</li>
 * </ul>
 * <p>接口类型（如 ErrorCode）和泛型枚举类型不能作为注解属性，因为它们不是编译时常量。
 * 因此采用枚举类 + 枚举名称的方式，在运行时通过反射获取枚举值。</p>
 *
 * @author 史偕成
 * @date 2025/11/29
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = NotEmptyWithErrorCodeValidator.class)
public @interface NotEmptyWithErrorCode {

    /**
     * 错误码枚举类（必须实现 ErrorCode 接口）
     * 例如：IamError.class
     */
    Class<? extends Enum<? extends ErrorCode>> errorCodeEnum();

    /**
     * 错误码枚举名称
     * 例如："SYSTEM_IDS_EMPTY"
     */
    String errorCodeName();

    /**
     * 错误消息（可选，如果不提供则使用错误码对应的国际化消息）
     */
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

