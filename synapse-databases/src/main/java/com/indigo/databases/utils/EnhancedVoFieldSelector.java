package com.indigo.databases.utils;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.indigo.core.entity.vo.BaseVO;
import com.indigo.databases.annotation.VoMapping;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 增强VO字段选择器
 * 支持多表关联查询的字段选择
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Slf4j
@SuppressWarnings("rawtypes")
public class EnhancedVoFieldSelector {
    
    /**
     * 根据VO类获取需要查询的字段列表（支持多表关联）
     */
    public static <V extends BaseVO> String[] getSelectFields(Class<V> voClass) {
        VoMapping mapping = voClass.getAnnotation(VoMapping.class);
        
        if (mapping != null && mapping.fields().length > 0) {
            // 使用注解配置的字段映射
            return getSelectFieldsFromMapping(mapping);
        } else {
            // 使用默认的单表字段选择
            return VoFieldSelector.getSelectFields(voClass);
        }
    }
    
    /**
     * 根据注解配置获取字段列表
     */
    private static String[] getSelectFieldsFromMapping(VoMapping mapping) {
        List<String> fields = new ArrayList<>();
        
        for (VoMapping.Field field : mapping.fields()) {
            String fieldSql = buildFieldSql(field);
            if (StringUtils.isNotBlank(fieldSql)) {
                fields.add(fieldSql);
            }
        }
        
        log.debug("从注解配置获取字段: {}", fields);
        return fields.toArray(new String[0]);
    }
    
    /**
     * 构建字段SQL
     */
    private static String buildFieldSql(VoMapping.Field field) {
        return switch (field.type()) {
            case ALIAS -> {
                String alias = StringUtils.isNotBlank(field.target()) ? field.target() : field.source();
                yield field.source() + " AS " + alias;
            }
            case EXPRESSION -> StringUtils.isNotBlank(field.expression()) ? field.expression() : field.source();
            default -> field.source();
        };
    }
    
    /**
     * 构建JOIN SQL
     */
    public static <V extends BaseVO> String buildJoinSql(Class<V> voClass) {
        VoMapping mapping = voClass.getAnnotation(VoMapping.class);
        
        if (mapping == null || mapping.joins().length == 0) {
            return "";
        }
        
        StringBuilder joinSql = new StringBuilder();
        
        for (VoMapping.Join join : mapping.joins()) {
            joinSql.append(" ")
                   .append(join.type().getSqlKeyword())
                   .append(" ")
                   .append(join.table())
                   .append(" ")
                   .append(join.alias())
                   .append(" ON ")
                   .append(join.on());
        }
        
        return joinSql.toString();
    }
    
    /**
     * 获取主表名
     */
    public static <V extends BaseVO> String getMainTableName(Class<V> voClass) {
        VoMapping mapping = voClass.getAnnotation(VoMapping.class);
        
        if (mapping != null && StringUtils.isNotBlank(mapping.table())) {
            return mapping.table();
        } else {
            return VoFieldSelector.getTableName(voClass);
        }
    }
    
    /**
     * 获取主表别名
     */
    public static <V extends BaseVO> String getMainTableAlias(Class<V> voClass) {
        VoMapping mapping = voClass.getAnnotation(VoMapping.class);
        
        if (mapping != null && StringUtils.isNotBlank(mapping.alias())) {
            return mapping.alias();
        } else {
            return "t";
        }
    }
    
    /**
     * 检查是否有JOIN查询
     */
    public static <V extends BaseVO> boolean hasJoinQuery(Class<V> voClass) {
        VoMapping mapping = voClass.getAnnotation(VoMapping.class);
        return mapping != null && mapping.joins().length > 0;
    }
    
    /**
     * 检查是否有自定义字段映射
     */
    public static <V extends BaseVO> boolean hasCustomFieldMapping(Class<V> voClass) {
        VoMapping mapping = voClass.getAnnotation(VoMapping.class);
        return mapping != null && mapping.fields().length > 0;
    }
}
