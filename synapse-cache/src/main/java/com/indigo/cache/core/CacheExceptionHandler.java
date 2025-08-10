package com.indigo.cache.core;

import com.indigo.cache.config.CacheProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 缓存异常处理服务
 * 统一处理缓存相关的异常，支持重试、降级等策略
 *
 * @author 史偕成
 * @date 2025/01/08
 */
@Slf4j
@Component
public class CacheExceptionHandler {

    private final CacheProperties cacheProperties;
    private final AtomicLong totalExceptions = new AtomicLong(0);
    private final AtomicLong handledExceptions = new AtomicLong(0);

    @Autowired
    public CacheExceptionHandler(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
    }

    /**
     * 执行带异常处理的操作
     *
     * @param operation 操作
     * @param <T>       返回值类型
     * @return 操作结果
     */
    public <T> T executeWithExceptionHandling(Supplier<T> operation) {
        return executeWithExceptionHandling(operation, null);
    }

    /**
     * 执行带异常处理的操作
     *
     * @param operation 操作
     * @param fallback  降级操作
     * @param <T>       返回值类型
     * @return 操作结果
     */
    public <T> T executeWithExceptionHandling(Supplier<T> operation, Supplier<T> fallback) {
        if (!cacheProperties.getExceptionHandling().isEnabled()) {
            try {
                return operation.get();
            } catch (Exception e) {
                logException(e);
                throw e;
            }
        }

        int retryCount = cacheProperties.getExceptionHandling().getRetryCount();
        long retryInterval = cacheProperties.getExceptionHandling().getRetryInterval().toMillis();

        for (int attempt = 0; attempt <= retryCount; attempt++) {
            try {
                T result = operation.get();
                if (attempt > 0) {
                    log.info("操作在第 {} 次重试后成功", attempt);
                }
                return result;
            } catch (Exception e) {
                totalExceptions.incrementAndGet();
                
                if (attempt < retryCount) {
                    // 只在第一次和每3次重试时记录警告，避免日志过多
                    if (attempt == 0 || (attempt + 1) % 3 == 0) {
                        log.warn("操作失败，准备第 {} 次重试: {}", attempt + 1, e.getMessage());
                    }
                    try {
                        Thread.sleep(retryInterval);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("重试等待被中断", ie);
                        break;
                    }
                } else {
                    logException(e);
                    return handleFallback(fallback, e);
                }
            }
        }

        return handleFallback(fallback, new RuntimeException("操作失败，已重试 " + retryCount + " 次"));
    }

    /**
     * 执行带异常处理的操作（无返回值）
     *
     * @param operation 操作
     */
    public void executeWithExceptionHandling(Runnable operation) {
        executeWithExceptionHandling(operation, null);
    }

    /**
     * 执行带异常处理的操作（无返回值）
     *
     * @param operation 操作
     * @param fallback  降级操作
     */
    public void executeWithExceptionHandling(Runnable operation, Runnable fallback) {
        if (!cacheProperties.getExceptionHandling().isEnabled()) {
            try {
                operation.run();
            } catch (Exception e) {
                logException(e);
                throw e;
            }
            return;
        }

        int retryCount = cacheProperties.getExceptionHandling().getRetryCount();
        long retryInterval = cacheProperties.getExceptionHandling().getRetryInterval().toMillis();

        for (int attempt = 0; attempt <= retryCount; attempt++) {
            try {
                operation.run();
                if (attempt > 0) {
                    log.info("操作在第 {} 次重试后成功", attempt);
                }
                return;
            } catch (Exception e) {
                totalExceptions.incrementAndGet();
                
                if (attempt < retryCount) {
                    // 只在第一次和每3次重试时记录警告，避免日志过多
                    if (attempt == 0 || (attempt + 1) % 3 == 0) {
                        log.warn("操作失败，准备第 {} 次重试: {}", attempt + 1, e.getMessage());
                    }
                    try {
                        Thread.sleep(retryInterval);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("重试等待被中断", ie);
                        break;
                    }
                } else {
                    logException(e);
                    handleFallback(fallback, e);
                    return;
                }
            }
        }

        handleFallback(fallback, new RuntimeException("操作失败，已重试 " + retryCount + " 次"));
    }

