package com.indigo.databases.utils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.indigo.core.entity.dto.QueryDTO;
import com.indigo.core.entity.vo.BaseVO;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

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
     * 注意：这里应该使用QueryConditionBuilder来构建WHERE条件，而不是直接从VO类获取
     */
    private static <V extends BaseVO> String buildWhereClause(QueryDTO queryDTO, Class<V> voClass) {
        try {
            // 使用QueryConditionBuilder构建WHERE条件
            QueryWrapper<?> wrapper = QueryConditionBuilder.buildQueryWrapper(queryDTO);
            String whereSql = wrapper.getSqlSegment();
            
            if (StringUtils.isNotBlank(whereSql)) {
                return whereSql;
            }
            
        } catch (Exception e) {
            log.error("构建WHERE条件失败", e);
        }
        
        return "";
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
