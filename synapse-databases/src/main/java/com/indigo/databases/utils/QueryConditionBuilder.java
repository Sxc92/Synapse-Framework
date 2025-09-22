package com.indigo.databases.utils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.indigo.core.annotation.QueryCondition;
import com.indigo.core.entity.dto.PageDTO;
import com.indigo.core.entity.dto.QueryDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

/**
 * 查询条件构建器
 * 根据实体类字段的@QueryCondition注解自动构建QueryWrapper
 * 充分利用 MyBatis-Plus 的配置，避免重复造轮子
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Slf4j
@Component
public class QueryConditionBuilder {

    /**
     * 根据QueryDTO对象构建查询条件（推荐使用）
     */
    public static <T> QueryWrapper<T> buildQueryWrapper(QueryDTO queryDTO) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();

        if (queryDTO != null) {
            addEntityConditions(wrapper, queryDTO);
            addOrderByConditions(wrapper, queryDTO);
        }

        return wrapper;
    }

    /**
     * 根据PageDTO对象构建查询条件（推荐使用）
     */
    public static <T> QueryWrapper<T> buildQueryWrapper(PageDTO queryDTO) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();

        if (queryDTO != null) {
            addEntityConditions(wrapper, queryDTO);
            addOrderByConditions(wrapper, queryDTO);
        }

        return wrapper;
    }

    /**
     * 根据实体对象构建查询条件
     */
    public static <T> QueryWrapper<T> buildQueryWrapper(T entity) {
        return buildQueryWrapper(entity, null);
    }

    /**
     * 根据实体对象和额外条件构建查询条件
     */
    public static <T> QueryWrapper<T> buildQueryWrapper(T entity, Map<String, Object> extraConditions) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        
        if (entity != null) {
            addEntityConditions(wrapper, entity);
        }
        
        if (extraConditions != null && !extraConditions.isEmpty()) {
            addExtraConditions(wrapper, extraConditions);
        }
        
        return wrapper;
    }

    /**
     * 仅根据Map条件构建查询条件
     */
    public static <T> QueryWrapper<T> buildQueryWrapper(Map<String, Object> conditions) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        
        if (conditions != null && !conditions.isEmpty()) {
            addExtraConditions(wrapper, conditions);
        }
        
        return wrapper;
    }

    /**
     * 添加实体类字段的查询条件
     */
    private static <T> void addEntityConditions(QueryWrapper<T> wrapper, Object entity) {
        try {
            Class<?> entityClass = entity.getClass();
            Field[] fields = entityClass.getDeclaredFields();
            
            for (Field field : fields) {
                QueryCondition annotation = field.getAnnotation(QueryCondition.class);
                if (annotation != null) {
                    field.setAccessible(true);
                    Object value = field.get(entity);
                    
                    if (shouldIncludeValue(value, annotation)) {
                        addCondition(wrapper, field, value, annotation, entityClass);
                    }
                }
            }
        } catch (Exception e) {
            log.error("构建实体查询条件失败", e);
        }
    }

    /**
     * 添加排序条件
     */
    private static <T> void addOrderByConditions(QueryWrapper<T> wrapper, QueryDTO queryDTO) {
        if (queryDTO.getOrderByList() != null && !queryDTO.getOrderByList().isEmpty()) {
            for (QueryDTO.OrderBy orderBy : queryDTO.getOrderByList()) {
                if (StringUtils.isNotBlank(orderBy.getField())) {
                    String columnName = convertFieldToColumn(orderBy.getField());
                    if ("DESC".equalsIgnoreCase(orderBy.getDirection())) {
                        wrapper.orderByDesc(columnName);
                    } else {
                        wrapper.orderByAsc(columnName);
                    }
                }
            }
        }
    }

    /**
     * 添加额外条件的查询条件
     */
    private static <T> void addExtraConditions(QueryWrapper<T> wrapper, Map<String, Object> conditions) {
        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value != null) {
                // 使用 MyBatis-Plus 的 StringUtils 进行判断
                if (value instanceof String && StringUtils.isNotBlank((String) value)) {
                    String strValue = (String) value;
                    String columnName = convertFieldToColumn(key);
                    if (strValue.contains("%")) {
                        wrapper.like(columnName, value);
                    } else {
                        wrapper.eq(columnName, value);
                    }
                } else if (value instanceof Collection && !((Collection<?>) value).isEmpty()) {
                    String columnName = convertFieldToColumn(key);
                    wrapper.in(columnName, (Collection<?>) value);
                } else if (!(value instanceof String)) {
                    String columnName = convertFieldToColumn(key);
                    wrapper.eq(columnName, value);
                }
            }
        }
    }

    /**
     * 添加单个查询条件
     */
    private static <T> void addCondition(QueryWrapper<T> wrapper, Field field, Object value, QueryCondition annotation, Class<?> entityClass) {
        try {
            String fieldName = StringUtils.isNotBlank(annotation.field()) ? annotation.field() : field.getName();
            String columnName = convertFieldToColumn(fieldName);
            
            switch (annotation.type()) {
                case EQ:
                    wrapper.eq(columnName, value);
                    break;
                case NE:
                    wrapper.ne(columnName, value);
                    break;
                case LIKE:
                    wrapper.like(columnName, value);
                    break;
                case LIKE_LEFT:
                    wrapper.likeLeft(columnName, value);
                    break;
                case LIKE_RIGHT:
                    wrapper.likeRight(columnName, value);
                    break;
                case GT:
                    wrapper.gt(columnName, value);
                    break;
                case GE:
                    wrapper.ge(columnName, value);
                    break;
                case LT:
                    wrapper.lt(columnName, value);
                    break;
                case LE:
                    wrapper.le(columnName, value);
                    break;
                case IN:
                    if (value instanceof Collection) {
                        wrapper.in(columnName, (Collection<?>) value);
                    }
                    break;
                case NOT_IN:
                    if (value instanceof Collection) {
                        wrapper.notIn(columnName, (Collection<?>) value);
                    }
                    break;
                case BETWEEN:
                    // BETWEEN需要特殊处理，通常需要两个值
                    if (value instanceof Object[] && ((Object[]) value).length == 2) {
                        Object[] range = (Object[]) value;
                        wrapper.between(columnName, range[0], range[1]);
                    }
                    break;
                case IS_NULL:
                    wrapper.isNull(columnName);
                    break;
                case IS_NOT_NULL:
                    wrapper.isNotNull(columnName);
                    break;
            }
        } catch (Exception e) {
            log.error("添加查询条件失败: field={}, value={}", field.getName(), value, e);
        }
    }

    /**
     * 判断是否应该包含该值
     */
    private static boolean shouldIncludeValue(Object value, QueryCondition annotation) {
        if (value == null) {
            return !annotation.ignoreNull();
        }
        
        if (value instanceof String) {
            String strValue = (String) value;
            if (StringUtils.isBlank(strValue) && annotation.ignoreEmpty()) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 字段名转换为列名
     * 使用 MyBatis-Plus 的命名策略，充分利用配置
     */
    private static String convertFieldToColumn(String fieldName) {
        // 使用 MyBatis-Plus 的 StringUtils 进行驼峰转下划线转换
        // 这里可以根据 MyBatis-Plus 的配置来决定使用哪种转换方式
        return StringUtils.camelToUnderline(fieldName);
    }
} 