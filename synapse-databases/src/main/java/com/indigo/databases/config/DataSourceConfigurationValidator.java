package com.indigo.databases.config;

import com.indigo.databases.dynamic.DynamicRoutingDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;

/**
 * 数据源配置验证器
 * 启动时自动验证配置完整性和数据源连接性
 *
 * @author 史偕成
 * @date 2025/01/19
 */
@Slf4j
@Component
public class DataSourceConfigurationValidator {
    
    private final SynapseDataSourceProperties properties;
    private final DynamicRoutingDataSource dynamicDataSource;
    
    public DataSourceConfigurationValidator(SynapseDataSourceProperties properties, 
                                         DynamicRoutingDataSource dynamicDataSource) {
        this.properties = properties;
        this.dynamicDataSource = dynamicDataSource;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void validateConfiguration() {
        log.debug("开始验证数据源配置...");
        
        try {
            // 1. 验证主数据源
            validatePrimaryDataSource();
            
            // 2. 验证连接池配置
            validateConnectionPoolConfiguration();
            
            // 3. 验证数据源连接性
            validateDataSourceConnectivity();
            
            // 4. 输出配置摘要
            printConfigurationSummary();
            
            log.debug("数据源配置验证完成 ✅");
            
        } catch (Exception e) {
            log.error("数据源配置验证失败 ❌", e);
            throw new ConfigurationException("数据源配置验证失败", e);
        }
    }
    
    /**
     * 验证主数据源配置
     */
    private void validatePrimaryDataSource() {
        if (!StringUtils.hasText(properties.getPrimary())) {
            throw new ConfigurationException("主数据源名称未配置");
        }
        
        if (!dynamicDataSource.getDataSources().containsKey(properties.getPrimary())) {
            throw new ConfigurationException("主数据源 [" + properties.getPrimary() + "] 不存在");
        }
        
        log.debug("✅ 主数据源验证通过: [{}]", properties.getPrimary());
    }
    
    /**
     * 验证连接池配置
     */
    private void validateConnectionPoolConfiguration() {
        for (Map.Entry<String, SynapseDataSourceProperties.DataSourceConfig> entry : 
                properties.getDatasources().entrySet()) {
            String name = entry.getKey();
            SynapseDataSourceProperties.DataSourceConfig config = entry.getValue();
            
            validateHikariConfiguration(name, config.getHikari());
            validateDruidConfiguration(name, config.getDruid());
        }
        
        log.debug("✅ 连接池配置验证通过");
    }
    
    /**
     * 验证 HikariCP 配置
     */
    private void validateHikariConfiguration(String dataSourceName, 
                                           SynapseDataSourceProperties.HikariConfig config) {
        if (config.getMaximumPoolSize() < config.getMinimumIdle()) {
            log.warn("⚠️  数据源 [{}] HikariCP 最大连接池大小小于最小空闲连接数", dataSourceName);
        }
        
        if (config.getConnectionTimeout() < 1000) {
            log.warn("⚠️  数据源 [{}] HikariCP 连接超时时间过短: {}ms", 
                    dataSourceName, config.getConnectionTimeout());
        }
        
        if (config.getLeakDetectionThreshold() > 0 && config.getLeakDetectionThreshold() < 10000) {
            log.warn("⚠️  数据源 [{}] HikariCP 连接泄漏检测阈值过小: {}ms", 
                    dataSourceName, config.getLeakDetectionThreshold());
        }
        
        if (!StringUtils.hasText(config.getConnectionTestQuery())) {
            log.warn("⚠️  数据源 [{}] HikariCP 未配置连接测试查询", dataSourceName);
        }
    }
    
    /**
     * 验证 Druid 配置
     */
    private void validateDruidConfiguration(String dataSourceName, 
                                          SynapseDataSourceProperties.DruidConfig config) {
        if (config.getMaxActive() < config.getMinIdle()) {
            log.warn("⚠️  数据源 [{}] Druid 最大活跃连接数小于最小空闲连接数", dataSourceName);
        }
        
        if (config.getMaxWait() < 1000) {
            log.warn("⚠️  数据源 [{}] Druid 最大等待时间过短: {}ms", 
                    dataSourceName, config.getMaxWait());
        }
        
        if (!StringUtils.hasText(config.getValidationQuery())) {
            log.warn("⚠️  数据源 [{}] Druid 未配置验证查询", dataSourceName);
        }
    }
    
    /**
     * 验证数据源连接性
     */
    private void validateDataSourceConnectivity() {
        for (Map.Entry<String, DataSource> entry : dynamicDataSource.getDataSources().entrySet()) {
            String name = entry.getKey();
            DataSource dataSource = entry.getValue();
            
            try (Connection conn = dataSource.getConnection()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("SELECT 1");
                }
                log.debug("✅ 数据源 [{}] 连接测试通过", name);
            } catch (Exception e) {
                log.error("❌ 数据源 [{}] 连接测试失败: {}", name, e.getMessage());
                throw new ConfigurationException("数据源 [" + name + "] 连接失败", e);
            }
        }
    }
    
    /**
     * 输出配置摘要
     */
    private void printConfigurationSummary() {
        log.debug("📊 数据源配置摘要:");
        log.debug("   主数据源: [{}]", properties.getPrimary());
        log.debug("   总数据源数: [{}]", dynamicDataSource.getDataSources().size());
        log.debug("   负载均衡策略: [{}]", properties.getLoadBalance().getStrategy());
        log.debug("   故障转移: [{}]", properties.getFailover().isEnabled() ? "启用" : "禁用");
        
        // 输出数据源权重分布
        log.debug("   数据源权重分布:");
        for (Map.Entry<String, SynapseDataSourceProperties.DataSourceConfig> entry : 
                properties.getDatasources().entrySet()) {
            String name = entry.getKey();
            SynapseDataSourceProperties.DataSourceConfig config = entry.getValue();
            log.debug("     [{}]: 权重: {}", name, config.getWeight());
        }
    }
    
    /**
     * 配置异常类
     */
    public static class ConfigurationException extends RuntimeException {
        public ConfigurationException(String message) {
            super(message);
        }
        
        public ConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
