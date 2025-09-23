package com.indigo.databases.strategy.impl;

import com.indigo.databases.strategy.FieldConversionStrategy;

/**
 * 自定义转换策略
 * 支持通过配置指定转换规则
 * 
 * @author 史偕成
 * @date 2025/12/20
 */
public class CustomConversionStrategy implements FieldConversionStrategy {
    
    private final String fieldToColumnPattern;
    private final String columnToFieldPattern;
    
    public CustomConversionStrategy(String fieldToColumnPattern, String columnToFieldPattern) {
        this.fieldToColumnPattern = fieldToColumnPattern;
        this.columnToFieldPattern = columnToFieldPattern;
    }
    
    @Override
    public String convertFieldToColumn(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return fieldName;
        }
        
        // 简单的模式替换实现
        // 可以根据需要扩展为更复杂的转换逻辑
        if (fieldToColumnPattern != null && !fieldToColumnPattern.isEmpty()) {
            // 这里可以实现更复杂的转换逻辑
            // 暂时使用简单的字符串替换
            return fieldName.replaceAll("([A-Z])", "_$1").toLowerCase();
        }
        
        return fieldName;
    }
    
    @Override
    public String convertColumnToField(String columnName) {
        if (columnName == null || columnName.isEmpty()) {
            return columnName;
        }
        
        // 简单的模式替换实现
        if (columnToFieldPattern != null && !columnToFieldPattern.isEmpty()) {
            // 这里可以实现更复杂的转换逻辑
            // 暂时使用简单的字符串替换
            StringBuilder result = new StringBuilder();
            boolean nextUpperCase = false;
            
            for (int i = 0; i < columnName.length(); i++) {
                char c = columnName.charAt(i);
                if (c == '_') {
                    nextUpperCase = true;
                } else {
                    if (nextUpperCase) {
                        result.append(Character.toUpperCase(c));
                        nextUpperCase = false;
                    } else {
                        result.append(Character.toLowerCase(c));
                    }
                }
            }
            return result.toString();
        }
        
        return columnName;
    }
}
