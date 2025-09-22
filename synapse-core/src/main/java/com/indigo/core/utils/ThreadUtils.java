package com.indigo.core.utils;

import com.indigo.core.exception.Ex;
import com.indigo.core.constants.StandardErrorCode;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Modern thread utility class for JDK17+
 * Provides convenient methods using virtual threads and structured concurrency
 * 
 * @author 史偕成
 * @date 2025/08/11 12:41:56
 **/
@Slf4j
@Component
@SuppressWarnings("unchecked")
public class ThreadUtils {

    private final ThreadPoolTaskExecutor ioThreadPool;
    private final ThreadPoolTaskExecutor cpuThreadPool;
    private final ScheduledExecutorService scheduledThreadPool;
    private final ThreadPoolTaskExecutor commonThreadPool;
    private final ThreadPoolTaskExecutor monitorThreadPool;

    public ThreadUtils(
            @Qualifier("ioThreadPool") ThreadPoolTaskExecutor ioThreadPool,
            @Qualifier("cpuThreadPool") ThreadPoolTaskExecutor cpuThreadPool,
            @Qualifier("scheduledThreadPool") ScheduledExecutorService scheduledThreadPool,
            @Qualifier("commonThreadPool") ThreadPoolTaskExecutor commonThreadPool,
            @Qualifier("monitorThreadPool") ThreadPoolTaskExecutor monitorThreadPool) {
        this.ioThreadPool = ioThreadPool;
        this.cpuThreadPool = cpuThreadPool;
        this.scheduledThreadPool = scheduledThreadPool;
        this.commonThreadPool = commonThreadPool;
        this.monitorThreadPool = monitorThreadPool;
    }

    // ==================== IO密集型任务 ====================
    // 适用于：网络请求、文件操作、数据库查询等

    /**
     * 执行IO密集型任务（使用虚拟线程）
     * 适用于网络请求、文件操作、数据库查询等
     */
    public void executeIoTask(Runnable task) {
        if (task == null) {
            Ex.throwEx(StandardErrorCode.PARAM_ERROR, "Task cannot be null");
        }
        // IO任务使用虚拟线程，避免阻塞平台线程
        if (isVirtualThreadSupported()) {
            try {
                // 使用反射调用Java 19+的startVirtualThread方法
                java.lang.reflect.Method startVirtualThreadMethod = Thread.class.getMethod("startVirtualThread", Runnable.class);
                startVirtualThreadMethod.invoke(null, wrapRunnable(task));
            } catch (Exception e) {
                log.debug("Virtual threads not supported, falling back to platform threads", e);
                // 回退到平台线程
                new Thread(wrapRunnable(task), "io-task-" + System.currentTimeMillis()).start();
            }
        } else {
            // 回退到平台线程
            new Thread(wrapRunnable(task), "io-task-" + System.currentTimeMillis()).start();
        }
    }

