package com.indigo.databases.utils;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.indigo.core.entity.vo.BaseVO;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * VO字段选择器
 * 根据VO类自动选择需要的数据库字段
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Slf4j
public class VoFieldSelector {
    
    /**
     * 根据VO类获取需要查询的字段列表
     */
    public static <V extends BaseVO> String[] getSelectFields(Class<V> voClass) {
        List<String> fields = new ArrayList<>();
        
        try {
            // 获取VO类的所有字段
            Field[] declaredFields = voClass.getDeclaredFields();
            
            for (Field field : declaredFields) {
                // 跳过静态字段
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                
                // 跳过特殊字段（如serialVersionUID）
                if (isSpecialField(field.getName())) {
                    continue;
                }
                
                // 转换为数据库字段名
                String columnName = convertFieldToColumn(field.getName());
                fields.add(columnName);
            }
            
            log.debug("VO类 {} 选择的字段: {}", voClass.getSimpleName(), fields);
            
        } catch (Exception e) {
            log.error("获取VO字段失败: {}", voClass.getSimpleName(), e);
            // 如果出错，返回所有字段（安全策略）
            return new String[]{"*"};
        }
        
        return fields.toArray(new String[0]);
    }
    
    /**
     * 根据VO类获取需要查询的字段列表（带表别名）
     */
    public static <V extends BaseVO> String[] getSelectFieldsWithAlias(Class<V> voClass, String tableAlias) {
        String[] fields = getSelectFields(voClass);
        
        if (StringUtils.isBlank(tableAlias)) {
            return fields;
        }
        
        // 为每个字段添加表别名
        String[] aliasedFields = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            aliasedFields[i] = tableAlias + "." + fields[i];
        }
        
        return aliasedFields;
    }
    
    /**
     * 字段名转换为列名
     */
    private static String convertFieldToColumn(String fieldName) {
        return StringUtils.camelToUnderline(fieldName);
    }
    
    /**
     * 判断是否为特殊字段
     */
    private static boolean isSpecialField(String fieldName) {
        // 常见的特殊字段
        return "serialVersionUID".equals(fieldName) ||
               "class".equals(fieldName) ||
               fieldName.startsWith("$");
    }
    
    /**
     * 获取VO类对应的表名
     */
    public static <V extends BaseVO> String getTableName(Class<V> voClass) {
        // 简化实现：使用类名转下划线
        String className = voClass.getSimpleName();
        
        // 移除VO后缀
        if (className.endsWith("VO")) {
            className = className.substring(0, className.length() - 2);
        }
        
        return StringUtils.camelToUnderline(className);
    }
    
    /**
     * 检查VO类是否有特殊字段映射需求
     */
    public static <V extends BaseVO> boolean hasSpecialFieldMapping(Class<V> voClass) {
        // 这里可以检查是否有特殊的注解或字段映射需求
        // 暂时返回false，表示使用默认映射
        return false;
    }
}
