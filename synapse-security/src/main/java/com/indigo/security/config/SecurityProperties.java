package com.indigo.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 安全模块配置属性
 *
 * @author 史偕成
 * @date 2025/01/07
 */
@Data
@ConfigurationProperties(prefix = "synapse.security")
public class SecurityProperties {

    /**
     * 认证配置
     */
    private Authentication authentication = new Authentication();

    /**
     * 认证配置
     */
    @Data
    public static class Authentication {
        
        /**
         * 默认认证策略类型
         */
        private String defaultStrategy = "satoken";
        
        /**
         * 是否启用策略类型推断
         */
        private boolean enableStrategyInference = true;
        
        /**
         * 是否启用请求策略类型覆盖
         */
        private boolean enableRequestStrategyOverride = true;
    }
} 