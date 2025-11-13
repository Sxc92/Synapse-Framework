package com.indigo.cache.core.constants;

/**
 * 会话缓存常量类
 * 统一管理会话相关的缓存名称和缓存类型常量
 * 
 * @author 史偕成
 * @date 2025/11/13
 */
public final class SessionCacheConstants {

    private SessionCacheConstants() {
        // 工具类，禁止实例化
    }

    // ===========================================
    // Caffeine 缓存名称常量
    // ===========================================

    /**
     * 用户会话缓存名称
     * 用于 Caffeine 本地缓存中的用户会话数据
     */
    public static final String CACHE_NAME_USER_SESSION = "userSession";

    /**
     * 用户 Token 缓存名称
     * 用于 Caffeine 本地缓存中的用户 Token 数据
     */
    public static final String CACHE_NAME_USER_TOKEN = "userToken";

    /**
     * 用户权限缓存名称
     * 用于 Caffeine 本地缓存中的用户权限数据
     */
    public static final String CACHE_NAME_USER_PERMISSIONS = "userPermissions";

    /**
     * 用户角色缓存名称
     * 用于 Caffeine 本地缓存中的用户角色数据
     */
    public static final String CACHE_NAME_USER_ROLES = "userRoles";

    /**
     * 用户菜单缓存名称
     * 用于 Caffeine 本地缓存中的用户菜单数据
     */
    public static final String CACHE_NAME_USER_MENUS = "userMenus";

    /**
     * 用户资源缓存名称
     * 用于 Caffeine 本地缓存中的用户资源数据
     */
    public static final String CACHE_NAME_USER_RESOURCES = "userResources";

    /**
     * 用户系统缓存名称
     * 用于 Caffeine 本地缓存中的用户系统数据
     */
    public static final String CACHE_NAME_USER_SYSTEMS = "userSystems";

    // ===========================================
    // 缓存类型常量（用于失效通知）
    // ===========================================

    /**
     * 用户会话缓存类型
     * 用于缓存失效通知中的缓存类型标识
     */
    public static final String CACHE_TYPE_USER_SESSION = "userSession";

    /**
     * 用户 Token 缓存类型
     * 用于缓存失效通知中的缓存类型标识
     */
    public static final String CACHE_TYPE_USER_TOKEN = "userToken";

    /**
     * 用户权限缓存类型
     * 用于缓存失效通知中的缓存类型标识
     */
    public static final String CACHE_TYPE_USER_PERMISSIONS = "userPermissions";

    /**
     * 用户角色缓存类型
     * 用于缓存失效通知中的缓存类型标识
     */
    public static final String CACHE_TYPE_USER_ROLES = "userRoles";

    /**
     * 用户菜单缓存类型
     * 用于缓存失效通知中的缓存类型标识
     */
    public static final String CACHE_TYPE_USER_MENUS = "userMenus";

    /**
     * 用户资源缓存类型
     * 用于缓存失效通知中的缓存类型标识
     */
    public static final String CACHE_TYPE_USER_RESOURCES = "userResources";

    /**
     * 用户系统缓存类型
     * 用于缓存失效通知中的缓存类型标识
     */
    public static final String CACHE_TYPE_USER_SYSTEMS = "userSystems";
}

