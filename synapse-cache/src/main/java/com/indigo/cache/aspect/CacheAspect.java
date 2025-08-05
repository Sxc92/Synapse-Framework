package com.indigo.cache.aspect;

import com.indigo.cache.annotation.CacheEvict;
import com.indigo.cache.annotation.Cacheable;
import com.indigo.cache.annotation.CachePut;
import com.indigo.cache.annotation.Caching;
import com.indigo.cache.core.TwoLevelCacheService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * 缓存切面，处理缓存注解
 *
 * @author 史偕成
 * @date 2025/05/16 10:20
 */
@Aspect
@Component
public class CacheAspect {

    private final TwoLevelCacheService cacheService;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public CacheAspect(TwoLevelCacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * 处理Cacheable注解
     */
    @Around("@annotation(com.indigo.cache.annotation.Cacheable)")
    public Object cacheableAround(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Cacheable cacheable = method.getAnnotation(Cacheable.class);

        // 获取模块名和键值
        String module = getModule(cacheable.module(), method);
        String spelKey = cacheable.key();
        long expireSeconds = cacheable.expireSeconds();
        TwoLevelCacheService.CacheStrategy strategy = cacheable.strategy();
        
        // 解析SpEL表达式的缓存键
        String key = parseKey(spelKey, method, joinPoint.getArgs());
        
        try {
            // 从缓存获取数据
            Optional<?> cachedResult = cacheService.get(module, key, strategy);
            
            // 缓存命中，直接返回
            if (cachedResult.isPresent()) {
                return cachedResult.get();
            }
            
            // 调用原方法
            Object result = joinPoint.proceed();
            
            // 检查缓存条件
            if (shouldCache(cacheable.condition(), result, method, joinPoint.getArgs())) {
                // 将结果存入缓存
                if (result != null) {
                    cacheService.save(module, key, result, expireSeconds, strategy);
                }
            }
            
            return result;
        } catch (Exception e) {
            if (cacheable.disableOnException()) {
                // 异常发生时禁用缓存，直接调用原方法
                return joinPoint.proceed();
            }
            throw e;
        }
    }

    /**
     * 处理CacheEvict注解
     */
    @Around("@annotation(com.indigo.cache.annotation.CacheEvict)")
    public Object cacheEvictAround(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        CacheEvict cacheEvict = method.getAnnotation(CacheEvict.class);

        // 获取模块名和键值
        String module = getModule(cacheEvict.module(), method);
        String spelKey = cacheEvict.key();
        TwoLevelCacheService.CacheStrategy strategy = cacheEvict.strategy();
        boolean allEntries = cacheEvict.allEntries();
        boolean beforeInvocation = cacheEvict.beforeInvocation();
        
        // 解析SpEL表达式的缓存键
        String key = parseKey(spelKey, method, joinPoint.getArgs());
        
        try {
            // 如果是在方法执行前清除缓存
            if (beforeInvocation) {
                evictCache(module, key, allEntries, strategy);
            }
            
            // 调用原方法
            Object result = joinPoint.proceed();
            
            // 如果是在方法执行后清除缓存
            if (!beforeInvocation) {
                // 检查清除条件
                if (shouldEvict(cacheEvict.condition(), result, method, joinPoint.getArgs())) {
                    evictCache(module, key, allEntries, strategy);
                }
            }
            
            return result;
        } catch (Exception e) {
            if (cacheEvict.disableOnException()) {
                // 异常发生时禁用缓存操作，直接调用原方法
                return joinPoint.proceed();
            }
            throw e;
        }
    }

    /**
     * 处理CachePut注解
     */
    @Around("@annotation(com.indigo.cache.annotation.CachePut)")
    public Object cachePutAround(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        CachePut cachePut = method.getAnnotation(CachePut.class);

        // 获取模块名和键值
        String module = getModule(cachePut.module(), method);
        String spelKey = cachePut.key();
        long expireSeconds = cachePut.expireSeconds();
        TwoLevelCacheService.CacheStrategy strategy = cachePut.strategy();
        boolean beforeInvocation = cachePut.beforeInvocation();
        
        // 解析SpEL表达式的缓存键
        String key = parseKey(spelKey, method, joinPoint.getArgs());
        
        try {
            Object result;
            
            if (beforeInvocation) {
                // 在方法执行前更新缓存（使用参数值）
                Object paramValue = getParameterValue(spelKey, method, joinPoint.getArgs());
                if (shouldCache(cachePut.condition(), paramValue, method, joinPoint.getArgs())) {
                    cacheService.save(module, key, paramValue, expireSeconds, strategy);
                }
                result = joinPoint.proceed();
            } else {
                // 在方法执行后更新缓存（使用返回值）
                result = joinPoint.proceed();
                if (shouldCache(cachePut.condition(), result, method, joinPoint.getArgs())) {
                    cacheService.save(module, key, result, expireSeconds, strategy);
                }
            }
            
            return result;
        } catch (Exception e) {
            if (cachePut.disableOnException()) {
                // 异常发生时禁用缓存操作，直接调用原方法
                return joinPoint.proceed();
            }
            throw e;
        }
    }

    /**
     * 处理Caching注解
     */
    @Around("@annotation(com.indigo.cache.annotation.Caching)")
    public Object cachingAround(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Caching caching = method.getAnnotation(Caching.class);

        try {
            // 处理Cacheable操作（在方法执行前）
            for (Cacheable cacheable : caching.cacheable()) {
                handleCacheable(cacheable, method, joinPoint.getArgs());
            }
            
            // 处理CachePut操作（根据beforeInvocation决定时机）
            for (CachePut cachePut : caching.put()) {
                if (cachePut.beforeInvocation()) {
                    handleCachePut(cachePut, method, joinPoint.getArgs(), null);
                }
            }
            
            // 处理CacheEvict操作（根据beforeInvocation决定时机）
            for (CacheEvict cacheEvict : caching.evict()) {
                if (cacheEvict.beforeInvocation()) {
                    handleCacheEvict(cacheEvict, method, joinPoint.getArgs());
                }
            }
            
            // 调用原方法
            Object result = joinPoint.proceed();
            
            // 处理CachePut操作（方法执行后）
            for (CachePut cachePut : caching.put()) {
                if (!cachePut.beforeInvocation()) {
                    handleCachePut(cachePut, method, joinPoint.getArgs(), result);
                }
            }
            
            // 处理CacheEvict操作（方法执行后）
            for (CacheEvict cacheEvict : caching.evict()) {
                if (!cacheEvict.beforeInvocation()) {
                    handleCacheEvict(cacheEvict, method, joinPoint.getArgs());
                }
            }
            
            return result;
        } catch (Exception e) {
            // 异常处理，直接调用原方法
            return joinPoint.proceed();
        }
    }
    
    /**
     * 清除缓存
     */
    private void evictCache(String module, String key, boolean allEntries, TwoLevelCacheService.CacheStrategy strategy) {
        if (allEntries) {
            // 清除模块下所有缓存
            // 这里可以根据实际情况实现清除模块所有缓存的逻辑
        } else {
            // 清除指定键的缓存
            cacheService.delete(module, key, strategy);
        }
    }
    
    /**
     * 获取模块名，如果注解中未指定，则使用方法所在类的简单名称
     */
    private String getModule(String annotationModule, Method method) {
        if (StringUtils.hasText(annotationModule)) {
            return annotationModule;
        }
        return method.getDeclaringClass().getSimpleName();
    }
    
    /**
     * 解析SpEL表达式的缓存键
     */
    private String parseKey(String spelKey, Method method, Object[] args) {
        if (!spelKey.contains("#")) {
            // 不包含SpEL表达式，直接返回
            return spelKey;
        }
        
        EvaluationContext context = createEvaluationContext(method, args);
        Expression expression = expressionParser.parseExpression(spelKey);
        return expression.getValue(context, String.class);
    }
    
    /**
     * 创建SpEL表达式的评估上下文
     */
    private EvaluationContext createEvaluationContext(Method method, Object[] args) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        // 获取方法参数名
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        if (parameterNames != null && args != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }
        
        return context;
    }
    