    /**
     * 提交IO密集型任务并返回Future（使用虚拟线程）
     */
    public <T> Future<T> submitIoTask(Callable<T> task) {
        if (task == null) {
            Ex.throwEx(StandardErrorCode.PARAM_ERROR, "Task cannot be null");
        }
        // 使用虚拟线程执行IO任务
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, createVirtualThreadExecutor());
    }

    /**
     * 批量执行IO任务（使用结构化并发或回退到CompletableFuture）
     */
    public <T> CompletableFuture<Void> executeIoTasks(Iterable<Callable<T>> tasks) {
        if (tasks == null) {
            Ex.throwEx(StandardErrorCode.PARAM_ERROR, "Tasks cannot be null");
        }
        
        // 尝试使用Java 19+的结构化并发
        if (isStructuredConcurrencySupported()) {
            try {
                return executeWithStructuredConcurrency(tasks);
            } catch (Exception e) {
                log.debug("Structured concurrency not supported, falling back to CompletableFuture", e);
            }
        }
        
        // 回退到CompletableFuture
        return executeWithCompletableFuture(tasks);
    }
    
    /**
     * 使用结构化并发执行任务（Java 19+）
     */
    private <T> CompletableFuture<Void> executeWithStructuredConcurrency(Iterable<Callable<T>> tasks) throws Exception {
        // 使用反射调用StructuredTaskScope
        Class<?> scopeClass = Class.forName("java.util.concurrent.StructuredTaskScope");
        Class<?> shutdownOnFailureClass = Class.forName("java.util.concurrent.StructuredTaskScope$ShutdownOnFailure");
        
        try (var scope = (AutoCloseable) shutdownOnFailureClass.getConstructor().newInstance()) {
            var futures = new java.util.ArrayList<Future<T>>();
            
            for (Callable<T> task : tasks) {
                // 调用scope.fork方法
                java.lang.reflect.Method forkMethod = scopeClass.getMethod("fork", Callable.class);
                futures.add((Future<T>) forkMethod.invoke(scope, (Callable<T>) () -> {
                    try {
                        return task.call();
                    } catch (Exception e) {
                        log.error("IO task execution failed", e);
                        throw new RuntimeException(e);
                    }
                }));
            }
            
            // 调用scope.join方法
            java.lang.reflect.Method joinMethod = scopeClass.getMethod("join");
            joinMethod.invoke(scope);
            
            // 调用scope.throwIfFailed方法
            java.lang.reflect.Method throwIfFailedMethod = scopeClass.getMethod("throwIfFailed");
            throwIfFailedMethod.invoke(scope);
            
            return CompletableFuture.completedFuture(null);
        }
    }
    
    /**
     * 使用CompletableFuture执行任务（Java 17兼容）
     */
    private <T> CompletableFuture<Void> executeWithCompletableFuture(Iterable<Callable<T>> tasks) {
        var futures = new java.util.ArrayList<CompletableFuture<T>>();
        
        for (Callable<T> task : tasks) {
            var future = CompletableFuture.supplyAsync(() -> {
                try {
                    return task.call();
                } catch (Exception e) {
                    log.error("IO task execution failed", e);
                    throw new CompletionException(e);
                }
            }, createVirtualThreadExecutor());
            futures.add(future);
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    // ==================== CPU密集型任务 ====================
    // 适用于：复杂计算、算法处理、数据转换等

    /**
     * 执行CPU密集型任务（使用平台线程池）
     * 适用于复杂计算、算法处理、数据转换等
     */
    public void executeCpuTask(Runnable task) {
        if (task == null) {
            Ex.throwEx(StandardErrorCode.PARAM_ERROR, "Task cannot be null");
        }
        // CPU任务使用平台线程池，避免虚拟线程的上下文切换开销
        cpuThreadPool.execute(wrapRunnable(task));
    }

    /**
     * 提交CPU密集型任务并返回Future
     */
    public <T> Future<T> submitCpuTask(Callable<T> task) {
        if (task == null) {
            Ex.throwEx(StandardErrorCode.PARAM_ERROR, "Task cannot be null");
        }
        return cpuThreadPool.submit(wrapCallable(task));
    }

    /**
     * 并行执行CPU任务（使用ForkJoinPool）
     */
    public <T> CompletableFuture<Void> executeCpuTasksParallel(Iterable<Callable<T>> tasks) {
        if (tasks == null) {
            Ex.throwEx(StandardErrorCode.PARAM_ERROR, "Tasks cannot be null");
        }
        
        var futures = new java.util.ArrayList<CompletableFuture<T>>();
        var forkJoinPool = ForkJoinPool.commonPool();
        
        for (Callable<T> task : tasks) {
            var future = CompletableFuture.supplyAsync(() -> {
                try {
                    return task.call();
                } catch (Exception e) {
                    log.error("CPU task execution failed", e);
                    throw new CompletionException(e);
                }
            }, forkJoinPool);
            futures.add(future);
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    // ==================== 通用任务 ====================
    // 适用于：一般业务逻辑、混合型任务等

    /**
     * 执行通用任务（智能选择线程池）
     */
    public void executeCommonTask(Runnable task) {
        if (task == null) {
            Ex.throwEx(StandardErrorCode.PARAM_ERROR, "Task cannot be null");
        }
        // 通用任务根据任务特性智能选择线程池
        if (isIoBoundTask(task)) {
            executeIoTask(task);
        } else if (isCpuBoundTask(task)) {
            executeCpuTask(task);
        } else {
            commonThreadPool.execute(wrapRunnable(task));
        }
    }

    /**
     * 提交通用任务并返回Future
     */
    public <T> Future<T> submitCommonTask(Callable<T> task) {
        if (task == null) {
            Ex.throwEx(StandardErrorCode.PARAM_ERROR, "Task cannot be null");
        }
        return commonThreadPool.submit(wrapCallable(task));
    }

    // ==================== 监控任务 ====================
    // 适用于：健康检查、指标收集、日志记录等

    /**
     * 执行监控任务（低优先级，不阻塞主业务）
     */
    public void executeMonitorTask(Runnable task) {
        if (task == null) {
            Ex.throwEx(StandardErrorCode.PARAM_ERROR, "Task cannot be null");
        }
        // 监控任务使用专门的线程池，避免影响主业务
        monitorThreadPool.execute(wrapRunnable(task));
    }

    /**
     * 提交监控任务并返回Future
     */
    public <T> Future<T> submitMonitorTask(Callable<T> task) {
        if (task == null) {
            Ex.throwEx(StandardErrorCode.PARAM_ERROR, "Task cannot be null");
        }
        return monitorThreadPool.submit(wrapCallable(task));
    }

    // ==================== 定时任务 ====================
    // 适用于：定时执行、周期性任务等

    /**
     * 执行定时任务
     */
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        if (task == null) {
            Ex.throwEx(StandardErrorCode.PARAM_ERROR, "Task cannot be null");
        }
        if (unit == null) {
            Ex.throwEx(StandardErrorCode.PARAM_ERROR, "TimeUnit cannot be null");
        }
        return scheduledThreadPool.schedule(wrapRunnable(task), delay, unit);
    }

    /**
     * 执行定时任务并返回Future
     */
    public <T> ScheduledFuture<T> schedule(Callable<T> task, long delay, TimeUnit unit) {
        if (task == null) {
            Ex.throwEx(StandardErrorCode.PARAM_ERROR, "Task cannot be null");
        }
        if (unit == null) {
            Ex.throwEx(StandardErrorCode.PARAM_ERROR, "TimeUnit cannot be null");
        }
        return scheduledThreadPool.schedule(wrapCallable(task), delay, unit);
    }

    /**
     * 执行周期性任务
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        if (task == null) {
            Ex.throwEx(StandardErrorCode.PARAM_ERROR, "Task cannot be null");
        }
        if (unit == null) {
            Ex.throwEx(StandardErrorCode.PARAM_ERROR, "TimeUnit cannot be null");
        }
        return scheduledThreadPool.scheduleAtFixedRate(wrapRunnable(task), initialDelay, period, unit);
    }

    /**
     * 执行带延迟的周期性任务
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit) {
        if (task == null) {
            Ex.throwEx(StandardErrorCode.PARAM_ERROR, "Task cannot be null");
        }
        if (unit == null) {
            Ex.throwEx(StandardErrorCode.PARAM_ERROR, "TimeUnit cannot be null");
        }
        return scheduledThreadPool.scheduleWithFixedDelay(wrapRunnable(task), initialDelay, delay, unit);
    }

    // ==================== 现代异步API ====================
    // 使用CompletableFuture和虚拟线程

    /**
     * 异步执行任务并返回CompletableFuture（使用虚拟线程）
     */
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        if (supplier == null) {
            Ex.throwEx(StandardErrorCode.PARAM_ERROR, "Supplier cannot be null");
        }
        // 优先使用虚拟线程，提高并发性能
        return CompletableFuture.supplyAsync(supplier, createVirtualThreadExecutor());
    }

    /**
     * 异步执行任务（使用虚拟线程）
     */
    public CompletableFuture<Void> runAsync(Runnable runnable) {
        if (runnable == null) {
            Ex.throwEx(StandardErrorCode.PARAM_ERROR, "Runnable cannot be null");
        }
        return CompletableFuture.runAsync(runnable, createVirtualThreadExecutor());
    }

    /**
     * 带超时的异步执行
     */
    public <T> CompletableFuture<T> supplyAsyncWithTimeout(Supplier<T> supplier, Duration timeout) {
        if (supplier == null) {
            Ex.throwEx(StandardErrorCode.PARAM_ERROR, "Supplier cannot be null");
        }
        if (timeout == null || timeout.isNegative()) {
            Ex.throwEx(StandardErrorCode.PARAM_ERROR, "Timeout must be positive");
        }
        
        var future = supplyAsync(supplier);
        var timeoutFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(timeout.toMillis());
                return null;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        });
        
        return future.completeOnTimeout(null, timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    // ==================== 线程池管理 ====================

    /**
     * 获取通用线程池
     */
    public ThreadPoolTaskExecutor getCommonThreadPool() {
        return commonThreadPool;
    }

    /**
     * 优雅关闭所有线程池
     */
    public void shutdownGracefully() {
        log.info("Shutting down thread pools gracefully...");
        
        // 关闭IO线程池
        if (ioThreadPool != null) {
            ioThreadPool.shutdown();
        }
        
        // 关闭CPU线程池
        if (cpuThreadPool != null) {
            cpuThreadPool.shutdown();
        }
        
        // 关闭通用线程池
        if (commonThreadPool != null) {
            commonThreadPool.shutdown();
        }
        
        // 关闭监控线程池
        if (monitorThreadPool != null) {
            monitorThreadPool.shutdown();
        }
        
        // 关闭定时线程池
        if (scheduledThreadPool != null) {
            scheduledThreadPool.shutdown();
        }
        
        log.info("All thread pools shutdown completed");
    }

    // ==================== 任务包装和异常处理 ====================

    /**
     * 包装Runnable，添加异常处理和监控
     */
    private Runnable wrapRunnable(Runnable runnable) {
        return () -> {
            var startTime = System.nanoTime();
            try {
                runnable.run();
                var duration = System.nanoTime() - startTime;
                if (duration > 1_000_000_000) { // 超过1秒记录警告
                    log.warn("Task execution took {} ms", duration / 1_000_000);
                }
            } catch (Exception e) {
                var duration = System.nanoTime() - startTime;
                log.error("Task execution failed after {} ms", duration / 1_000_000, e);
                if (e instanceof RuntimeException) {
                    throw e; // 保留原始异常
                } else {
                    Ex.throwEx(StandardErrorCode.THREAD_ERROR, "Task execution failed: " + e.getMessage(), e);
                }
            }
        };
    }

    /**
     * 包装Callable，添加异常处理和监控
     */
    private <T> Callable<T> wrapCallable(Callable<T> callable) {
        return () -> {
            long startTime = System.nanoTime();
            String taskName = getTaskName(callable);
            
            try {
                T result = callable.call();
                long duration = System.nanoTime() - startTime;
                long durationMs = duration / 1_000_000;
                
                // 性能监控：超过1秒记录警告
                if (durationMs > 1000) {
                    log.warn("Task '{}' execution took {} ms - consider optimization", taskName, durationMs);
                } else if (log.isDebugEnabled()) {
                    log.debug("Task '{}' completed in {} ms", taskName, durationMs);
                }
                
                return result;
            } catch (Exception e) {
                long duration = System.nanoTime() - startTime;
                long durationMs = duration / 1_000_000;
                
                log.error("Task '{}' execution failed after {} ms", taskName, durationMs, e);
                
                // 统一异常处理：所有异常都包装为RuntimeException
                if (e instanceof RuntimeException) {
                    throw e;
                } else {
                    throw new RuntimeException("Task '" + taskName + "' execution failed: " + e.getMessage(), e);
                }
            }
        };
    }
    
    /**
     * 获取任务名称用于日志记录
     */
    private String getTaskName(Callable<?> callable) {
        if (callable == null) {
            return "null";
        }
        
        String className = callable.getClass().getSimpleName();
        // 如果是匿名类或Lambda，尝试获取更友好的名称
        if (className.contains("$$") || className.contains("Lambda")) {
            return "Task-" + callable.hashCode();
        }
        
        return className;
    }

    // ==================== 任务类型判断 ====================

    /**
     * 判断是否为IO密集型任务
     */
    private boolean isIoBoundTask(Runnable task) {
        // 这里可以通过任务名称、注解或其他方式判断
        // 暂时返回false，实际使用时可以根据具体需求实现
        return false;
    }

    /**
     * 判断是否为CPU密集型任务
     */
    private boolean isCpuBoundTask(Runnable task) {
        // 这里可以通过任务名称、注解或其他方式判断
        // 暂时返回false，实际使用时可以根据具体需求实现
        return false;
    }

    // ==================== 线程池状态监控 ====================

    /**
     * 获取线程池状态
     */
    public ThreadPoolStatus getThreadPoolStatus() {
        return ThreadPoolStatus.builder()
                .ioThreadPool(getExecutorStatus(ioThreadPool))
                .cpuThreadPool(getExecutorStatus(cpuThreadPool))
                .commonThreadPool(getExecutorStatus(commonThreadPool))
                .monitorThreadPool(getExecutorStatus(monitorThreadPool))
                .build();
    }

    private ExecutorStatus getExecutorStatus(ThreadPoolTaskExecutor executor) {
        if (executor == null) {
            Ex.throwEx(StandardErrorCode.PARAM_ERROR, "Executor cannot be null");
        }
        return ExecutorStatus.builder()
                .poolSize(executor.getPoolSize())
                .activeThreads(executor.getActiveCount())
                .queueSize(executor.getThreadPoolExecutor().getQueue().size())
                .completedTasks(executor.getThreadPoolExecutor().getCompletedTaskCount())
                .totalTasks(executor.getThreadPoolExecutor().getTaskCount())
                .build();
    }

    /**
     * 线程池状态信息
     */
    @Data
    @Builder
    public static class ThreadPoolStatus {
        private final ExecutorStatus ioThreadPool;
        private final ExecutorStatus cpuThreadPool;
        private final ExecutorStatus commonThreadPool;
        private final ExecutorStatus monitorThreadPool;
    }

    /**
     * 执行器状态信息
     */
    @Data
    @Builder
    public static class ExecutorStatus {
        private final int poolSize;
        private final int activeThreads;
        private final int queueSize;
        private final long completedTasks;
        private final long totalTasks;
    }

    // ==================== 虚拟线程支持 ====================
    
    /**
     * 创建虚拟线程执行器（兼容Java 17）
     * 如果Java版本支持虚拟线程，则使用虚拟线程
     * 否则回退到平台线程池
     */
    private Executor createVirtualThreadExecutor() {
        try {
            // 尝试使用Java 19+的虚拟线程
            if (isVirtualThreadSupported()) {
                // 使用反射调用newVirtualThreadPerTaskExecutor方法
                @SuppressWarnings("unchecked")
                java.lang.reflect.Method method = Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
                return (Executor) method.invoke(null);
            }
        } catch (Exception e) {
            log.debug("Virtual threads not supported, falling back to platform threads", e);
        }
        
        // 回退到平台线程池
        return Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("fallback-virtual-" + t.getId());
            return t;
        });
    }
    
    /**
     * 检查是否支持虚拟线程
     */
    private boolean isVirtualThreadSupported() {
        try {
            // 检查Java版本
            String version = System.getProperty("java.version");
            if (version != null && version.startsWith("1.")) {
                // Java 8-10
                int majorVersion = Integer.parseInt(version.split("\\.")[1]);
                return majorVersion >= 9;
            } else if (version != null && version.matches("\\d+")) {
                // Java 11+
                int majorVersion = Integer.parseInt(version);
                return majorVersion >= 19;
            }
        } catch (Exception e) {
            log.debug("Could not determine Java version", e);
        }
        
        // 尝试反射调用虚拟线程相关方法
        try {
            Class.forName("java.util.concurrent.Executors");
            Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查是否支持结构化并发
     */
    private boolean isStructuredConcurrencySupported() {
        try {
            // 检查Java版本
            String version = System.getProperty("java.version");
            if (version != null && version.startsWith("1.")) {
                // Java 8-10
                int majorVersion = Integer.parseInt(version.split("\\.")[1]);
                return majorVersion >= 9;
            } else if (version != null && version.matches("\\d+")) {
                // Java 11+
                int majorVersion = Integer.parseInt(version);
                return majorVersion >= 19;
            }
        } catch (Exception e) {
            log.debug("Could not determine Java version", e);
        }
        
        // 尝试反射调用StructuredTaskScope相关方法
        try {
            Class.forName("java.util.concurrent.StructuredTaskScope");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
} 