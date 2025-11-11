package com.indigo.security.service;

import com.indigo.core.context.UserContext;
import com.indigo.security.constants.PermissionCode;
import com.indigo.security.model.DataPermissionRule;
import com.indigo.security.utils.SqlExpressionParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限校验服务
 * 基于权限编码的灵活权限校验
 *
 * @author 史偕成
 * @date 2025/01/09
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(DataPermissionService.class)
public class PermissionValidationService {

    private final DataPermissionService dataPermissionService;

    /**
     * 检查用户是否有指定权限编码的权限
     *
     * @param permissionCode 权限编码
     * @return 是否有权限
     */
    public boolean hasPermission(String permissionCode) {
        UserContext user = UserContext.getCurrentUser();
        if (user == null) {
            log.warn("权限校验失败: 用户上下文为空, permissionCode={}", permissionCode);
            return false;
        }
        return hasPermission(user, permissionCode);
    }

    /**
     * 检查用户是否有指定权限编码的权限
     *
     * @param user 用户上下文
     * @param permissionCode 权限编码
     * @return 是否有权限
     */
    public boolean hasPermission(UserContext user, String permissionCode) {
        if (user == null || !StringUtils.hasText(permissionCode)) {
            log.warn("权限校验失败: 参数为空, userId={}, permissionCode={}", 
                user != null ? user.getUserId() : null, permissionCode);
            return false;
        }

        try {
            // 1. 获取用户所有权限规则
            List<DataPermissionRule> rules = getAllUserRules(user);

            // 2. 检查是否有匹配的权限规则
            boolean hasPermission = rules.stream()
                .filter(DataPermissionRule::getEnabled)
                .anyMatch(rule -> PermissionCode.matches(rule.getPermissionCode(), permissionCode));

            log.debug("权限校验结果: userId={}, permissionCode={}, hasPermission={}", 
                user.getUserId(), permissionCode, hasPermission);

            return hasPermission;

        } catch (Exception e) {
            log.error("权限校验异常: userId={}, permissionCode={}", user.getUserId(), permissionCode, e);
            return false;
        }
    }

    /**
     * 获取用户对指定资源的数据权限SQL条件
     *
     * @param resourceType 资源类型
     * @return 数据权限SQL条件
     */
    public String getDataScopeSql(String resourceType) {
        UserContext user = UserContext.getCurrentUser();
        if (user == null) {
            log.warn("获取数据权限失败: 用户上下文为空, resourceType={}", resourceType);
            return "1=0";
        }
        return getDataScopeSql(user, resourceType);
    }

    /**
     * 获取用户对指定资源的数据权限SQL条件
     *
     * @param user 用户上下文
     * @param resourceType 资源类型
     * @return 数据权限SQL条件
     */
    public String getDataScopeSql(UserContext user, String resourceType) {
        if (user == null || !StringUtils.hasText(resourceType)) {
            log.warn("获取数据权限失败: 参数为空, userId={}, resourceType={}", 
                user != null ? user.getUserId() : null, resourceType);
            return "1=0";
        }

        try {
            // 1. 获取用户所有权限规则
            List<DataPermissionRule> rules = getAllUserRules(user);

            // 2. 找到匹配资源类型的规则
            Optional<DataPermissionRule> matchingRule = rules.stream()
                .filter(rule -> rule.getEnabled())
                .filter(rule -> resourceType.equals(rule.getResourceType()))
                .min(Comparator.comparing(DataPermissionRule::getPriority));

            if (matchingRule.isPresent()) {
                String dataScopeSql = buildDataScopeSql(matchingRule.get(), user);
                log.debug("数据权限SQL: userId={}, resourceType={}, sql={}", 
                    user.getUserId(), resourceType, dataScopeSql);
                return dataScopeSql;
            }

            log.debug("未找到匹配的数据权限规则: userId={}, resourceType={}", 
                user.getUserId(), resourceType);
            return "1=0";

        } catch (Exception e) {
            log.error("获取数据权限异常: userId={}, resourceType={}", user.getUserId(), resourceType, e);
            return "1=0";
        }
    }

    /**
     * 批量检查用户对多个权限编码的权限
     *
     * @param permissionCodes 权限编码列表
     * @return 权限编码到权限结果的映射
     */
    public Map<String, Boolean> batchCheckPermissions(List<String> permissionCodes) {
        UserContext user = UserContext.getCurrentUser();
        if (user == null) {
            log.warn("批量权限校验失败: 用户上下文为空");
            return permissionCodes.stream()
                .collect(Collectors.toMap(code -> code, code -> false));
        }
        return batchCheckPermissions(user, permissionCodes);
    }