    /**
     * 处理降级操作
     *
     * @param fallback 降级操作
     * @param exception 异常
     * @param <T>       返回值类型
     * @return 降级结果
     */
    private <T> T handleFallback(Supplier<T> fallback, Exception exception) {
        if (fallback != null) {
            try {
                log.info("执行降级操作");
                T result = fallback.get();
                handledExceptions.incrementAndGet();
                return result;
            } catch (Exception e) {
                log.error("降级操作也失败了", e);
            }
        }

        // 根据配置决定是否抛出异常
        if (cacheProperties.getExceptionHandling().isThrowExceptions()) {
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            } else {
                throw new RuntimeException("缓存操作失败", exception);
            }
        } else {
            log.warn("缓存操作失败，返回null: {}", exception.getMessage());
            return null;
        }
    }

    /**
     * 处理降级操作（无返回值）
     *
     * @param fallback  降级操作
     * @param exception 异常
     */
    private void handleFallback(Runnable fallback, Exception exception) {
        if (fallback != null) {
            try {
                log.info("执行降级操作");
                fallback.run();
                handledExceptions.incrementAndGet();
                return;
            } catch (Exception e) {
                log.error("降级操作也失败了", e);
            }
        }

        // 根据配置决定是否抛出异常
        if (cacheProperties.getExceptionHandling().isThrowExceptions()) {
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            } else {
                throw new RuntimeException("缓存操作失败", exception);
            }
        } else {
            log.warn("缓存操作失败: {}", exception.getMessage());
        }
    }

    /**
     * 记录异常
     *
     * @param exception 异常
     */
    private void logException(Exception exception) {
        if (cacheProperties.getExceptionHandling().isLogExceptions()) {
            log.error("缓存操作异常: {}", exception.getMessage(), exception);
        }
    }

    /**
     * 获取异常统计信息
     *
     * @return 异常统计信息
     */
    public ExceptionStatistics getExceptionStatistics() {
        return ExceptionStatistics.builder()
            .totalExceptions(totalExceptions.get())
            .handledExceptions(handledExceptions.get())
            .exceptionHandlingEnabled(cacheProperties.getExceptionHandling().isEnabled())
            .logExceptions(cacheProperties.getExceptionHandling().isLogExceptions())
            .throwExceptions(cacheProperties.getExceptionHandling().isThrowExceptions())
            .retryCount(cacheProperties.getExceptionHandling().getRetryCount())
            .retryInterval(cacheProperties.getExceptionHandling().getRetryInterval())
            .fallbackStrategy(cacheProperties.getExceptionHandling().getFallbackStrategy())
            .build();
    }

    /**
     * 重置异常统计
     */
    public void resetStatistics() {
        totalExceptions.set(0);
        handledExceptions.set(0);
        log.info("缓存异常统计已重置");
    }

    /**
     * 异常统计信息
     */
    public static class ExceptionStatistics {
        private final long totalExceptions;
        private final long handledExceptions;
        private final boolean exceptionHandlingEnabled;
        private final boolean logExceptions;
        private final boolean throwExceptions;
        private final int retryCount;
        private final java.time.Duration retryInterval;
        private final String fallbackStrategy;

        public ExceptionStatistics(long totalExceptions, long handledExceptions, boolean exceptionHandlingEnabled,
                                  boolean logExceptions, boolean throwExceptions, int retryCount,
                                  java.time.Duration retryInterval, String fallbackStrategy) {
            this.totalExceptions = totalExceptions;
            this.handledExceptions = handledExceptions;
            this.exceptionHandlingEnabled = exceptionHandlingEnabled;
            this.logExceptions = logExceptions;
            this.throwExceptions = throwExceptions;
            this.retryCount = retryCount;
            this.retryInterval = retryInterval;
            this.fallbackStrategy = fallbackStrategy;
        }

        public static Builder builder() {
            return new Builder();
        }

        public long getTotalExceptions() { return totalExceptions; }
        public long getHandledExceptions() { return handledExceptions; }
        public boolean isExceptionHandlingEnabled() { return exceptionHandlingEnabled; }
        public boolean isLogExceptions() { return logExceptions; }
        public boolean isThrowExceptions() { return throwExceptions; }
        public int getRetryCount() { return retryCount; }
        public java.time.Duration getRetryInterval() { return retryInterval; }
        public String getFallbackStrategy() { return fallbackStrategy; }

        public static class Builder {
            private long totalExceptions;
            private long handledExceptions;
            private boolean exceptionHandlingEnabled;
            private boolean logExceptions;
            private boolean throwExceptions;
            private int retryCount;
            private java.time.Duration retryInterval;
            private String fallbackStrategy;

            public Builder totalExceptions(long totalExceptions) {
                this.totalExceptions = totalExceptions;
                return this;
            }

            public Builder handledExceptions(long handledExceptions) {
                this.handledExceptions = handledExceptions;
                return this;
            }

            public Builder exceptionHandlingEnabled(boolean exceptionHandlingEnabled) {
                this.exceptionHandlingEnabled = exceptionHandlingEnabled;
                return this;
            }

            public Builder logExceptions(boolean logExceptions) {
                this.logExceptions = logExceptions;
                return this;
            }

            public Builder throwExceptions(boolean throwExceptions) {
                this.throwExceptions = throwExceptions;
                return this;
            }

            public Builder retryCount(int retryCount) {
                this.retryCount = retryCount;
                return this;
            }

            public Builder retryInterval(java.time.Duration retryInterval) {
                this.retryInterval = retryInterval;
                return this;
            }

            public Builder fallbackStrategy(String fallbackStrategy) {
                this.fallbackStrategy = fallbackStrategy;
                return this;
            }

            public ExceptionStatistics build() {
                return new ExceptionStatistics(totalExceptions, handledExceptions, exceptionHandlingEnabled,
                    logExceptions, throwExceptions, retryCount, retryInterval, fallbackStrategy);
            }
        }
    }
} 