package com.indigo.databases.utils;

import com.indigo.databases.service.FieldConversionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 字段转换工具类
 * 提供静态方法访问字段转换服务
 * 
 * @author 史偕成
 * @date 2025/12/20
 */
@Component
public class FieldConversionUtils {
    
    private static FieldConversionService fieldConversionService;
    
    @Autowired
    public void setFieldConversionService(FieldConversionService fieldConversionService) {
        FieldConversionUtils.fieldConversionService = fieldConversionService;
    }
    
    /**
     * 将字段名转换为数据库列名
     * 
     * @param fieldName 字段名
     * @return 数据库列名
     */
    public static String convertFieldToColumn(String fieldName) {
        if (fieldConversionService == null) {
            // 如果服务未初始化，使用默认的驼峰转下划线
            return camelToUnderline(fieldName);
        }
        return fieldConversionService.convertFieldToColumn(fieldName);
    }
    
    /**
     * 将数据库列名转换为字段名
     * 
     * @param columnName 数据库列名
     * @return 字段名
     */
    public static String convertColumnToField(String columnName) {
        if (fieldConversionService == null) {
            // 如果服务未初始化，使用默认的下划线转驼峰
            return underlineToCamel(columnName);
        }
        return fieldConversionService.convertColumnToField(columnName);
    }
    
    /**
     * 驼峰转下划线（默认实现）
     */
    private static String camelToUnderline(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
    
    /**
     * 下划线转驼峰（默认实现）
     */
    private static String underlineToCamel(String underline) {
        if (underline == null || underline.isEmpty()) {
            return underline;
        }
        
        StringBuilder result = new StringBuilder();
        boolean nextUpperCase = false;
        
        for (int i = 0; i < underline.length(); i++) {
            char c = underline.charAt(i);
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
}
