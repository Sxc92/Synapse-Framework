package com.indigo.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 分布式死锁检测配置属性
 * 
 * <p><b>注意：</b>此类通过 {@link LockAutoConfiguration} 中的 {@code @EnableConfigurationProperties} 注册为 Bean，
 * 不需要 {@code @Component} 注解。如果同时使用 {@code @Component} 和 {@code @EnableConfigurationProperties}，
 * 会导致创建两个 Bean 实例，引发冲突。
 * 
 * @author 史偕成
 * @date 2025/01/08
 */
@Data
@ConfigurationProperties(prefix = "synapse.cache.lock.deadlock.distributed")
public class DistributedDeadlockProperties {

    /**
     * 是否启用分布式死锁检测
     */
    private boolean enabled = true;

    /**
     * 状态同步间隔（毫秒）
     */
    private long syncInterval = 5000;

    /**
     * 全局检测间隔（毫秒）
     */
    private long globalDetectionInterval = 10000;

    /**
     * 节点超时时间（毫秒）
     */
    private long nodeTimeout = 30000;

    /**
     * 最大节点数量
     */
    private int maxNodes = 10;

    /**
     * Redis键前缀
     */
    private String redisPrefix = "deadlock:global";

    /**
     * 是否启用调试日志
     */
    private boolean debug = false;

    /**
     * 心跳间隔（毫秒）
     */
    private long heartbeatInterval = 1000;

    /**
     * 清理间隔（毫秒）
     */
    private long cleanupInterval = 30000;
}