    /**
     * 判断是否应该缓存结果
     */
    private boolean shouldCache(String condition, Object result, Method method, Object[] args) {
        if (!StringUtils.hasText(condition)) {
            return true;
        }
        
        EvaluationContext context = createEvaluationContext(method, args);
        context.setVariable("result", result);
        Expression expression = expressionParser.parseExpression(condition);
        return Boolean.TRUE.equals(expression.getValue(context, Boolean.class));
    }
    
    /**
     * 判断是否应该清除缓存
     */
    private boolean shouldEvict(String condition, Object result, Method method, Object[] args) {
        if (!StringUtils.hasText(condition)) {
            return true;
        }
        
        EvaluationContext context = createEvaluationContext(method, args);
        context.setVariable("result", result);
        Expression expression = expressionParser.parseExpression(condition);
        return Boolean.TRUE.equals(expression.getValue(context, Boolean.class));
    }
    
    /**
     * 处理Cacheable操作
     */
    private void handleCacheable(Cacheable cacheable, Method method, Object[] args) {
        String module = getModule(cacheable.module(), method);
        String key = parseKey(cacheable.key(), method, args);
        
        // 从缓存获取数据，如果命中则直接返回
        Optional<?> cachedResult = cacheService.get(module, key, cacheable.strategy());
        if (cachedResult.isPresent()) {
            // 这里需要特殊处理，因为Cacheable通常需要返回值
            // 在Caching注解中，我们主要关注CachePut和CacheEvict
        }
    }
    
    /**
     * 处理CachePut操作
     */
    private void handleCachePut(CachePut cachePut, Method method, Object[] args, Object result) {
        String module = getModule(cachePut.module(), method);
        String key = parseKey(cachePut.key(), method, args);
        long expireSeconds = cachePut.expireSeconds();
        TwoLevelCacheService.CacheStrategy strategy = cachePut.strategy();
        
        Object valueToCache = result;
        if (result == null) {
            // 如果result为null，说明是beforeInvocation，使用参数值
            valueToCache = getParameterValue(cachePut.key(), method, args);
        }
        
        if (shouldCache(cachePut.condition(), valueToCache, method, args)) {
            cacheService.save(module, key, valueToCache, expireSeconds, strategy);
        }
    }
    
    /**
     * 处理CacheEvict操作
     */
    private void handleCacheEvict(CacheEvict cacheEvict, Method method, Object[] args) {
        String module = getModule(cacheEvict.module(), method);
        String key = parseKey(cacheEvict.key(), method, args);
        TwoLevelCacheService.CacheStrategy strategy = cacheEvict.strategy();
        boolean allEntries = cacheEvict.allEntries();
        
        evictCache(module, key, allEntries, strategy);
    }
    
    /**
     * 获取参数值（用于CachePut的beforeInvocation）
     */
    private Object getParameterValue(String spelKey, Method method, Object[] args) {
        if (!spelKey.contains("#")) {
            return null;
        }
        
        EvaluationContext context = createEvaluationContext(method, args);
        Expression expression = expressionParser.parseExpression(spelKey);
        return expression.getValue(context);
    }
} 