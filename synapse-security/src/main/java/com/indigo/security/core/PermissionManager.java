package com.indigo.security.core;

import cn.dev33.satoken.stp.StpInterface;
import com.indigo.cache.session.UserSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 简化的权限管理服务
 * 实现Sa-Token的StpInterface接口，避免重复造轮子
 * 权限检查请使用Sa-Token的注解：@SaCheckPermission、@SaCheckRole
 *
 * @author 史偕成
 * @date 2024/01/08
 */
@Slf4j
@Service
public class PermissionManager implements StpInterface {

    private final UserSessionService userSessionService;

    public PermissionManager(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
        log.info("权限管理服务初始化完成");
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        try {
            String token = resolveToken(loginId);
            if (token != null) {
                List<String> permissions = userSessionService.getUserPermissions(token);
                log.debug("获取用户权限列表: loginId={}, permissions={}", loginId, permissions);
                return permissions;
            }
        } catch (Exception e) {
            log.error("获取用户权限列表失败: loginId={}, loginType={}", loginId, loginType, e);
        }
        return List.of();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        try {
            String token = resolveToken(loginId);
            if (token != null) {
                List<String> roles = userSessionService.getUserRoles(token);
                log.debug("获取用户角色列表: loginId={}, roles={}", loginId, roles);
                return roles;
            }
        } catch (Exception e) {
            log.error("获取用户角色列表失败: loginId={}, loginType={}", loginId, loginType, e);
        }
        return List.of();
    }

    /**
     * 解析Token
     * 这里需要根据你的业务逻辑实现如何通过loginId获取token
     */
    private String resolveToken(Object loginId) {
        return loginId != null ? loginId.toString() : null;
    }
} 