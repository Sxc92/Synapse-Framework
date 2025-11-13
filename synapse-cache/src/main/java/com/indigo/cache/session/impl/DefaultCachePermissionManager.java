package com.indigo.cache.session.impl;

import com.indigo.cache.core.CacheInvalidationService;
import com.indigo.cache.core.CacheInvalidationTracker;
import com.indigo.cache.core.CacheService;
import com.indigo.cache.infrastructure.CaffeineCacheManager;
import com.indigo.cache.manager.CacheKeyGenerator;
import com.indigo.cache.session.CachePermissionManager;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

/**
 * 默认缓存权限管理器实现
 * 基于缓存服务的权限管理
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Slf4j
public class DefaultCachePermissionManager implements CachePermissionManager {

    /**
     * Caffeine 缓存名称
     */
    private static final String CACHE_NAME_USER_PERMISSIONS = "userPermissions";
    private static final String CACHE_NAME_USER_ROLES = "userRoles";
    private static final String CACHE_NAME_USER_MENUS = "userMenus";
    private static final String CACHE_NAME_USER_RESOURCES = "userResources";
    private static final String CACHE_NAME_USER_SYSTEMS = "userSystems";
    
    /**
     * 缓存类型（用于失效通知）
     */
    private static final String CACHE_TYPE_USER_PERMISSIONS = "userPermissions";
    private static final String CACHE_TYPE_USER_ROLES = "userRoles";

    /**
     * 本地缓存默认过期时间（秒）
     * 设置为 Redis 过期时间的 1/10，确保本地缓存先过期，避免数据不一致
     */
    private static final int LOCAL_CACHE_EXPIRE_SECONDS = 300; // 5分钟

    private final CacheService cacheService;
    private final CacheKeyGenerator keyGenerator;
    private final CaffeineCacheManager caffeineCacheManager;
    private final CacheInvalidationService cacheInvalidationService;
    private final CacheInvalidationTracker invalidationTracker;

    /**
     * 构造函数（兼容旧版本，CaffeineCacheManager 和 CacheInvalidationService 为可选）
     */
    public DefaultCachePermissionManager(CacheService cacheService, CacheKeyGenerator keyGenerator) {
        this.cacheService = cacheService;
        this.keyGenerator = keyGenerator;
        this.caffeineCacheManager = null;
        this.cacheInvalidationService = null;
        this.invalidationTracker = null;
    }

    /**
     * 构造函数（支持 Caffeine 缓存）
     */
    public DefaultCachePermissionManager(CacheService cacheService, 
                                        CacheKeyGenerator keyGenerator,
                                        CaffeineCacheManager caffeineCacheManager) {
        this(cacheService, keyGenerator, caffeineCacheManager, null, null);
    }

    /**
     * 构造函数（支持 Caffeine 缓存和失效通知）
     */
    public DefaultCachePermissionManager(CacheService cacheService, 
                                        CacheKeyGenerator keyGenerator,
                                        CaffeineCacheManager caffeineCacheManager,
                                        CacheInvalidationService cacheInvalidationService) {
        this(cacheService, keyGenerator, caffeineCacheManager, cacheInvalidationService, null);
    }

    /**
     * 构造函数（支持 Caffeine 缓存、失效通知和失效追踪）
     */
    public DefaultCachePermissionManager(CacheService cacheService, 
                                        CacheKeyGenerator keyGenerator,
                                        CaffeineCacheManager caffeineCacheManager,
                                        CacheInvalidationService cacheInvalidationService,
                                        CacheInvalidationTracker invalidationTracker) {
        this.cacheService = cacheService;
        this.keyGenerator = keyGenerator;
        this.caffeineCacheManager = caffeineCacheManager;
        this.cacheInvalidationService = cacheInvalidationService;
        this.invalidationTracker = invalidationTracker;
    }

    @Override
    public void storeUserPermissions(String token, List<String> permissions, long expiration) {
        String permissionsKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "permissions", token);
        
        // 1. 写入 Redis（主存储）
        cacheService.setObject(permissionsKey, permissions, expiration);
        
        // 2. 写入 Caffeine 本地缓存（如果可用）
        if (caffeineCacheManager != null) {
            try {
                int localExpireSeconds = calculateLocalCacheExpire(expiration);
                caffeineCacheManager.put(CACHE_NAME_USER_PERMISSIONS, token, permissions, localExpireSeconds);
                log.debug("Stored user permissions to local cache: token={}, expireSeconds={}", token, localExpireSeconds);
            } catch (Exception e) {
                log.warn("Failed to store user permissions to local cache: token={}", token, e);
            }
        }
        
        // 3. 发布缓存失效通知（通知其他节点清除本地缓存）
        if (cacheInvalidationService != null) {
            cacheInvalidationService.publishInvalidation(CACHE_TYPE_USER_PERMISSIONS, token);
        }
        
        log.info("Stored user permissions for token: {}", token);
    }

    @Override
    public void storeUserRoles(String token, List<String> roles, long expiration) {
        String rolesKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "roles", token);
        
        // 1. 写入 Redis（主存储）
        cacheService.setObject(rolesKey, roles, expiration);
        
        // 2. 写入 Caffeine 本地缓存（如果可用）
        if (caffeineCacheManager != null) {
            try {
                int localExpireSeconds = calculateLocalCacheExpire(expiration);
                caffeineCacheManager.put(CACHE_NAME_USER_ROLES, token, roles, localExpireSeconds);
                log.debug("Stored user roles to local cache: token={}, expireSeconds={}", token, localExpireSeconds);
            } catch (Exception e) {
                log.warn("Failed to store user roles to local cache: token={}", token, e);
            }
        }
        
        // 3. 发布缓存失效通知（通知其他节点清除本地缓存）
        if (cacheInvalidationService != null) {
            cacheInvalidationService.publishInvalidation(CACHE_TYPE_USER_ROLES, token);
        }
        
        log.info("Stored user roles for token: {}", token);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getUserPermissions(String token) {
        // 1. 优先从 Caffeine 本地缓存读取
        if (caffeineCacheManager != null) {
            try {
                Optional<List<String>> cachedPermissions = caffeineCacheManager.get(CACHE_NAME_USER_PERMISSIONS, token);
                if (cachedPermissions.isPresent()) {
                    log.debug("Retrieved user permissions from local cache: token={}", token);
                    return cachedPermissions.get();
                }
            } catch (Exception e) {
                log.warn("Failed to get user permissions from local cache: token={}", token, e);
            }
        }

        // 2. 从 Redis 读取
        String permissionsKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "permissions", token);
        List<String> permissions = cacheService.getObject(permissionsKey, List.class);
        
        // 3. 写入 Caffeine 本地缓存（如果可用且数据存在）
        if (permissions != null && caffeineCacheManager != null) {
            try {
                // 记录数据读取时间戳（用于并发控制）
                long dataTimestamp = System.currentTimeMillis();
                
                // 检查是否在失效之后（防止写入旧数据）
                if (invalidationTracker != null && 
                    invalidationTracker.isInvalidated(CACHE_TYPE_USER_PERMISSIONS, token, dataTimestamp)) {
                    log.debug("跳过写入本地缓存（数据已失效）: token={}", token);
                    return permissions;
                }
                
                // 获取 Redis 中的剩余过期时间
                long remainingTime = cacheService.getTimeToLive(permissionsKey);
                int localExpireSeconds = remainingTime > 0 
                    ? calculateLocalCacheExpire(remainingTime) 
                    : LOCAL_CACHE_EXPIRE_SECONDS;
                caffeineCacheManager.put(CACHE_NAME_USER_PERMISSIONS, token, permissions, localExpireSeconds);
                
                // 清除失效记录（数据已成功更新）
                if (invalidationTracker != null) {
                    invalidationTracker.clearInvalidation(CACHE_TYPE_USER_PERMISSIONS, token);
                }
                
                log.debug("Stored user permissions to local cache after Redis read: token={}, expireSeconds={}", 
                        token, localExpireSeconds);
            } catch (Exception e) {
                log.warn("Failed to store user permissions to local cache after Redis read: token={}", token, e);
            }
        }
        
        return permissions;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getUserRoles(String token) {
        // 1. 优先从 Caffeine 本地缓存读取
        if (caffeineCacheManager != null) {
            try {
                Optional<List<String>> cachedRoles = caffeineCacheManager.get(CACHE_NAME_USER_ROLES, token);
                if (cachedRoles.isPresent()) {
                    log.debug("Retrieved user roles from local cache: token={}", token);
                    return cachedRoles.get();
                }
            } catch (Exception e) {
                log.warn("Failed to get user roles from local cache: token={}", token, e);
            }
        }

        // 2. 从 Redis 读取
        String rolesKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "roles", token);
        List<String> roles = cacheService.getObject(rolesKey, List.class);
        
        // 3. 写入 Caffeine 本地缓存（如果可用且数据存在）
        if (roles != null && caffeineCacheManager != null) {
            try {
                // 记录数据读取时间戳（用于并发控制）
                long dataTimestamp = System.currentTimeMillis();
                
                // 检查是否在失效之后（防止写入旧数据）
                if (invalidationTracker != null && 
                    invalidationTracker.isInvalidated(CACHE_TYPE_USER_ROLES, token, dataTimestamp)) {
                    log.debug("跳过写入本地缓存（数据已失效）: token={}", token);
                    return roles;
                }
                
                // 获取 Redis 中的剩余过期时间
                long remainingTime = cacheService.getTimeToLive(rolesKey);
                int localExpireSeconds = remainingTime > 0 
                    ? calculateLocalCacheExpire(remainingTime) 
                    : LOCAL_CACHE_EXPIRE_SECONDS;
                caffeineCacheManager.put(CACHE_NAME_USER_ROLES, token, roles, localExpireSeconds);
                
                // 清除失效记录（数据已成功更新）
                if (invalidationTracker != null) {
                    invalidationTracker.clearInvalidation(CACHE_TYPE_USER_ROLES, token);
                }
                
                log.debug("Stored user roles to local cache after Redis read: token={}, expireSeconds={}", 
                        token, localExpireSeconds);
            } catch (Exception e) {
                log.warn("Failed to store user roles to local cache after Redis read: token={}", token, e);
            }
        }
        
        return roles;
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
        
        // 1. 删除 Redis 中的权限和角色
        cacheService.delete(permissionsKey);
        cacheService.delete(rolesKey);
        
        // 2. 删除 Caffeine 本地缓存（如果可用）
        if (caffeineCacheManager != null) {
            try {
                caffeineCacheManager.remove(CACHE_NAME_USER_PERMISSIONS, token);
                caffeineCacheManager.remove(CACHE_NAME_USER_ROLES, token);
                log.debug("Removed user permissions and roles from local cache: token={}", token);
            } catch (Exception e) {
                log.warn("Failed to remove user permissions and roles from local cache: token={}", token, e);
            }
        }
        
        // 3. 发布缓存失效通知（通知其他节点清除本地缓存）
        if (cacheInvalidationService != null) {
            cacheInvalidationService.publishInvalidation(CACHE_TYPE_USER_PERMISSIONS, token);
            cacheInvalidationService.publishInvalidation(CACHE_TYPE_USER_ROLES, token);
        }
        
        log.info("Removed user permissions and roles for token: {}", token);
    }

    @Override
    public void extendUserPermissions(String token, long expiration) {
        String permissionsKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "permissions", token);
        String rolesKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "roles", token);
        
        // 1. 延长 Redis 中的过期时间
        cacheService.resetExpiry(permissionsKey, expiration);
        cacheService.resetExpiry(rolesKey, expiration);
        
        // 2. 更新 Caffeine 本地缓存（如果可用）
        if (caffeineCacheManager != null) {
            try {
                int localExpireSeconds = calculateLocalCacheExpire(expiration);
                
                // 更新权限缓存
                Optional<List<String>> cachedPermissions = caffeineCacheManager.get(CACHE_NAME_USER_PERMISSIONS, token);
                if (cachedPermissions.isPresent()) {
                    caffeineCacheManager.put(CACHE_NAME_USER_PERMISSIONS, token, cachedPermissions.get(), localExpireSeconds);
                }
                
                // 更新角色缓存
                Optional<List<String>> cachedRoles = caffeineCacheManager.get(CACHE_NAME_USER_ROLES, token);
                if (cachedRoles.isPresent()) {
                    caffeineCacheManager.put(CACHE_NAME_USER_ROLES, token, cachedRoles.get(), localExpireSeconds);
                }
                
                log.debug("Extended user permissions and roles in local cache: token={}, expireSeconds={}", 
                        token, localExpireSeconds);
            } catch (Exception e) {
                log.warn("Failed to extend user permissions and roles in local cache: token={}", token, e);
            }
        }
        
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

    /**
     * 计算本地缓存过期时间
     * 本地缓存过期时间应该小于 Redis 过期时间，确保本地缓存先过期
     * 
     * @param redisExpiration Redis 过期时间（秒）
     * @return 本地缓存过期时间（秒）
     */
    private int calculateLocalCacheExpire(long redisExpiration) {
        // 本地缓存过期时间 = min(Redis过期时间的1/10, 5分钟)
        long localExpire = Math.min(redisExpiration / 10, LOCAL_CACHE_EXPIRE_SECONDS);
        // 确保至少 1 分钟
        return (int) Math.max(localExpire, 60);
    }

    @Override
    public void removeUserMenusAndResources(String token) {
        String menusKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "menus", token);
        String resourcesKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "resources", token);
        String systemsKey = keyGenerator.generate(CacheKeyGenerator.Module.USER, "systems", token);
        
        // 1. 删除 Redis 中的数据
        cacheService.delete(menusKey);
        cacheService.delete(resourcesKey);
        cacheService.delete(systemsKey);
        
        // 2. 删除 Caffeine 本地缓存（如果可用）
        if (caffeineCacheManager != null) {
            try {
                caffeineCacheManager.remove(CACHE_NAME_USER_MENUS, token);
                caffeineCacheManager.remove(CACHE_NAME_USER_RESOURCES, token);
                caffeineCacheManager.remove(CACHE_NAME_USER_SYSTEMS, token);
                log.debug("Removed user menus, resources and systems from local cache: token={}", token);
            } catch (Exception e) {
                log.warn("Failed to remove user menus, resources and systems from local cache: token={}", token, e);
            }
        }
        
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