package com.indigo.databases.enums;

import lombok.Getter;

/**
 * 字段转换策略类型枚举
 * 
 * @author 史偕成
 * @date 2025/12/20
 */
@Getter
public enum FieldConversionStrategyType {
    
    /**
     * 驼峰转下划线（默认）
     * userName -> user_name
     */
    CAMEL_TO_UNDERLINE("驼峰转下划线"),
    
    /**
     * 驼峰转短横线
     * userName -> user-name
     */
    CAMEL_TO_KEBAB_CASE("驼峰转短横线"),
    
    /**
     * 无转换
     * userName -> userName
     */
    NO_CONVERSION("无转换"),
    
    /**
     * 自定义转换
     * 通过配置指定转换规则
     */
    CUSTOM("自定义转换");
    
    private final String description;
    
    FieldConversionStrategyType(String description) {
        this.description = description;
    }

    /**
     * 根据字符串获取枚举值
     * 
     * @param strategy 策略字符串
     * @return 枚举值，如果不存在则返回默认值
     */
    public static FieldConversionStrategyType fromString(String strategy) {
        if (strategy == null || strategy.trim().isEmpty()) {
            return CAMEL_TO_UNDERLINE;
        }
        
        try {
            return valueOf(strategy.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CAMEL_TO_UNDERLINE;
        }
    }
    
    /**
     * 检查是否为有效的策略类型
     * 
     * @param strategy 策略字符串
     * @return 是否为有效策略
     */
    public static boolean isValid(String strategy) {
        if (strategy == null || strategy.trim().isEmpty()) {
            return false;
        }
        
        try {
            valueOf(strategy.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
