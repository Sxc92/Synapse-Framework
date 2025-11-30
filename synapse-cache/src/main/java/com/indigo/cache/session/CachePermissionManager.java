package com.indigo.cache.session;

import java.util.List;

/**
 * 缓存权限管理器接口
 * 定义用户权限和角色管理的核心操作
 *
 * @author 史偕成
 * @date 2025/12/19
 */
public interface CachePermissionManager {

    /**
     * 存储用户权限信息
     *
     * @param token       访问令牌
     * @param permissions 权限列表
     * @param expiration  过期时间（秒）
     */
    void storeUserPermissions(String token, List<String> permissions, long expiration);

    /**
     * 存储用户角色信息
     *
     * @param token      访问令牌
     * @param roles      角色列表
     * @param expiration 过期时间（秒）
     */
    void storeUserRoles(String token, List<String> roles, long expiration);

    /**
     * 获取用户权限列表
     *
     * @param token 访问令牌
     * @return 权限列表
     */
    List<String> getUserPermissions(String token);

    /**
     * 获取用户角色列表
     *
     * @param token 访问令牌
     * @return 角色列表
     */
    List<String> getUserRoles(String token);

    /**
     * 检查用户是否有指定权限
     *
     * @param token      访问令牌
     * @param permission 权限标识
     * @return 是否有权限
     */
    boolean hasPermission(String token, String permission);

    /**
     * 检查用户是否有指定角色
     *
     * @param token 访问令牌
     * @param role  角色标识
     * @return 是否有角色
     */
    boolean hasRole(String token, String role);

    /**
     * 删除用户权限和角色信息
     *
     * @param token 访问令牌
     */
    void removeUserPermissions(String token);

    /**
     * 延长用户权限和角色过期时间
     *
     * @param token      访问令牌
     * @param expiration 新的过期时间（秒）
     */
    void extendUserPermissions(String token, long expiration);

    // ========== 菜单、资源、系统管理相关方法 ==========

    /**
     * 存储用户菜单列表
     *
     * @param token      访问令牌
     * @param menus      菜单列表（泛型，支持任意类型）
     * @param expiration 过期时间（秒）
     * @param <T>        菜单类型
     */
    <T> void storeUserMenus(String token, List<T> menus, long expiration);

    /**
     * 存储用户资源列表
     *
     * @param token      访问令牌
     * @param resources  资源列表（泛型，支持任意类型）
     * @param expiration 过期时间（秒）
     * @param <T>        资源类型
     */
    <T> void storeUserResources(String token, List<T> resources, long expiration);

    /**
     * 存储用户系统列表
     *
     * @param token      访问令牌
     * @param systems    系统列表（泛型，支持任意类型）
     * @param expiration 过期时间（秒）
     * @param <T>        系统类型
     */
    <T> void storeUserSystems(String token, List<T> systems, long expiration);

    /**
     * 获取用户菜单列表
     *
     * @param token 访问令牌
     * @param clazz 菜单类型
     * @param <T>   菜单类型
     * @return 菜单列表
     */
    <T> List<T> getUserMenus(String token, Class<T> clazz);

    /**
     * 获取用户资源列表
     *
     * @param token 访问令牌
     * @param clazz 资源类型
     * @param <T>   资源类型
     * @return 资源列表
     */
    <T> List<T> getUserResources(String token, Class<T> clazz);

    /**
     * 获取用户系统列表
     *
     * @param token 访问令牌
     * @param clazz 系统类型
     * @param <T>   系统类型
     * @return 系统列表
     */
    <T> List<T> getUserSystems(String token, Class<T> clazz);

    /**
     * 存储用户系统菜单树列表
     *
     * @param token      访问令牌
     * @param systemMenuTree 系统菜单树列表（泛型，支持任意类型）
     * @param expiration 过期时间（秒）
     * @param <T>        系统菜单树类型
     */
    <T> void storeUserSystemMenuTree(String token, List<T> systemMenuTree, long expiration);

    /**
     * 获取用户系统菜单树列表
     *
     * @param token 访问令牌
     * @param clazz 系统菜单树类型
     * @param <T>   系统菜单树类型
     * @return 系统菜单树列表
     */
    <T> List<T> getUserSystemMenuTree(String token, Class<T> clazz);

    /**
     * 删除用户菜单、资源、系统信息
     *
     * @param token 访问令牌
     */
    void removeUserMenusAndResources(String token);

    /**
     * 延长用户菜单、资源、系统过期时间
     *
     * @param token      访问令牌
     * @param expiration 新的过期时间（秒）
     */
    void extendUserMenusAndResources(String token, long expiration);
} 