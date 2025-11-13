package com.indigo.core.utils;

import com.indigo.core.context.UserContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 用户上下文持有者（已废弃）
 * 
 * <p>此类已废弃，请使用 {@link UserContext} 的静态方法替代：
 * <ul>
 *   <li>{@link UserContext#getCurrentUser()} - 替代 {@link #getCurrentUser()}</li>
 *   <li>{@link UserContext#getCurrentUserId()} - 替代 {@link #getCurrentUserId()}（注意：返回类型为 String）</li>
 *   <li>{@link UserContext#getCurrentAccount()} - 替代 {@link #getCurrentUsername()}</li>
 *   <li>{@link UserContext#getCurrentRealName()} - 获取用户真实姓名</li>
 *   <li>{@link UserContext#getCurrentEmail()} - 获取用户邮箱</li>
 *   <li>{@link UserContext#getCurrentMobile()} - 获取用户手机号</li>
 *   <li>{@link UserContext#getCurrentAvatar()} - 获取用户头像URL</li>
 *   <li>{@link UserContext#getCurrentRoles()} - 替代 {@link #getCurrentUserRoles()}</li>
 *   <li>{@link UserContext#getCurrentPermissions()} - 替代 {@link #getCurrentUserPermissions()}</li>
 *   <li>{@link UserContext#hasRole(String)} - 替代 {@link #hasRole(String)}</li>
 *   <li>{@link UserContext#hasPermission(String)} - 替代 {@link #hasPermission(String)}</li>
 *   <li>{@link UserContext#hasAnyRole(String...)} - 替代 {@link #hasAnyRole(String...)}</li>
 *   <li>{@link UserContext#hasAnyPermission(String...)} - 替代 {@link #hasAnyPermission(String...)}</li>
 *   <li>{@link UserContext#hasAllRoles(String...)} - 替代 {@link #hasAllRoles(String...)}</li>
 *   <li>{@link UserContext#hasAllPermissions(String...)} - 替代 {@link #hasAllPermissions(String...)}</li>
 * </ul>
 * 
 * <p>此类将在后续版本中移除。当前实现已委托给 {@link UserContext} 的静态方法，确保向后兼容。
 *
 * @author 史偕成
 * @date 2025/12/19
 * @deprecated 请使用 {@link UserContext} 的静态方法替代
 */
@Slf4j
@Deprecated
public class UserContextHolder {

    /**
     * 获取当前用户上下文
     * 
     * @deprecated 请使用 {@link UserContext#getCurrentUser()} 替代
     */
    @Deprecated
    public static UserContext getCurrentUser() {
        return UserContext.getCurrentUser();
    }

    /**
     * 获取当前用户ID
     * 
     * @deprecated 请使用 {@link UserContext#getCurrentUserId()} 替代（注意：返回类型为 String）
     */
    @Deprecated
    public static Long getCurrentUserId() {
        String userId = UserContext.getCurrentUserId();
        if (userId == null) {
                return null;
            }
        try {
            return Long.valueOf(userId);
        } catch (NumberFormatException e) {
            log.warn("用户ID格式转换失败: {}", userId, e);
            return null;
        }
    }

    /**
     * 获取当前用户名
     * 
     * @deprecated 请使用 {@link UserContext#getCurrentAccount()} 替代
     */
    @Deprecated
    public static String getCurrentUsername() {
        return UserContext.getCurrentAccount();
    }

    /**
     * 获取当前租户ID
     * 
     * @deprecated UserContext 中没有 tenantId 字段，请直接从 UserContext 获取
     */
    @Deprecated
    public static Long getCurrentTenantId() {
        UserContext userContext = UserContext.getCurrentUser();
        if (userContext == null) {
            return null;
        }
        // UserContext 中没有 tenantId 字段，返回 null
        // 如果需要，可以从 userContext 的其他属性获取
        return null;
    }

    /**
     * 获取当前部门ID
     * 
     * @deprecated UserContext 中没有 deptId 字段，请直接从 UserContext 获取
     */
    @Deprecated
    public static Long getCurrentDeptId() {
        // UserContext 中没有 deptId 字段，返回 null
        // 如果需要，可以从 userContext 的其他属性获取
                return null;
    }

    /**
     * 获取当前用户角色列表
     * 
     * @deprecated 请使用 {@link UserContext#getCurrentRoles()} 替代
     */
    @Deprecated
    public static List<String> getCurrentUserRoles() {
        return UserContext.getCurrentRoles();
    }

    /**
     * 获取当前用户权限列表
     * 
     * @deprecated 请使用 {@link UserContext#getCurrentPermissions()} 替代
     */
    @Deprecated
    public static List<String> getCurrentUserPermissions() {
        return UserContext.getCurrentPermissions();
    }

    /**
     * 检查当前用户是否有指定角色
     * 
     * @deprecated 请使用 {@link UserContext#hasRole(String)} 替代
     */
    @Deprecated
    public static boolean hasRole(String role) {
        return UserContext.hasRole(role);
    }

    /**
     * 检查当前用户是否有指定权限
     * 
     * @deprecated 请使用 {@link UserContext#hasPermission(String)} 替代
     */
    @Deprecated
    public static boolean hasPermission(String permission) {
        return UserContext.hasPermission(permission);
    }

    /**
     * 检查当前用户是否有任一指定角色
     * 
     * @deprecated 请使用 {@link UserContext#hasAnyRole(String...)} 替代
     */
    @Deprecated
    public static boolean hasAnyRole(String... roles) {
        return UserContext.hasAnyRole(roles);
    }

    /**
     * 检查当前用户是否有任一指定权限
     * 
     * @deprecated 请使用 {@link UserContext#hasAnyPermission(String...)} 替代
     */
    @Deprecated
    public static boolean hasAnyPermission(String... permissions) {
        return UserContext.hasAnyPermission(permissions);
    }

    /**
     * 检查当前用户是否有所有指定角色
     * 
     * @deprecated 请使用 {@link UserContext#hasAllRoles(String...)} 替代
     */
    @Deprecated
    public static boolean hasAllRoles(String... roles) {
        return UserContext.hasAllRoles(roles);
    }

    /**
     * 检查当前用户是否有所有指定权限
     * 
     * @deprecated 请使用 {@link UserContext#hasAllPermissions(String...)} 替代
     */
    @Deprecated
    public static boolean hasAllPermissions(String... permissions) {
        return UserContext.hasAllPermissions(permissions);
    }
}
