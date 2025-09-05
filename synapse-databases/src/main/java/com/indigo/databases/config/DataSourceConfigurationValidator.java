package com.indigo.databases.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
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
    private final Map<String, DataSource> dataSourceMap;
    
    public DataSourceConfigurationValidator(SynapseDataSourceProperties properties, 
                                         Map<String, DataSource> dataSourceMap) {
        this.properties = properties;
        this.dataSourceMap = dataSourceMap;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void validateConfiguration() {
        log.info("开始验证数据源配置...");
        
        try {
            // 1. 验证主数据源
            validatePrimaryDataSource();
            
            // 2. 验证读写分离配置
            validateReadWriteConfiguration();
            
            // 3. 验证连接池配置
            validateConnectionPoolConfiguration();
            
            // 4. 验证数据源连接性
            validateDataSourceConnectivity();
            
            // 5. 输出配置摘要
            printConfigurationSummary();
            
            log.info("数据源配置验证完成 ✅");
            
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
        
        if (!dataSourceMap.containsKey(properties.getPrimary())) {
            throw new ConfigurationException("主数据源 [" + properties.getPrimary() + "] 不存在");
        }
        
        log.info("✅ 主数据源验证通过: [{}]", properties.getPrimary());
    }
    
    /**
     * 验证读写分离配置
     */
    private void validateReadWriteConfiguration() {
        if (properties.getReadWrite().isEnabled()) {
            List<String> readSources = properties.getReadWrite().getReadSources();
            List<String> writeSources = properties.getReadWrite().getWriteSources();
            
            if (readSources.isEmpty()) {
                log.warn("⚠️  读写分离已启用，但读数据源列表为空");
            }
            
            if (writeSources.isEmpty()) {
                log.warn("⚠️  读写分离已启用，但写数据源列表为空");
            }
            
            // 验证数据源角色配置
            for (String source : readSources) {
                if (!dataSourceMap.containsKey(source)) {
                    throw new ConfigurationException("读数据源 [" + source + "] 不存在");
                }
                validateDataSourceRole(source, "READ");
            }
            
            for (String source : writeSources) {
                if (!dataSourceMap.containsKey(source)) {
                    throw new ConfigurationException("写数据源 [" + source + "] 不存在");
                }
                validateDataSourceRole(source, "WRITE");
            }
            
            log.info("✅ 读写分离配置验证通过");
        }
    }
    
    /**
     * 验证数据源角色配置
     */
    private void validateDataSourceRole(String dataSourceName, String expectedRole) {
        SynapseDataSourceProperties.DataSourceConfig config = properties.getDatasources().get(dataSourceName);
        if (config != null) {
            String actualRole = config.getRole().name();
            if (!isRoleCompatible(actualRole, expectedRole)) {
                log.warn("⚠️  数据源 [{}] 角色配置不匹配，期望: [{}], 实际: [{}]", 
                        dataSourceName, expectedRole, actualRole);
            }
        }
    }
    
    /**
     * 检查角色是否兼容
     */
    private boolean isRoleCompatible(String actualRole, String expectedRole) {
        if ("READ_WRITE".equals(actualRole)) {
            return true; // READ_WRITE 兼容所有角色
        }
        return actualRole.equals(expectedRole);
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
        
        log.info("✅ 连接池配置验证通过");
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
        for (Map.Entry<String, DataSource> entry : dataSourceMap.entrySet()) {
            String name = entry.getKey();
            DataSource dataSource = entry.getValue();
            
            try (Connection conn = dataSource.getConnection()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("SELECT 1");
                }
                log.info("✅ 数据源 [{}] 连接测试通过", name);
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
        log.info("📊 数据源配置摘要:");
        log.info("   主数据源: [{}]", properties.getPrimary());
        log.info("   总数据源数: [{}]", dataSourceMap.size());
        log.info("   读写分离: [{}]", properties.getReadWrite().isEnabled() ? "启用" : "禁用");
        log.info("   负载均衡策略: [{}]", properties.getLoadBalance().getStrategy());
        log.info("   故障转移: [{}]", properties.getFailover().isEnabled() ? "启用" : "禁用");
        
        if (properties.getReadWrite().isEnabled()) {
            log.info("   读数据源: [{}]", String.join(", ", properties.getReadWrite().getReadSources()));
            log.info("   写数据源: [{}]", String.join(", ", properties.getReadWrite().getWriteSources()));
        }
        
        // 输出数据源角色分布
        log.info("   数据源角色分布:");
        for (Map.Entry<String, SynapseDataSourceProperties.DataSourceConfig> entry : 
                properties.getDatasources().entrySet()) {
            String name = entry.getKey();
            SynapseDataSourceProperties.DataSourceConfig config = entry.getValue();
            log.info("     [{}]: {} (权重: {})", name, config.getRole(), config.getWeight());
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
