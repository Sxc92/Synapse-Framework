package com.indigo.databases.utils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.indigo.core.entity.dto.QueryDTO;
import com.indigo.core.entity.vo.BaseVO;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 多表查询构建器
 * 支持复杂的多表关联查询
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Slf4j
@SuppressWarnings("rawtypes")
public class MultiTableQueryBuilder {
    
    /**
     * 构建多表查询SQL
     */
    public static <V extends BaseVO> String buildMultiTableSql(QueryDTO queryDTO, Class<V> voClass) {
        StringBuilder sql = new StringBuilder();
        
        // 1. 构建SELECT字段
        sql.append("SELECT ");
        String[] selectFields = EnhancedVoFieldSelector.getSelectFields(voClass);
        if (selectFields != null && selectFields.length > 0) {
            sql.append(String.join(", ", selectFields));
        } else {
            sql.append("*");
        }
        
        // 2. 构建FROM子句
        String mainTable = EnhancedVoFieldSelector.getMainTableName(voClass);
        String mainAlias = EnhancedVoFieldSelector.getMainTableAlias(voClass);
        sql.append(" FROM ").append(mainTable).append(" ").append(mainAlias);
        
        // 3. 构建JOIN子句
        String joinSql = EnhancedVoFieldSelector.buildJoinSql(voClass);
        if (StringUtils.isNotBlank(joinSql)) {
            sql.append(joinSql);
        }
        
        // 4. 构建WHERE条件
        String whereClause = buildWhereClause(queryDTO, voClass);
        if (StringUtils.isNotBlank(whereClause)) {
            sql.append(" WHERE ").append(whereClause);
        }
        
        // 5. 构建ORDER BY
        String orderByClause = buildOrderByClause(queryDTO);
        if (StringUtils.isNotBlank(orderByClause)) {
            sql.append(" ORDER BY ").append(orderByClause);
        }
        
        return sql.toString();
    }
    
    /**
     * 构建WHERE条件
     * 注意：多表查询时，需要将字段名转换为带表别名的形式（如：account -> u.account）
     * 同时需要将参数占位符替换为实际值，因为使用 ${sql} 时不会自动绑定参数
     */
    private static <V extends BaseVO> String buildWhereClause(QueryDTO queryDTO, Class<V> voClass) {
        try {
            // 使用QueryConditionBuilder构建WHERE条件
            QueryWrapper<?> wrapper = QueryConditionBuilder.buildQueryWrapper(queryDTO);
            String whereSql = wrapper.getSqlSegment();
            
            if (StringUtils.isNotBlank(whereSql)) {
                // 多表查询时，需要将字段名转换为带表别名的形式
                String mainAlias = EnhancedVoFieldSelector.getMainTableAlias(voClass);
                whereSql = addTableAliasToWhereClause(whereSql, mainAlias);
                
                // 将参数占位符替换为实际值（因为使用 ${sql} 时不会自动绑定参数）
                whereSql = replaceParameterPlaceholders(whereSql, wrapper);
                
                return whereSql;
            }
            
        } catch (Exception e) {
            log.error("构建WHERE条件失败", e);
        }
        
        return "";
    }
    
    /**
     * 替换参数占位符为实际值
     * 因为使用 ${sql} 动态SQL时，MyBatis不会自动绑定QueryWrapper的参数
     */
    private static String replaceParameterPlaceholders(String sqlSegment, QueryWrapper<?> wrapper) {
        // 获取QueryWrapper的参数映射
        Map<String, Object> paramNameValuePairs = wrapper.getParamNameValuePairs();
        
        if (paramNameValuePairs == null || paramNameValuePairs.isEmpty()) {
            log.debug("QueryWrapper参数映射为空，SQL: {}", sqlSegment);
            return sqlSegment;
        }
        
        log.debug("QueryWrapper参数映射: {}", paramNameValuePairs);
        log.debug("原始SQL片段: {}", sqlSegment);
        
        String result = sqlSegment;
        for (Map.Entry<String, Object> entry : paramNameValuePairs.entrySet()) {
            String paramName = entry.getKey();
            Object paramValue = entry.getValue();
            
            // MyBatis-Plus 的参数占位符格式：#{ew.paramNameValuePairs.paramName}
            String placeholder = "#{ew.paramNameValuePairs." + paramName + "}";
            String value = formatSqlValue(paramValue);
            
            log.debug("替换参数: {} = {} -> {}", placeholder, paramValue, value);
            result = result.replace(placeholder, value);
        }
        
        log.debug("替换后的SQL片段: {}", result);
        return result;
    }
    
    /**
     * 格式化SQL值
     */
    private static String formatSqlValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        
        if (value instanceof String) {
            // 转义单引号，防止SQL注入
            String escapedValue = value.toString().replace("'", "''");
            return "'" + escapedValue + "'";
        }
        
        if (value instanceof Number) {
            return value.toString();
        }
        
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "1" : "0";
        }
        
        // 其他类型转换为字符串
        return "'" + value.toString().replace("'", "''") + "'";
    }
    
    /**
     * 为WHERE子句中的字段添加表别名
     * 例如：account -> u.account, create_time -> u.create_time
     */
    private static String addTableAliasToWhereClause(String whereSql, String tableAlias) {
        if (StringUtils.isBlank(whereSql) || StringUtils.isBlank(tableAlias)) {
            return whereSql;
        }
        
        // 简单的字符串替换：将字段名替换为 表别名.字段名
        // 注意：这里假设字段名不会包含在字符串值中
        // 更安全的做法是使用正则表达式，但这里先使用简单替换
        
        // 检查字段名是否已经包含表别名
        // 如果 whereSql 中已经包含表别名模式（如 "u."），则不需要添加
        // 否则，将常见的字段名替换为 表别名.字段名
        if (!whereSql.contains(tableAlias + ".")) {
            // 将常见的字段名替换为 表别名.字段名
            // 注意：这是一个简化的实现，适用于大多数场景
            // 更完善的实现需要使用 SQL 解析器
            
            // 对于简单的 WHERE 条件，如 (account LIKE ?)，替换为 (u.account LIKE ?)
            // 使用正则表达式匹配字段名（不包含表别名的情况）
            whereSql = whereSql.replaceAll("\\b(account|user_name|create_time|modify_time|enabled|locked|expired|type|id)\\b", 
                tableAlias + ".$1");
        }
        
        return whereSql;
    }
    
    /**
     * 构建ORDER BY子句
     */
    private static String buildOrderByClause(QueryDTO queryDTO) {
        if (queryDTO.getOrderByList() == null || queryDTO.getOrderByList().isEmpty()) {
            return "";
        }
        
        List<String> orderByList = new ArrayList<>();
        for (Object orderByObj : queryDTO.getOrderByList()) {
            QueryDTO.OrderBy orderBy = (QueryDTO.OrderBy) orderByObj;
            if (StringUtils.isNotBlank(orderBy.getField())) {
                String columnName = convertFieldToColumn(orderBy.getField());
                String direction = "DESC".equalsIgnoreCase(orderBy.getDirection()) ? "DESC" : "ASC";
                orderByList.add(columnName + " " + direction);
            }
        }
        
        return String.join(", ", orderByList);
    }
    
    /**
     * 字段名转列名
     */
    private static String convertFieldToColumn(String fieldName) {
        return StringUtils.camelToUnderline(fieldName);
    }
}
