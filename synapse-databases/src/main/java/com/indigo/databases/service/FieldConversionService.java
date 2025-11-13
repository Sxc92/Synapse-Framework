package com.indigo.databases.service;

import com.indigo.databases.config.SynapseDataSourceProperties;
import com.indigo.databases.enums.FieldConversionStrategyType;
import com.indigo.databases.strategy.FieldConversionStrategy;
import com.indigo.databases.strategy.FieldConversionStrategyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;


/**
 * 字段转换服务
 * 根据配置提供字段转换功能
 * 
 * @author 史偕成
 * @date 2025/12/20
 */
@Slf4j
@Service
public class FieldConversionService {
    
    private final SynapseDataSourceProperties dataSourceProperties;
    private FieldConversionStrategy strategy;
    
    public FieldConversionService(SynapseDataSourceProperties dataSourceProperties) {
        this.dataSourceProperties = dataSourceProperties;
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        SynapseDataSourceProperties.FieldConversionConfig config = dataSourceProperties.getFieldConversion();
        
        if (!config.isEnabled()) {
            log.info("字段转换功能已禁用");
            strategy = FieldConversionStrategyFactory.createStrategy(FieldConversionStrategyType.NO_CONVERSION);
            return;
        }
        
        try {
            FieldConversionStrategyType type = config.getStrategy();
            
            if (type == FieldConversionStrategyType.CUSTOM) {
                // 自定义转换策略
                SynapseDataSourceProperties.FieldConversionConfig.CustomConversionPattern pattern = 
                    config.getCustomPattern();
                strategy = FieldConversionStrategyFactory.createStrategy(
                    type, 
                    pattern.getFieldToColumnPattern(), 
                    pattern.getColumnToFieldPattern());
            } else {
                strategy = FieldConversionStrategyFactory.createStrategy(type);
            }
            
            log.debug("字段转换策略初始化完成: {} ({})", type.name(), type.getDescription());
            
        } catch (Exception e) {
            log.warn("字段转换策略初始化失败，使用默认策略: {}", e.getMessage());
            strategy = FieldConversionStrategyFactory.createStrategy(FieldConversionStrategyType.CAMEL_TO_UNDERLINE);
        }
    }
    
    /**
     * 将字段名转换为数据库列名
     * 
     * @param fieldName 字段名
     * @return 数据库列名
     */
    public String convertFieldToColumn(String fieldName) {
        if (strategy == null) {
            return fieldName;
        }
        return strategy.convertFieldToColumn(fieldName);
    }
    
    /**
     * 将数据库列名转换为字段名
     * 
     * @param columnName 数据库列名
     * @return 字段名
     */
    public String convertColumnToField(String columnName) {
        if (strategy == null) {
            return columnName;
        }
        return strategy.convertColumnToField(columnName);
    }
    
    /**
     * 获取当前转换策略
     * 
     * @return 转换策略
     */
    public FieldConversionStrategy getStrategy() {
        return strategy;
    }
}
