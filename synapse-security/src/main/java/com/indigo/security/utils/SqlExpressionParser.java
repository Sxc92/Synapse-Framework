package com.indigo.security.utils;

import com.indigo.core.context.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL表达式解析工具类
 * 用于解析和替换数据权限SQL表达式中的动态参数
 *
 * @author 史偕成
 * @date 2025/01/09
 */
@Slf4j
public class SqlExpressionParser {

    /**
     * 参数占位符正则表达式
     */
    private static final Pattern PARAM_PATTERN = Pattern.compile("#\\{([^}]+)\\}");

    /**
     * 替换SQL表达式中的动态参数
     *
     * @param expression SQL表达式
     * @param user 用户上下文
     * @return 替换后的SQL表达式
     */
    public static String replaceParameters(String expression, UserContext user) {
        if (!StringUtils.hasText(expression) || user == null) {
            return expression;
        }

        try {
            // 构建参数映射
            Map<String, String> parameters = buildParameterMap(user);
            
            // 替换参数
            return replaceParameters(expression, parameters);
            
        } catch (Exception e) {
            log.error("SQL表达式参数替换失败: expression={}, userId={}", expression, user.getUserId(), e);
            return expression;
        }
    }

    /**
     * 替换SQL表达式中的动态参数
     *
     * @param expression SQL表达式
     * @param parameters 参数映射
     * @return 替换后的SQL表达式
     */
    public static String replaceParameters(String expression, Map<String, String> parameters) {
        if (!StringUtils.hasText(expression) || parameters == null) {
            return expression;
        }

        String result = expression;
        Matcher matcher = PARAM_PATTERN.matcher(expression);
        
        while (matcher.find()) {
            String placeholder = matcher.group(0); // #{paramName}
            String paramName = matcher.group(1);  // paramName
            
            String paramValue = parameters.get(paramName);
            if (paramValue != null) {
                result = result.replace(placeholder, paramValue);
            } else {
                log.warn("未找到参数值: paramName={}, expression={}", paramName, expression);
                // 如果找不到参数值，替换为空字符串或抛出异常
                result = result.replace(placeholder, "''");
            }
        }
        
        return result;
    }

    /**
     * 构建用户参数映射
     *
     * @param user 用户上下文
     * @return 参数映射
     */
    public static Map<String, String> buildParameterMap(UserContext user) {
        Map<String, String> parameters = new HashMap<>();
        
        if (user == null) {
            return parameters;
        }

        // 基础用户信息
        parameters.put("userId", user.getUserId());
        parameters.put("username", user.getUsername());
        parameters.put("nickname", user.getNickname());
        parameters.put("email", user.getEmail());
        parameters.put("phone", user.getPhone());
        parameters.put("tenantId", user.getTenantId());
        parameters.put("tenantName", user.getTenantName());

        // 部门信息
        parameters.put("deptId", user.getDeptId());
        parameters.put("deptName", user.getDeptName());
        parameters.put("deptPath", user.getDeptPath());

        // 职级信息
        parameters.put("positionId", user.getPositionId());
        parameters.put("positionName", user.getPositionName());
        parameters.put("positionLevel", String.valueOf(user.getPositionLevel()));

        // 角色和权限信息
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            parameters.put("roles", "'" + String.join("','", user.getRoles()) + "'");
        }
        
        if (user.getPermissions() != null && !user.getPermissions().isEmpty()) {
            parameters.put("permissions", "'" + String.join("','", user.getPermissions()) + "'");
        }

        // 部门职级组合信息
        if (user.getDeptPositions() != null && !user.getDeptPositions().isEmpty()) {
            StringBuilder deptIds = new StringBuilder();
            StringBuilder positionIds = new StringBuilder();
            StringBuilder deptPositionIds = new StringBuilder();
            
            for (UserContext.UserDeptPositionInfo deptPosition : user.getDeptPositions()) {
                if (deptIds.length() > 0) deptIds.append(",");
                if (positionIds.length() > 0) positionIds.append(",");
                if (deptPositionIds.length() > 0) deptPositionIds.append(",");
                
                deptIds.append("'").append(deptPosition.getDeptId()).append("'");
                positionIds.append("'").append(deptPosition.getPositionId()).append("'");
                deptPositionIds.append("'").append(deptPosition.getDeptId()).append(":")
                    .append(deptPosition.getPositionId()).append("'");
            }
            
            parameters.put("deptIds", deptIds.toString());
            parameters.put("positionIds", positionIds.toString());
            parameters.put("deptPositionIds", deptPositionIds.toString());
        }

        return parameters;
    }

    /**
     * 验证SQL表达式是否包含有效的参数占位符
     *
     * @param expression SQL表达式
     * @return 是否包含有效参数
     */
    public static boolean hasValidParameters(String expression) {
        if (!StringUtils.hasText(expression)) {
            return false;
        }
        
        Matcher matcher = PARAM_PATTERN.matcher(expression);
        return matcher.find();
    }

    /**
     * 提取SQL表达式中的所有参数名
     *
     * @param expression SQL表达式
     * @return 参数名列表
     */
    public static java.util.List<String> extractParameterNames(String expression) {
        java.util.List<String> parameterNames = new java.util.ArrayList<>();
        
        if (!StringUtils.hasText(expression)) {
            return parameterNames;
        }
        
        Matcher matcher = PARAM_PATTERN.matcher(expression);
        while (matcher.find()) {
            String paramName = matcher.group(1);
            if (!parameterNames.contains(paramName)) {
                parameterNames.add(paramName);
            }
        }
        
        return parameterNames;
    }

    /**
     * 检查参数是否完整
     *
     * @param expression SQL表达式
     * @param parameters 参数映射
     * @return 是否参数完整
     */
    public static boolean isParametersComplete(String expression, Map<String, String> parameters) {
        if (!StringUtils.hasText(expression) || parameters == null) {
            return true;
        }
        
        java.util.List<String> requiredParams = extractParameterNames(expression);
        for (String paramName : requiredParams) {
            if (!parameters.containsKey(paramName) || !StringUtils.hasText(parameters.get(paramName))) {
                log.warn("缺少必需参数: paramName={}, expression={}", paramName, expression);
                return false;
            }
        }
        
        return true;
    }

    /**
     * 安全地构建SQL IN条件
     *
     * @param values 值列表
     * @return SQL IN条件
     */
    public static String buildInCondition(java.util.List<String> values) {
        if (values == null || values.isEmpty()) {
            return "1=0";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("'").append(values.get(i)).append("'");
        }
        sb.append(")");
        
        return sb.toString();
    }

    /**
     * 安全地构建SQL LIKE条件
     *
     * @param value 值
     * @param pattern 模式（%value%）
     * @return SQL LIKE条件
     */
    public static String buildLikeCondition(String value, String pattern) {
        if (!StringUtils.hasText(value)) {
            return "1=0";
        }
        
        String escapedValue = value.replace("'", "''");
        return "'" + escapedValue + "'";
    }
}
