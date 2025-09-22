package com.indigo.databases.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * IDE友好的Repository注解
 * 帮助IDE识别@AutoRepository接口为Spring Bean
 *
 * @author 史偕成
 * @date 2025/01/27
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface IdeFriendlyRepository {
    String value() default "";
}
