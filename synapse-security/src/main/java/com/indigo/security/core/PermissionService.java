package com.indigo.security.core;

import com.indigo.core.context.UserContext;
import com.indigo.core.exception.Ex;
import com.indigo.security.annotation.Logical;
import com.indigo.security.config.SecurityAutoConfiguration;
import com.indigo.security.constants.SecurityError;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;

/**
 * 权限检查服务
 * 提供权限和角色的检查方法，替代 Sa-Token 的权限检查功能
 * 
 * <p><b>设计说明：</b>
 * <ul>
 *   <li>从 UserContext 获取权限和角色信息（ThreadLocal）</li>
 *   <li>支持 AND/OR 逻辑判断</li>
 *   <li>检查失败时抛出相应异常</li>
 * </ul>
 * 
 * <p><b>历史说明：</b>
 * 之前使用 PermissionManager 实现 Sa-Token 的 StpInterface，现已移除 Sa-Token 依赖
 * PermissionService 用于业务代码直接调用，提供更简洁的 API
 * 
 * <p><b>注意：</b>此服务不依赖任何 Bean，只使用 ThreadLocal 中的 UserContext。
 * 通过 {@link SecurityAutoConfiguration} 中的 @Bean 方法创建，避免条件加载问题。
 * 
 * @author 史偕成
 * @date 2025/01/XX
 */
@Slf4j
public class PermissionService {

    /**
     * 检查用户是否已登录
     * 
     * @throws com.indigo.core.exception.SynapseException 如果用户未登录
     */
    public void checkLogin() {
        UserContext userContext = UserContext.getCurrentUser();
        if (userContext == null) {
            Ex.throwEx(SecurityError.NOT_LOGIN);
        }
        log.debug("登录检查通过: userId={}", userContext.getUserId());
    }

    /**
     * 检查用户是否有指定角色
     * 
     * @param roles  需要的角色列表
     * @param logical 逻辑运算符（AND/OR）
     * @throws com.indigo.core.exception.SynapseException 如果用户未登录或没有所需角色
     */
    public void checkRole(String[] roles, Logical logical) {
        if (roles == null || roles.length == 0) {
            Ex.throwEx(SecurityError.PERMISSION_DENIED, "角色列表不能为空");
        }

        UserContext userContext = UserContext.getCurrentUser();
        if (userContext == null) {
            Ex.throwEx(SecurityError.NOT_LOGIN);
        }

        List<String> userRoles = userContext.getRoles();
        if (userRoles == null || userRoles.isEmpty()) {
            log.warn("用户没有角色: userId={}", userContext.getUserId());
            Ex.throwEx(SecurityError.PERMISSION_DENIED, "用户没有所需角色");
        }

        boolean hasRole;
        if (logical == Logical.AND) {
            // AND 逻辑：需要所有角色
            hasRole = new HashSet<>(userRoles).containsAll(List.of(roles));
        } else {
            // OR 逻辑：需要任一角色
            hasRole = userRoles.stream().anyMatch(List.of(roles)::contains);
        }

        if (!hasRole) {
            log.warn("用户角色不足: userId={}, required={}, userRoles={}", 
                    userContext.getUserId(), List.of(roles), userRoles);
            Ex.throwEx(SecurityError.PERMISSION_DENIED, "用户没有所需角色");
        }

        log.debug("角色检查通过: userId={}, required={}, userRoles={}", 
                userContext.getUserId(), List.of(roles), userRoles);
    }

    /**
     * 检查用户是否有指定权限
     * 
     * @param permissions 需要的权限列表
     * @param logical     逻辑运算符（AND/OR）
     * @throws com.indigo.core.exception.SynapseException 如果用户未登录或没有所需权限
     */
    public void checkPermission(String[] permissions, Logical logical) {
        if (permissions == null || permissions.length == 0) {
            log.warn("权限列表为空");
            Ex.throwEx(SecurityError.PERMISSION_DENIED, "权限列表不能为空");
        }

        UserContext userContext = UserContext.getCurrentUser();
        if (userContext == null) {
            log.warn("用户未登录");
            Ex.throwEx(SecurityError.NOT_LOGIN);
        }

        List<String> userPermissions = userContext.getPermissions();
        if (userPermissions == null || userPermissions.isEmpty()) {
            log.warn("用户没有权限: userId={}", userContext.getUserId());
            Ex.throwEx(SecurityError.PERMISSION_DENIED, "用户没有所需权限");
        }

        boolean hasPermission;
        if (logical == Logical.AND) {
            // AND 逻辑：需要所有权限
            hasPermission = new HashSet<>(userPermissions).containsAll(List.of(permissions));
        } else {
            // OR 逻辑：需要任一权限
            hasPermission = userPermissions.stream().anyMatch(List.of(permissions)::contains);
        }

        if (!hasPermission) {
            log.warn("用户权限不足: userId={}, required={}, userPermissions={}", 
                    userContext.getUserId(), List.of(permissions), userPermissions);
            Ex.throwEx(SecurityError.PERMISSION_DENIED, "用户没有所需权限");
        }

        log.debug("权限检查通过: userId={}, required={}, userPermissions={}", 
                userContext.getUserId(), List.of(permissions), userPermissions);
    }

    /**
     * 检查用户是否有指定角色（辅助方法，用于业务代码直接调用）
     * 
     * @param role 需要的角色
     * @throws com.indigo.core.exception.SynapseException 如果用户未登录或没有所需角色
     */
    public void checkRole(String role) {
        checkRole(new String[]{role}, Logical.OR);
    }

    /**
     * 检查用户是否有指定权限（辅助方法，用于业务代码直接调用）
     * 
     * @param permission 需要的权限
     * @throws com.indigo.core.exception.SynapseException 如果用户未登录或没有所需权限
     */
    public void checkPermission(String permission) {
        checkPermission(new String[]{permission}, Logical.OR);
    }
}