    /**
     * 批量检查用户对多个权限编码的权限
     *
     * @param user 用户上下文
     * @param permissionCodes 权限编码列表
     * @return 权限编码到权限结果的映射
     */
    public Map<String, Boolean> batchCheckPermissions(UserContext user, List<String> permissionCodes) {
        if (user == null || permissionCodes == null || permissionCodes.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            // 1. 获取用户所有权限规则
            List<DataPermissionRule> rules = getAllUserRules(user);

            // 2. 批量检查权限
            return permissionCodes.stream()
                .collect(Collectors.toMap(
                    permissionCode -> permissionCode,
                    permissionCode -> rules.stream()
                        .filter(rule -> rule.getEnabled())
                        .anyMatch(rule -> PermissionCode.matches(rule.getPermissionCode(), permissionCode))
                ));

        } catch (Exception e) {
            log.error("批量权限校验异常: userId={}", user.getUserId(), e);
            return permissionCodes.stream()
                .collect(Collectors.toMap(code -> code, code -> false));
        }
    }

    /**
     * 获取用户所有适用的权限规则
     *
     * @param user 用户上下文
     * @return 权限规则列表
     */
    private List<DataPermissionRule> getAllUserRules(UserContext user) {
        List<DataPermissionRule> allRules = new ArrayList<>();

        // 1. 添加用户特定规则
        if (StringUtils.hasText(user.getUserId())) {
            allRules.addAll(dataPermissionService.getUserRules(user.getUserId()));
        }

        // 2. 添加角色规则
        if (user.getRoles() != null) {
            for (String role : user.getRoles()) {
                try {
                    allRules.addAll(dataPermissionService.getRoleRules(role));
                } catch (Exception e) {
                    log.warn("获取角色权限规则失败: roleId={}", role, e);
                }
            }
        }

        // 3. 添加部门规则
        if (StringUtils.hasText(user.getDeptId())) {
            allRules.addAll(dataPermissionService.getDepartmentRules(user.getDeptId()));
        }

        // 4. 添加职级规则
        if (StringUtils.hasText(user.getPositionId())) {
            allRules.addAll(getPositionRules(user.getPositionId()));
        }

        // 5. 添加部门职级组合规则
        if (StringUtils.hasText(user.getDeptId()) && StringUtils.hasText(user.getPositionId())) {
            String deptPositionId = user.getDeptId() + ":" + user.getPositionId();
            allRules.addAll(getDeptPositionRules(deptPositionId));
        }

        return allRules;
    }

    /**
     * 构建数据权限SQL条件
     *
     * @param rule 权限规则
     * @param user 用户上下文
     * @return SQL条件
     */
    private String buildDataScopeSql(DataPermissionRule rule, UserContext user) {
        // 优先使用自定义SQL表达式
        if (StringUtils.hasText(rule.getDataScopeExpression())) {
            return replaceDynamicParameters(rule.getDataScopeExpression(), user);
        }

        // 使用默认的数据范围类型
        return buildDefaultDataScopeSql(rule.getDataScopeType(), user);
    }

    /**
     * 构建默认数据权限SQL
     *
     * @param dataScopeType 数据范围类型
     * @param user 用户上下文
     * @return SQL条件
     */
    private String buildDefaultDataScopeSql(DataPermissionRule.DataScopeType dataScopeType, UserContext user) {
        return switch (dataScopeType) {
            case ALL -> "1=1";
            case DEPARTMENT -> "dept_id = '" + user.getDeptId() + "'";
            case DEPARTMENT_AND_BELOW -> 
                "dept_id IN (SELECT id FROM iam_department WHERE path LIKE '%" + user.getDeptId() + "%')";
            case POSITION_AND_BELOW -> 
                "position_id IN (SELECT id FROM iam_position WHERE level >= " + user.getPositionLevel() + ")";
            case PERSONAL -> "create_user_id = '" + user.getUserId() + "'";
            case CUSTOM -> "1=0";
        };
    }

    /**
     * 替换动态参数
     *
     * @param expression SQL表达式
     * @param user 用户上下文
     * @return 替换后的SQL表达式
     */
    private String replaceDynamicParameters(String expression, UserContext user) {
        return SqlExpressionParser.replaceParameters(expression, user);
    }

    /**
     * 获取职级权限规则
     *
     * @param positionId 职级ID
     * @return 权限规则列表
     */
    private List<DataPermissionRule> getPositionRules(String positionId) {
        // TODO: 实现职级权限规则查询
        // 这里需要根据实际的数据库表结构来实现
        return Collections.emptyList();
    }

    /**
     * 获取部门职级组合权限规则
     *
     * @param deptPositionId 部门职级组合ID
     * @return 权限规则列表
     */
    private List<DataPermissionRule> getDeptPositionRules(String deptPositionId) {
        // TODO: 实现部门职级组合权限规则查询
        // 这里需要根据实际的数据库表结构来实现
        return Collections.emptyList();
    }
}
