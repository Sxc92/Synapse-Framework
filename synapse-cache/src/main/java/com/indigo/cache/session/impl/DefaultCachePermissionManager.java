package com.indigo.cache.session.impl;

import com.indigo.cache.core.CacheService;
import com.indigo.cache.manager.CacheKeyGenerator;
import com.indigo.cache.session.CachePermissionManager;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 默认缓存权限管理器实现
 * 基于缓存服务的权限管理
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Slf4j
public class DefaultCachePermissionManager implements CachePermissionManager {

    private final CacheService cacheService;
    private final CacheKeyGenerator keyGenerator;

    public DefaultCachePermissionManager(CacheService cacheService, CacheKeyGenerator keyGenerator) {
        this.cacheService = cacheService;
        this.keyGenerator = keyGenerator;
    }

    @Override
    public void storeUserPermissions(String token, List<String> permissions, long expiration) {
        String permissionsKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "permissions", token);
        cacheService.setObject(permissionsKey, permissions, expiration);
        log.info("Stored user permissions for token: {}", token);
    }

    @Override
    public void storeUserRoles(String token, List<String> roles, long expiration) {
        String rolesKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "roles", token);
        cacheService.setObject(rolesKey, roles, expiration);
        log.info("Stored user roles for token: {}", token);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getUserPermissions(String token) {
        String permissionsKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "permissions", token);
        return cacheService.getObject(permissionsKey, List.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getUserRoles(String token) {
        String rolesKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "roles", token);
        return cacheService.getObject(rolesKey, List.class);
    }

    @Override
    public boolean hasPermission(String token, String permission) {
        List<String> permissions = getUserPermissions(token);
        return permissions != null && permissions.contains(permission);
    }

    @Override
    public boolean hasRole(String token, String role) {
        List<String> roles = getUserRoles(token);
        return roles != null && roles.contains(role);
    }

    @Override
    public void removeUserPermissions(String token) {
        String permissionsKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "permissions", token);
        String rolesKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "roles", token);
        cacheService.delete(permissionsKey);
        cacheService.delete(rolesKey);
        log.info("Removed user permissions and roles for token: {}", token);
    }

    @Override
    public void extendUserPermissions(String token, long expiration) {
        String permissionsKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "permissions", token);
        String rolesKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "roles", token);
        cacheService.resetExpiry(permissionsKey, expiration);
        cacheService.resetExpiry(rolesKey, expiration);
        log.info("Extended user permissions and roles for token: {}", token);
    }

    // ========== 菜单、资源、系统管理相关方法实现 ==========

    @Override
    public <T> void storeUserMenus(String token, List<T> menus, long expiration) {
        String menusKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "menus", token);
        cacheService.setObject(menusKey, menus, expiration);
        log.debug("Stored user menus for token: {}, count: {}", token, menus != null ? menus.size() : 0);
    }

    @Override
    public <T> void storeUserResources(String token, List<T> resources, long expiration) {
        String resourcesKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "resources", token);
        cacheService.setObject(resourcesKey, resources, expiration);
        log.debug("Stored user resources for token: {}, count: {}", token, resources != null ? resources.size() : 0);
    }

    @Override
    public <T> void storeUserSystems(String token, List<T> systems, long expiration) {
        String systemsKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "systems", token);
        cacheService.setObject(systemsKey, systems, expiration);
        log.debug("Stored user systems for token: {}, count: {}", token, systems != null ? systems.size() : 0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getUserMenus(String token, Class<T> clazz) {
        String menusKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "menus", token);
        return cacheService.getObject(menusKey, List.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getUserResources(String token, Class<T> clazz) {
        String resourcesKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "resources", token);
        return cacheService.getObject(resourcesKey, List.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getUserSystems(String token, Class<T> clazz) {
        String systemsKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "systems", token);
        return cacheService.getObject(systemsKey, List.class);
    }

    @Override
    public void removeUserMenusAndResources(String token) {
        String menusKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "menus", token);
        String resourcesKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "resources", token);
        String systemsKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "systems", token);
        cacheService.delete(menusKey);
        cacheService.delete(resourcesKey);
        cacheService.delete(systemsKey);
        log.info("Removed user menus, resources and systems for token: {}", token);
    }

    @Override
    public void extendUserMenusAndResources(String token, long expiration) {
        String menusKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "menus", token);
        String resourcesKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "resources", token);
        String systemsKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "systems", token);
        cacheService.resetExpiry(menusKey, expiration);
        cacheService.resetExpiry(resourcesKey, expiration);
        cacheService.resetExpiry(systemsKey, expiration);
        log.debug("Extended user menus, resources and systems for token: {}", token);
    }
} 