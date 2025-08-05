package com.indigo.cache.annotation;

import java.lang.annotation.*;

/**
 * 缓存组合注解，用于在一个方法上组合多个缓存操作
 * 可以同时执行缓存、更新缓存、删除缓存等操作
 *
 * @author 史偕成
 * @date 2025/01/08
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Caching {

    /**
     * 缓存操作数组
     */
    Cacheable[] cacheable() default {};

    /**
     * 缓存更新操作数组
     */
    CachePut[] put() default {};

    /**
     * 缓存删除操作数组
     */
    CacheEvict[] evict() default {};
} 