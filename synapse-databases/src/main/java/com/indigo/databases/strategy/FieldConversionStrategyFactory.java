package com.indigo.databases.strategy;

import com.indigo.databases.enums.FieldConversionStrategyType;
import com.indigo.databases.strategy.impl.*;

/**
 * 字段转换策略工厂
 * 根据配置创建相应的转换策略
 * 
 * @author 史偕成
 * @date 2025/12/20
 */
public class FieldConversionStrategyFactory {
    
    /**
     * 创建字段转换策略
     * 
     * @param type 转换类型
     * @return 字段转换策略
     */
    public static FieldConversionStrategy createStrategy(FieldConversionStrategyType type) {
        return createStrategy(type, null, null);
    }
    
    /**
     * 创建字段转换策略
     * 
     * @param type 转换类型
     * @param fieldToColumnPattern 字段转列名模式（用于自定义转换）
     * @param columnToFieldPattern 列名转字段模式（用于自定义转换）
     * @return 字段转换策略
     */
    public static FieldConversionStrategy createStrategy(FieldConversionStrategyType type, 
                                                        String fieldToColumnPattern, 
                                                        String columnToFieldPattern) {
        switch (type) {
            case CAMEL_TO_UNDERLINE:
                return new CamelToUnderlineStrategy();
            case CAMEL_TO_KEBAB_CASE:
                return new CamelToKebabCaseStrategy();
            case NO_CONVERSION:
                return new NoConversionStrategy();
            case CUSTOM:
                return new CustomConversionStrategy(fieldToColumnPattern, columnToFieldPattern);
            default:
                return new CamelToUnderlineStrategy();
        }
    }
    
    /**
     * 根据字符串创建转换策略
     * 
     * @param typeStr 转换类型字符串
     * @return 字段转换策略
     */
    public static FieldConversionStrategy createStrategy(String typeStr) {
        FieldConversionStrategyType type = FieldConversionStrategyType.fromString(typeStr);
        return createStrategy(type);
    }
}
