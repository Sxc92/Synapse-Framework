package com.indigo.security.core;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.indigo.cache.session.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 权限管理服务
 * 实现Sa-Token的StpInterface接口，专注于权限和角色的获取逻辑
 * 权限检查请使用Sa-Token的注解：@SaCheckPermission、@SaCheckRole
 * 
 * 注意：该类通过UserSessionService使用synapse-cache模块中的权限管理功能，
 * 避免重复实现权限管理逻辑，同时保持更好的封装性
 *
 * @author 史偕成
 * @date 2025/01/08
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(UserSessionService.class)
public class PermissionManager implements StpInterface {

    private final UserSessionService userSessionService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        try {
            if (loginId == null) {
                log.warn("获取用户权限失败: loginId为空");
                return List.of();
            }

            // 获取当前token
            String token = StpUtil.getTokenValue();
            if (token == null || token.trim().isEmpty()) {
                log.warn("获取用户权限失败: 无法获取当前token, loginId={}", loginId);
                return List.of();
            }
            
            // 通过UserSessionService获取权限
            List<String> permissions = userSessionService.getUserPermissions(token);
            log.debug("获取用户权限列表: loginId={}, token={}, permissions={}", loginId, token, permissions);
            return permissions != null ? permissions : List.of();

        } catch (Exception e) {
            log.error("获取用户权限列表失败: loginId={}, loginType={}", loginId, loginType, e);
            return List.of();
        }
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        try {
            if (loginId == null) {
                log.warn("获取用户角色失败: loginId为空");
                return List.of();
            }

            // 获取当前token
            String token = StpUtil.getTokenValue();
            if (token == null || token.trim().isEmpty()) {
                log.warn("获取用户角色失败: 无法获取当前token, loginId={}", loginId);
                return List.of();
            }
            
            // 通过UserSessionService获取角色
            List<String> roles = userSessionService.getUserRoles(token);
            log.debug("获取用户角色列表: loginId={}, token={}, roles={}", loginId, token, roles);
            return roles != null ? roles : List.of();

        } catch (Exception e) {
            log.error("获取用户角色列表失败: loginId={}, loginType={}", loginId, loginType, e);
            return List.of();
        }
    }
} 