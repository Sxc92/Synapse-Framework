package com.indigo.security.core;

import com.indigo.cache.session.UserSessionService;
import com.indigo.core.context.UserContext;
import com.indigo.core.exception.Ex;
import com.indigo.security.annotation.Logical;
import com.indigo.security.config.SecurityAutoConfiguration;
import com.indigo.security.constants.SecurityConstants;
import com.indigo.security.constants.SecurityError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;

/**
 * 权限检查服务
 * 提供权限和角色的检查方法，替代 Sa-Token 的权限检查功能
 * 
 * <p><b>设计说明：</b>
 * <ul>
 *   <li>从 UserContext 获取用户信息（ThreadLocal）</li>
 *   <li>从 UserSessionService 获取权限信息（通过 token）</li>
 *   <li>支持 AND/OR 逻辑判断</li>
 *   <li>检查失败时抛出相应异常</li>
 * </ul>
 * 
 * <p><b>历史说明：</b>
 * 之前使用 PermissionManager 实现 Sa-Token 的 StpInterface，现已移除 Sa-Token 依赖
 * PermissionService 用于业务代码直接调用，提供更简洁的 API
 * 
 * <p><b>注意：</b>此服务现在需要注入 UserSessionService 来获取权限信息。
 * 通过 {@link SecurityAutoConfiguration} 中的 @Bean 方法创建，避免条件加载问题。
 * 
 * @author 史偕成
 * @date 2025/01/XX
 */
@Slf4j
public class PermissionService {

    private final UserSessionService userSessionService;

