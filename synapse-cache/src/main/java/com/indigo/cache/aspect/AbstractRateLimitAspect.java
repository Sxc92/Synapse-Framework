package com.indigo.cache.aspect;

import com.indigo.cache.annotation.RateLimit;
import com.indigo.cache.extension.ratelimit.RateLimitService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.aspectj.lang.reflect.MethodSignature;
import java.lang.reflect.Method;

/**
 * 限流切面抽象基类
 * 提取公共的限流逻辑，减少代码重复
 *
 * @author 史偕成
 * @date 2024/12/19
 */
@Slf4j
@Aspect
@Order(100)
@Component
public abstract class AbstractRateLimitAspect {

    protected final RateLimitService rateLimitService;
    protected final ExpressionParser parser = new SpelExpressionParser();

    protected AbstractRateLimitAspect(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 生成限流键
        String key = generateKey(joinPoint, rateLimit);
        
        // 转换时间单位（简化处理，直接使用秒）
        long timeWindow = rateLimit.window();
        
        // 执行限流检查
        boolean allowed = rateLimitService.isAllowed(key, rateLimit.algorithm(), timeWindow, rateLimit.limit());
        
        if (!allowed) {
            log.warn("请求被限流: key={}, algorithm={}, timeWindow={}s, maxRequests={}", 
                key, rateLimit.algorithm(), timeWindow, rateLimit.limit());
            
            // 根据策略处理限流
            return handleRateLimit(rateLimit);
        }
        
        // 限流通过，继续执行原方法
        return joinPoint.proceed();
    }

    /**
     * 生成限流键
     */
    protected String generateKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String keyExpression = rateLimit.key();
        
        if (keyExpression.isEmpty()) {
            // 如果没有指定 key 表达式，使用默认的 IP + 方法名
            return getDefaultKey(joinPoint);
        }
        
        try {
            // 解析 SpEL 表达式
            Expression expression = parser.parseExpression(keyExpression);
            EvaluationContext context = createEvaluationContext(joinPoint);
            Object result = expression.getValue(context);
            return result != null ? result.toString() : getDefaultKey(joinPoint);
        } catch (Exception e) {
            log.warn("解析限流键表达式失败: {}", keyExpression, e);
            return getDefaultKey(joinPoint);
        }
    }

    /**
     * 创建表达式上下文 - 由子类实现
     */
    protected abstract EvaluationContext createEvaluationContext(ProceedingJoinPoint joinPoint);

    /**
     * 获取默认限流键 - 由子类实现
     */
    protected abstract String getDefaultKey(ProceedingJoinPoint joinPoint);

    /**
     * 获取参数名（使用Spring的参数名发现器）
     */
    protected String[] getParameterNames(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            Method method = methodSignature.getMethod();
            
            // 使用Spring的参数名发现器
            DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
            return discoverer.getParameterNames(method);
        } catch (Exception e) {
            log.warn("无法获取方法参数名: {}", e.getMessage());
            // 如果无法获取参数名，生成默认的参数名
            Object[] args = joinPoint.getArgs();
            String[] paramNames = new String[args.length];
            for (int i = 0; i < args.length; i++) {
                paramNames[i] = "arg" + i;
            }
            return paramNames;
        }
    }

    /**
     * 处理限流
     */
    protected Object handleRateLimit(RateLimit rateLimit) throws Throwable {
        return switch (rateLimit.strategy()) {
            case "REJECT" -> throw new RuntimeException(rateLimit.message());
            case "WAIT" -> {
                // 等待策略：固定延迟1秒后重试
                long waitTime = 1000; // 固定等待1秒
                log.info("限流等待策略，延迟 {}ms 后重试", waitTime);
                Thread.sleep(waitTime);
                yield null; // 返回null表示继续执行原方法
            }
            case "FALLBACK" -> {
                // 降级策略：返回默认值或执行降级逻辑
                log.info("限流降级策略，返回默认值");
                yield getFallbackResult(rateLimit);
            }
            default -> {
                log.warn("未知的限流策略: {}", rateLimit.strategy());
                throw new RuntimeException(rateLimit.message());
            }
        };
    }

    /**
     * 获取降级结果
     */
    protected Object getFallbackResult(RateLimit rateLimit) {
        // 子类可以重写此方法提供特定的降级逻辑
        return null;
    }
} 