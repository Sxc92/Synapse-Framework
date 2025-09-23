package com.indigo.databases.strategy;

/**
 * 字段转换策略接口
 * 支持不同的字段名到数据库列名的转换方式
 * 
 * @author 史偕成
 * @date 2025/12/20
 */
public interface FieldConversionStrategy {
    
    /**
     * 将字段名转换为数据库列名
     * 
     * @param fieldName 字段名
     * @return 数据库列名
     */
    String convertFieldToColumn(String fieldName);
    
    /**
     * 将数据库列名转换为字段名
     * 
     * @param columnName 数据库列名
     * @return 字段名
     */
    String convertColumnToField(String columnName);
}
