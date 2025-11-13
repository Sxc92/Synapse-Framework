package com.indigo.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.*;

/**
 * Thread pool configuration for different use cases
 * Provides optimized thread pools for IO, CPU, common, and monitoring tasks
 * 
 * @author 史偕成
 * @date 2025/08/11 12:41:56
 **/
@Slf4j
@Configuration
public class ThreadPoolConfig {

    // ==================== IO密集型线程池 ====================
    // 特点：线程数较多，队列较大，适合网络请求、文件操作等

    @Bean("ioThreadPool")
    public ThreadPoolTaskExecutor ioThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // IO任务通常需要更多线程来处理并发
        executor.setCorePoolSize(50);
        executor.setMaxPoolSize(200);
        executor.setQueueCapacity(1000);
        executor.setKeepAliveSeconds(60);
        
        // 线程名前缀，便于监控和调试
        executor.setThreadNamePrefix("io-");
        
        // 拒绝策略：对于IO任务，通常希望等待而不是拒绝
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        log.debug("IO thread pool initialized: core={}, max={}, queue={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }

    // ==================== CPU密集型线程池 ====================
    // 特点：线程数较少，队列较小，适合计算密集型任务

    @Bean("cpuThreadPool")
    public ThreadPoolTaskExecutor cpuThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // CPU任务线程数通常设置为CPU核心数或略多
        int cpuCores = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(cpuCores);
        executor.setMaxPoolSize(cpuCores * 2);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(300);
        
        executor.setThreadNamePrefix("cpu-");
        
        // 拒绝策略：CPU任务通常可以拒绝，避免系统过载
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        log.debug("CPU thread pool initialized: core={}, max={}, queue={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }

    // ==================== 通用线程池 ====================
    // 特点：平衡配置，适合一般业务逻辑

    @Bean("commonThreadPool")
    public ThreadPoolTaskExecutor commonThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 通用线程池使用中等配置
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(500);
        executor.setKeepAliveSeconds(120);
        
        executor.setThreadNamePrefix("common-");
        
        // 拒绝策略：使用CallerRunsPolicy，让调用线程执行任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        log.debug("Common thread pool initialized: core={}, max={}, queue={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }

    // ==================== 监控线程池 ====================
    // 特点：低优先级，不阻塞主业务

    @Bean("monitorThreadPool")
    public ThreadPoolTaskExecutor monitorThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 监控任务使用较少线程，避免影响主业务
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(300);
        
        executor.setThreadNamePrefix("monitor-");
        
        // 拒绝策略：监控任务可以丢弃，不影响主业务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(10);
        
        executor.initialize();
        log.debug("Monitor thread pool initialized: core={}, max={}, queue={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }

    // ==================== 定时任务线程池 ====================
    // 特点：支持定时和周期性任务

    @Bean("scheduledThreadPool")
    public ScheduledExecutorService scheduledThreadPool() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10, r -> {
            Thread t = new Thread(r);
            t.setName("scheduled-" + t.getId());
            t.setDaemon(true);
            return t;
        });
        
        log.debug("Scheduled thread pool initialized: poolSize=10");
        
        return scheduler;
    }

    // ==================== 虚拟线程工厂 ====================
    // 用于创建虚拟线程，适合IO密集型任务

    @Bean("virtualThreadFactory")
    public ThreadFactory virtualThreadFactory() {
        // 兼容Java 17的虚拟线程工厂
        if (isVirtualThreadSupported()) {
            try {
                // 使用反射调用Java 19+的虚拟线程方法
                java.lang.reflect.Method ofVirtualMethod = ThreadFactory.class.getMethod("ofVirtual");
                Object builder = ofVirtualMethod.invoke(null);
                
                // 调用name方法
                java.lang.reflect.Method nameMethod = builder.getClass().getMethod("name", String.class, long.class);
                builder = nameMethod.invoke(builder, "virtual-", 0L);
                
                // 调用uncaughtExceptionHandler方法
                java.lang.reflect.Method handlerMethod = builder.getClass().getMethod("uncaughtExceptionHandler", Thread.UncaughtExceptionHandler.class);
                builder = handlerMethod.invoke(builder, (Thread.UncaughtExceptionHandler) (thread, throwable) -> {
                    log.error("Uncaught exception in virtual thread: {}", thread.getName(), throwable);
                });
                
                // 调用build方法
                java.lang.reflect.Method buildMethod = builder.getClass().getMethod("build");
                return (ThreadFactory) buildMethod.invoke(builder);
            } catch (Exception e) {
                log.warn("Failed to create virtual thread factory using reflection, falling back to platform threads", e);
            }
        }
        
        // 回退到平台线程工厂
        return r -> {
            Thread t = new Thread(r);
            t.setName("virtual-fallback-" + t.getId());
            t.setDaemon(true);
            t.setUncaughtExceptionHandler((thread, throwable) -> {
                log.error("Uncaught exception in fallback thread: {}", thread.getName(), throwable);
            });
            return t;
        };
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

    // ==================== 异步任务执行器 ====================
    // 用于@Async注解的默认执行器

    @Bean("asyncTaskExecutor")
    public Executor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 异步任务执行器配置
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("async-");
        
        // 拒绝策略：使用CallerRunsPolicy
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        log.debug("Async task executor initialized: core={}, max={}, queue={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }
} 