package com.indigo.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 分布式锁配置属性
 * 基于Redis实现，简化配置
 * 
 * @author 史偕成
 * @date 2025/01/08
 */
@Data
@ConfigurationProperties(prefix = "synapse.cache.lock")
public class LockProperties {
    
    /**
     * 是否启用分布式锁功能
     */
    private boolean enabled = true;
    
    /**
     * 锁键前缀
     */
    private String keyPrefix = "synapse:lock";
    
    /**
     * 默认锁超时时间（秒）
     */
    private long defaultTimeout = 30;
    
    /**
     * 重试间隔（毫秒）
     */
    private long retryInterval = 100;
    
    /**
     * 最大重试次数
     */
    private int maxRetries = 3;
    
    /**
     * 自动释放配置
     */
    private AutoRelease autoRelease = new AutoRelease();
    
    @Data
    public static class AutoRelease {
        /**
         * 是否启用自动释放
         */
        private boolean enabled = true;
        
        /**
         * 自动释放检查间隔（毫秒）
         */
        private long checkInterval = 60000; // 1分钟
        
        /**
         * 核心服务释放阈值（毫秒）- 超过此时间未访问则自动释放
         */
        private long coreServiceThreshold = 1800000; // 30分钟
        
        /**
         * 业务缓存释放阈值（毫秒）- 超过此时间未访问则自动释放
         */
        private long businessCacheThreshold = 900000; // 15分钟
        
        /**
         * 临时资源释放阈值（毫秒）- 超过此时间未访问则自动释放
         */
        private long temporaryThreshold = 300000; // 5分钟
    }
} 