    /**
     * 构造函数
     * 
     * @param userSessionService 用户会话服务（可选，如果为 null 则无法获取权限）
     */
    public PermissionService(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

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
     * 检查用户是否有指定系统
     * 注意：角色信息已改为系统菜单树结构，此方法用于检查用户是否有指定系统权限
     * 
     * @param systemIds 需要的系统ID列表
     * @param logical   逻辑运算符（AND/OR）
     * @throws com.indigo.core.exception.SynapseException 如果用户未登录或没有所需系统权限
     */
    public void checkSystem(String[] systemIds, Logical logical) {
        if (systemIds == null || systemIds.length == 0) {
            Ex.throwEx(SecurityError.PERMISSION_DENIED, "系统ID列表不能为空");
        }

        UserContext userContext = UserContext.getCurrentUser();
        if (userContext == null) {
            Ex.throwEx(SecurityError.NOT_LOGIN);
        }

        boolean hasSystem;
        if (logical == Logical.AND) {
            // AND 逻辑：需要所有系统
            hasSystem = UserContext.hasAllSystems(systemIds);
        } else {
            // OR 逻辑：需要任一系统
            hasSystem = UserContext.hasAnySystem(systemIds);
        }

        if (!hasSystem) {
            log.warn("用户系统权限不足: userId={}, required={}", 
                    userContext.getUserId(), List.of(systemIds));
            Ex.throwEx(SecurityError.PERMISSION_DENIED, "用户没有所需系统权限");
        }

        log.debug("系统权限检查通过: userId={}, required={}", 
                userContext.getUserId(), List.of(systemIds));
    }

    /**
     * 检查用户是否有指定角色
     * 
     * @deprecated 角色信息已改为系统菜单树结构，请使用 {@link #checkSystem(String[], Logical)} 方法
     * @param roles  需要的角色列表
     * @param logical 逻辑运算符（AND/OR）
     * @throws com.indigo.core.exception.SynapseException 如果用户未登录或没有所需角色
     */
    @Deprecated
    public void checkRole(String[] roles, Logical logical) {
        log.warn("checkRole 方法已废弃，请使用 checkSystem 方法: roles={}", List.of(roles));
        // 为了向后兼容，暂时抛出异常提示使用新方法
        Ex.throwEx(SecurityError.PERMISSION_DENIED, "请使用 checkSystem 方法替代 checkRole 方法");
    }

    /**
     * 检查用户是否有指定权限
     * 
     * <p><b>注意：</b>权限信息不再存储在 UserContext 中，而是从 UserSessionService 获取。
     * 需要从请求属性中获取 token，然后通过 token 从 UserSessionService 获取权限列表。
     * 
     * @param permissions 需要的权限列表
     * @param logical     逻辑运算符（AND/OR）
     * @throws com.indigo.core.exception.SynapseException 如果用户未登录或没有所需权限
     */
    public void checkPermission(String[] permissions, Logical logical) {
        validatePermissionParams(permissions);

        UserContext userContext = getCurrentUserContext();
        
        // 从 UserSessionService 获取权限（通过 token）
        List<String> userPermissions = getUserPermissions();
        
        if (userPermissions == null || userPermissions.isEmpty()) {
            log.warn("用户没有权限: userId={}", userContext.getUserId());
            Ex.throwEx(SecurityError.PERMISSION_DENIED, "用户没有所需权限");
        }

        boolean hasPermission = checkPermissionLogic(userPermissions, permissions, logical);
        if (!hasPermission) {
            log.warn("用户权限不足: userId={}, required={}, userPermissions={}", 
                    userContext.getUserId(), List.of(permissions), userPermissions);
            Ex.throwEx(SecurityError.PERMISSION_DENIED, "用户没有所需权限");
        }

        log.debug("权限检查通过: userId={}, required={}, userPermissions={}", 
                userContext.getUserId(), List.of(permissions), userPermissions);
    }

    /**
     * 从 UserSessionService 获取用户权限列表
     * 
     * @return 用户权限列表，如果无法获取则返回空列表
     */
    private List<String> getUserPermissions() {
        // 1. 检查 UserSessionService 是否可用
        if (userSessionService == null) {
            log.warn("UserSessionService 未注入，无法获取权限信息");
            return List.of();
        }

        // 2. 从请求属性中获取 token
        String token = getTokenFromRequest();
        if (token == null || token.isEmpty()) {
            log.warn("无法从请求中获取 token，无法获取权限信息");
            return List.of();
        }

        // 3. 从 UserSessionService 获取权限
        try {
            List<String> permissions = userSessionService.getUserPermissions(token);
            return permissions != null ? permissions : List.of();
        } catch (Exception e) {
            log.error("从 UserSessionService 获取权限失败: token={}", token, e);
            return List.of();
        }
    }

    /**
     * 从请求中获取 token
     * 优先从请求属性中获取（由 UserContextInterceptor 设置），
     * 如果获取不到则尝试从 RequestContextHolder 获取
     * 
     * @return token 字符串，如果获取不到则返回 null
     */
    private String getTokenFromRequest() {
        try {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
                HttpServletRequest request = servletRequestAttributes.getRequest();
                if (request != null) {
                    // 从请求属性中获取 token（由 UserContextInterceptor 设置）
                    Object tokenObj = request.getAttribute(SecurityConstants.REQUEST_ATTR_TOKEN);
                    if (tokenObj instanceof String token) {
                        return token;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("从请求中获取 token 失败", e);
        }
        return null;
    }

    /**
     * 验证权限参数
     */
    private void validatePermissionParams(String[] permissions) {
        if (permissions == null || permissions.length == 0) {
            log.warn("权限列表为空");
            Ex.throwEx(SecurityError.PERMISSION_DENIED, "权限列表不能为空");
        }
    }

    /**
     * 获取当前用户上下文
     */
    private UserContext getCurrentUserContext() {
        UserContext userContext = UserContext.getCurrentUser();
        if (userContext == null) {
            log.warn("用户未登录");
            Ex.throwEx(SecurityError.NOT_LOGIN);
        }
        return userContext;
    }

    /**
     * 检查权限逻辑（AND/OR）
     */
    private boolean checkPermissionLogic(List<String> userPermissions, String[] requiredPermissions, Logical logical) {
        List<String> requiredList = List.of(requiredPermissions);
        if (logical == Logical.AND) {
            // AND 逻辑：需要所有权限
            return new HashSet<>(userPermissions).containsAll(requiredList);
        } else {
            // OR 逻辑：需要任一权限
            return userPermissions.stream().anyMatch(requiredList::contains);
        }
    }

    /**
     * 检查用户是否有指定系统（辅助方法，用于业务代码直接调用）
     * 
     * @param systemId 需要的系统ID
     * @throws com.indigo.core.exception.SynapseException 如果用户未登录或没有所需系统权限
     */
    public void checkSystem(String systemId) {
        checkSystem(new String[]{systemId}, Logical.OR);
    }

    /**
     * 检查用户是否有指定角色（辅助方法，用于业务代码直接调用）
     * 
     * @deprecated 请使用 {@link #checkSystem(String)} 方法
     * @param role 需要的角色
     * @throws com.indigo.core.exception.SynapseException 如果用户未登录或没有所需角色
     */
    @Deprecated
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

