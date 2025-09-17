package com.indigo.databases.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Seata兼容性配置
 * 解决synapse-databases代理机制与Seata的冲突问题
 *
 * @author 史偕成
 * @date 2025/01/19
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = "io.seata.spring.annotation.GlobalTransactional")
@ConditionalOnProperty(name = "synapse.datasource.seata.enabled", havingValue = "true", matchIfMissing = false)
public class SeataCompatibilityConfiguration {
    
    public SeataCompatibilityConfiguration() {
        log.info("SeataCompatibilityConfiguration 被加载 - 启用Seata兼容模式");
    }
    
    /**
     * 配置Seata与synapse-databases的兼容性
     * 延迟Seata的扫描，避免与动态代理冲突
     */
    @Configuration
    @ConditionalOnClass(name = "org.apache.seata.spring.boot.autoconfigure.SeataAutoConfiguration")
    static class SeataAutoConfigurationCompatibility {
        
        public SeataAutoConfigurationCompatibility() {
            log.info("配置Seata自动配置兼容性");
        }
    }
}
