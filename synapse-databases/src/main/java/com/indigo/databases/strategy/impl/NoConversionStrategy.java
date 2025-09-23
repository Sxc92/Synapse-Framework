package com.indigo.databases.strategy.impl;

import com.indigo.databases.strategy.FieldConversionStrategy;

/**
 * 无转换策略
 * 字段名和数据库列名保持一致
 * 
 * @author 史偕成
 * @date 2025/12/20
 */
public class NoConversionStrategy implements FieldConversionStrategy {
    
    @Override
    public String convertFieldToColumn(String fieldName) {
        return fieldName;
    }
    
    @Override
    public String convertColumnToField(String columnName) {
        return columnName;
    }
}
