package com.indigo.databases.utils;

import com.indigo.core.entity.vo.BaseVO;
import com.indigo.core.annotation.FieldMapping;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VO映射工具类
 * 支持字段映射注解，使用Spring BeanUtils进行实体到VO的映射
 * 
 * @author 史偕成
 * @date 2025/12/19
 */
@SuppressWarnings("rawtypes")
public class VoMapper {
    
    /**
     * 将实体对象映射为VO对象
     * 支持字段映射注解
     * 
     * @param entity 实体对象
     * @param voClass VO类
     * @return VO对象
     */
    public static <T, V extends BaseVO> V mapToVo(T entity, Class<V> voClass) {
        if (entity == null) {
            System.out.println("实体对象为null，返回null");
            return null;
        }
        
        try {
            V vo = voClass.getDeclaredConstructor().newInstance();
            System.out.println("开始映射实体: " + entity.getClass().getSimpleName() + " -> " + voClass.getSimpleName());
            
            // 检查是否有字段映射注解
            if (hasFieldMappingAnnotations(voClass)) {
                System.out.println("检测到字段映射注解，使用自定义映射");
                // 使用自定义映射
                mapWithAnnotations(entity, vo, voClass);
            } else {
                System.out.println("未检测到字段映射注解，使用默认映射");
                // 使用默认映射
                BeanUtils.copyProperties(entity, vo);
            }
            
            System.out.println("映射完成，返回VO对象");
            return vo;
        } catch (Exception e) {
            System.err.println("实体映射到VO失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("实体映射到VO失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将实体列表映射为VO列表
     * 
     * @param entities 实体列表
     * @param voClass VO类
     * @return VO列表
     */
    public static <T, V extends BaseVO> List<V> mapToVoList(List<T> entities, Class<V> voClass) {
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<V> voList = new ArrayList<>(entities.size());
        for (T entity : entities) {
            V vo = mapToVo(entity, voClass);
            if (vo != null) {
                voList.add(vo);
            }
        }
        
        return voList;
    }
    
    /**
     * 检查VO类是否有字段映射注解
     */
    private static <V extends BaseVO> boolean hasFieldMappingAnnotations(Class<V> voClass) {
        Field[] fields = voClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(FieldMapping.class)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 使用注解进行字段映射
     */
    private static <T, V extends BaseVO> void mapWithAnnotations(T entity, V vo, Class<V> voClass) {
        // 收集需要特殊处理的字段
        Map<String, Field> annotationFields = new HashMap<>();
        Field[] fields = voClass.getDeclaredFields();
        
        for (Field field : fields) {
            FieldMapping mapping = field.getAnnotation(FieldMapping.class);
            if (mapping != null && !mapping.ignore()) {
                annotationFields.put(field.getName(), field);
            }
        }
        
        // 先使用默认映射，但跳过有注解的字段
        BeanUtils.copyProperties(entity, vo, getIgnoredProperties(annotationFields));
        
        // 然后处理有注解的字段
        for (Field field : fields) {
            FieldMapping mapping = field.getAnnotation(FieldMapping.class);
            if (mapping != null && !mapping.ignore()) {
                try {
                    // 获取数据库字段名
                    String dbFieldName = mapping.value();
                    if (dbFieldName.isEmpty()) {
                        // 如果没有指定，使用默认的驼峰转下划线
                        dbFieldName = camelToUnderline(field.getName());
                    }
                    
                    // 从实体中获取值
                    Field entityField = findEntityField(entity.getClass(), dbFieldName);
                    if (entityField != null) {
                        entityField.setAccessible(true);
                        Object value = entityField.get(entity);
                        
                        // 设置到VO中
                        field.setAccessible(true);
                        field.set(vo, value);
                        
                        System.out.println("字段映射成功: " + field.getName() + " <- " + dbFieldName + " = " + value);
                    } else {
                        System.err.println("未找到实体字段: " + dbFieldName + " 对应VO字段: " + field.getName());
                    }
                } catch (Exception e) {
                    // 忽略映射失败的字段，继续处理其他字段
                    System.err.println("字段映射失败: " + field.getName() + " -> " + mapping.value() + ", 错误: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 获取需要忽略的属性名数组
     */
    private static String[] getIgnoredProperties(Map<String, Field> annotationFields) {
        return annotationFields.keySet().toArray(new String[0]);
    }
    
    /**
     * 在实体类中查找对应的字段
     */
    private static Field findEntityField(Class<?> entityClass, String dbFieldName) {
        // 尝试直接匹配
        Field field = ReflectionUtils.findField(entityClass, dbFieldName);
        if (field != null) {
            return field;
        }
        
        // 尝试下划线转驼峰
        String camelFieldName = underlineToCamel(dbFieldName);
        field = ReflectionUtils.findField(entityClass, camelFieldName);
        if (field != null) {
            return field;
        }
        
        // 尝试驼峰转下划线
        String underlineFieldName = camelToUnderline(dbFieldName);
        field = ReflectionUtils.findField(entityClass, underlineFieldName);
        if (field != null) {
            return field;
        }
        
        return null;
    }
    
    /**
     * 驼峰转下划线
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
     * 下划线转驼峰
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
