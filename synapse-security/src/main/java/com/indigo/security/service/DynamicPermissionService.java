package com.indigo.security.service;

import com.indigo.cache.core.CacheService;
import com.indigo.cache.session.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 动态权限管理服务
 * 支持运行时权限变更、权限模板、权限继承等功能
 *
 * @author 史偕成
 * @date 2025/01/08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicPermissionService {

    private final UserSessionService userSessionService;
    private final CacheService cacheService;

    // 缓存键前缀
    private static final String PERMISSION_TEMPLATE_PREFIX = "permission_template:";
    private static final String USER_PERMISSION_PREFIX = "user_permission:";
    private static final String ROLE_PERMISSION_PREFIX = "role_permission:";

    /**
     * 运行时更新用户权限
     */
    public boolean updateUserPermission(String userId, String permission, boolean granted) {
        try {
            if (granted) {
                addUserPermission(userId, permission);
            } else {
                removeUserPermission(userId, permission);
            }
            
            // TODO: 操作审计 - 记录权限变更事件
            // auditService.logUserAction(userId, "PERMISSION_UPDATED", "dynamic_permission", 
            //     String.format("权限%s: %s", granted ? "授予" : "撤销", permission));
            
            log.info("用户权限更新成功: userId={}, permission={}, granted={}", userId, permission, granted);
            return true;
            
        } catch (Exception e) {
            log.error("用户权限更新失败: userId={}, permission={}, granted={}", userId, permission, granted, e);
            return false;
        }
    }

    /**
     * 应用权限模板
     */
    public boolean applyPermissionTemplate(String userId, String templateName) {
        try {
            PermissionTemplate template = getPermissionTemplate(templateName);
            if (template == null) {
                return false;
            }
            
            for (String permission : template.getPermissions()) {
                updateUserPermission(userId, permission, true);
            }
            
            log.info("权限模板应用成功: userId={}, template={}", userId, templateName);
            return true;
            
        } catch (Exception e) {
            log.error("权限模板应用失败: userId={}, template={}", userId, templateName, e);
            return false;
        }
    }

    /**
     * 创建权限模板
     */
    public boolean createPermissionTemplate(String templateName, List<String> permissions, 
                                         List<String> roles, String description) {
        try {
            PermissionTemplate template = PermissionTemplate.builder()
                .name(templateName)
                .permissions(permissions)
                .roles(roles)
                .description(description)
                .createdAt(System.currentTimeMillis())
                .build();
            
            String cacheKey = PERMISSION_TEMPLATE_PREFIX + templateName;
            cacheService.setObject(cacheKey, template, 86400);
            
            return true;
            
        } catch (Exception e) {
            log.error("权限模板创建失败: {}", templateName, e);
            return false;
        }
    }

    /**
     * 获取权限模板
     */
    public PermissionTemplate getPermissionTemplate(String templateName) {
        String cacheKey = PERMISSION_TEMPLATE_PREFIX + templateName;
        return cacheService.getObject(cacheKey, PermissionTemplate.class);
    }

    private void addUserPermission(String userId, String permission) {
        List<String> currentPermissions = userSessionService.getUserPermissions(userId);
        if (currentPermissions == null) {
            currentPermissions = new java.util.ArrayList<>();
        }
        
        if (!currentPermissions.contains(permission)) {
            currentPermissions.add(permission);
            userSessionService.storeUserPermissions(userId, currentPermissions, 7200L);
        }
    }

    private void removeUserPermission(String userId, String permission) {
        List<String> currentPermissions = userSessionService.getUserPermissions(userId);
        if (currentPermissions != null) {
            currentPermissions.remove(permission);
            userSessionService.storeUserPermissions(userId, currentPermissions, 7200L);
        }
    }

    /**
     * 权限模板
     */
    @lombok.Data
    @lombok.Builder
    public static class PermissionTemplate {
        private String name;
        private List<String> permissions;
        private List<String> roles;
        private String description;
        private Long createdAt;
    }
}
