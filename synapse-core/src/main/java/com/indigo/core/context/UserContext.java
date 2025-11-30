package com.indigo.core.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 用户上下文信息
 * 用于存储JWT token中的用户信息
 * 实现 Serializable 接口以支持 Redis 序列化
 *
 * @author 史偕成
 * @date 2025/03/21
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 线程本地变量，用于存储当前用户上下文
     */
    private static final ThreadLocal<UserContext> USER_CONTEXT_HOLDER = new ThreadLocal<>();


    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户名
     */
    private String account;

    /**
     * 用户昵称
     */
    private String realName;

    /**
     * 电子邮箱
     */
    private String email;

    /**
     * 手机号码
     */
    private String mobile;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 获取当前用户上下文
     *
     * @return 当前用户上下文
     */
    public static UserContext getCurrentUser() {
        return USER_CONTEXT_HOLDER.get();
    }

    /**
     * 设置当前用户上下文
     *
     * @param userContext 用户上下文
     */
    public static void setCurrentUser(UserContext userContext) {
        USER_CONTEXT_HOLDER.set(userContext);
    }

    /**
     * 清除当前用户上下文
     */
    public static void clearCurrentUser() {
        USER_CONTEXT_HOLDER.remove();
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID，如果没有当前用户则返回null
     */
    public static String getCurrentUserId() {
        UserContext userContext = getCurrentUser();
        return userContext != null ? userContext.getUserId() : null;
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名，如果没有当前用户则返回null
     */
    public static String getCurrentAccount() {
        UserContext userContext = getCurrentUser();
        return userContext != null ? userContext.getAccount() : null;
    }

    /**
     * 获取当前用户真实姓名
     *
     * @return 用户真实姓名，如果没有当前用户则返回null
     */
    public static String getCurrentRealName() {
        UserContext userContext = getCurrentUser();
        return userContext != null ? userContext.getRealName() : null;
    }

    /**
     * 获取当前用户邮箱
     *
     * @return 用户邮箱，如果没有当前用户则返回null
     */
    public static String getCurrentEmail() {
        UserContext userContext = getCurrentUser();
        return userContext != null ? userContext.getEmail() : null;
    }

    /**
     * 获取当前用户手机号
     *
     * @return 用户手机号，如果没有当前用户则返回null
     */
    public static String getCurrentMobile() {
        UserContext userContext = getCurrentUser();
        return userContext != null ? userContext.getMobile() : null;
    }

    /**
     * 获取当前用户头像URL
     *
     * @return 用户头像URL，如果没有当前用户则返回null
     */
    public static String getCurrentAvatar() {
        UserContext userContext = getCurrentUser();
        return userContext != null ? userContext.getAvatar() : null;
    }

    /**
     * 获取当前用户系统菜单树列表
     *
     * <p><b>注意：</b>systemMenuTree 不再存储在 UserContext 中，而是单独存储在缓存中。
     * 如需获取系统菜单树，请使用 {@link com.indigo.security.core.AuthenticationService#getUserSystemMenuTree(String, Class)}
     * 或从 UserSessionService 获取。
     *
     * @return 空列表（systemMenuTree 已从 UserContext 中移除，请从缓存中获取）
     */
    public static <T> List<T> getCurrentSystemMenuTree() {
        // systemMenuTree 已从 UserContext 中移除，单独存储在缓存中
        return List.of();
    }

    /**
     * 获取当前用户权限列表
     *
     * <p><b>注意：</b>permissions 不再存储在 UserContext 中，而是单独存储在缓存中。
     * 如需获取权限列表，请使用 {@link com.indigo.cache.session.UserSessionService#getUserPermissions(String)}
     * 或从 UserSessionService 获取。
     *
     * @return 空列表（permissions 已从 UserContext 中移除，请从缓存中获取）
     */
    public static List<String> getCurrentPermissions() {
        // permissions 已从 UserContext 中移除，单独存储在缓存中
        return List.of();
    }

    /**
     * 检查当前用户是否有指定系统
     *
     * @param systemId 系统ID
     * @return 如果有该系统返回true，否则返回false
     */
    public static boolean hasSystem(String systemId) {
        if (systemId == null || systemId.isEmpty()) {
            return false;
        }
        List<?> systemMenuTree = getCurrentSystemMenuTree();
        if (systemMenuTree == null || systemMenuTree.isEmpty()) {
            return false;
        }
        // 检查系统菜单树中是否包含该系统
        return systemMenuTree.stream()
                .anyMatch(item -> {
                    try {
                        Object id = item.getClass().getMethod("getId").invoke(item);
                        return systemId.equals(id);
                    } catch (Exception e) {
                        return false;
                    }
                });
    }

    /**
     * 检查当前用户是否有指定权限
     *
     * @param permission 权限标识
     * @return 如果有该权限返回true，否则返回false
     */
    public static boolean hasPermission(String permission) {
        if (permission == null || permission.isEmpty()) {
            return false;
        }
        List<String> permissions = getCurrentPermissions();
        return permissions.contains(permission);
    }

    /**
     * 检查当前用户是否有任一指定系统
     *
     * @param systemIds 系统ID数组
     * @return 如果有任一系统返回true，否则返回false
     */
    public static boolean hasAnySystem(String... systemIds) {
        if (systemIds == null || systemIds.length == 0) {
            return false;
        }
        return java.util.Arrays.stream(systemIds).anyMatch(UserContext::hasSystem);
    }

    /**
     * 检查当前用户是否有任一指定权限
     *
     * @param permissions 权限标识数组
     * @return 如果有任一权限返回true，否则返回false
     */
    public static boolean hasAnyPermission(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        List<String> userPermissions = getCurrentPermissions();
        return java.util.Arrays.stream(permissions).anyMatch(userPermissions::contains);
    }

    /**
     * 检查当前用户是否有所有指定系统
     *
     * @param systemIds 系统ID数组
     * @return 如果有所有系统返回true，否则返回false
     */
    public static boolean hasAllSystems(String... systemIds) {
        if (systemIds == null || systemIds.length == 0) {
            return false;
        }
        return java.util.Arrays.stream(systemIds).allMatch(UserContext::hasSystem);
    }

    /**
     * 检查当前用户是否有所有指定权限
     *
     * @param permissions 权限标识数组
     * @return 如果有所有权限返回true，否则返回false
     */
    public static boolean hasAllPermissions(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        List<String> userPermissions = getCurrentPermissions();
        return java.util.Arrays.stream(permissions).allMatch(userPermissions::contains);
    }

}