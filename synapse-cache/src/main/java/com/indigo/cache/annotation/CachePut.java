package com.indigo.cache.annotation;

import com.indigo.cache.core.TwoLevelCacheService;

import java.lang.annotation.*;

/**
 * 缓存更新注解，用于方法级别的缓存更新配置
 * 配合TwoLevelCacheService使用，无论方法是否返回null都会更新缓存
 *
 * @author 史偕成
 * @date 2025/01/08
 * @see TwoLevelCacheService.CacheStrategy
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CachePut {

    /**
     * 缓存模块名，用于区分不同业务领域的缓存
     */
    String module() default "";

    /**
     * 缓存键名，支持SpEL表达式，如：#user.id
     */
    String key();

    /**
     * 缓存过期时间（秒）
     */
    long expireSeconds() default 3600;

    /**
     * 缓存策略
     */
    TwoLevelCacheService.CacheStrategy strategy() default TwoLevelCacheService.CacheStrategy.LOCAL_AND_REDIS;

    /**
     * 是否在发生异常时禁用缓存操作
     */
    boolean disableOnException() default true;
    
    /**
     * 缓存条件，支持SpEL表达式，如：#result != null
     * 只有满足条件时才更新缓存
     */
    String condition() default "";
    
    /**
     * 是否在方法执行前更新缓存
     * true: 在方法执行前更新缓存（使用参数值）
     * false: 在方法执行后更新缓存（使用返回值）
     */
    boolean beforeInvocation() default false;
} 