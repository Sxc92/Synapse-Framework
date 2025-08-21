package com.indigo.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 安全模块配置属性
 * 直接使用Sa-Token框架处理所有认证
 *
 * @author 史偕成
 * @date 2025/01/07
 */
@Data
@ConfigurationProperties(prefix = "synapse.security")
public class SecurityProperties {

    /**
     * 是否启用安全模块
     */
    private boolean enabled = true;

    /**
     * 安全模式
     */
    private SecurityMode mode = SecurityMode.STRICT;

    /**
     * 是否启用安全日志
     */
    private boolean securityLogging = true;

    /**
     * 安全日志级别
     */
    private String securityLogLevel = "INFO";

    /**
     * 安全模式枚举
     */
    public enum SecurityMode {
        /**
         * 严格模式：所有请求都需要认证
         */
        STRICT,
        
        /**
         * 宽松模式：部分请求可以匿名访问
         */
        PERMISSIVE,
        
        /**
         * 关闭模式：不进行安全控制
         */
        DISABLED
    }
} 