package com.indigo.security.core;

import cn.dev33.satoken.stp.StpInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 权限管理服务
 * 实现Sa-Token的StpInterface接口，专注于权限和角色的获取逻辑
 * 权限检查请使用Sa-Token的注解：@SaCheckPermission、@SaCheckRole
 *
 * @author 史偕成
 * @date 2024/01/08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionManager implements StpInterface {

    private final TokenManager tokenManager;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        try {
            if (loginId == null) {
                log.warn("获取用户权限失败: loginId为空");
                return List.of();
            }

            // 通过TokenManager获取当前token
            String token = tokenManager.getCurrentToken();
            if (token == null) {
                log.warn("获取用户权限失败: 无法获取当前token, loginId={}", loginId);
                return List.of();
            }

            // 通过TokenManager获取用户上下文
            var userContext = tokenManager.getUserContext(token);
            if (userContext == null) {
                log.warn("获取用户权限失败: 用户上下文为空, loginId={}, token={}", loginId, token);
                return List.of();
            }

            List<String> permissions = userContext.getPermissions();
            log.debug("获取用户权限列表: loginId={}, permissions={}", loginId, permissions);
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

            // 通过TokenManager获取当前token
            String token = tokenManager.getCurrentToken();
            if (token == null) {
                log.warn("获取用户角色失败: 无法获取当前token, loginId={}", loginId);
                return List.of();
            }

            // 通过TokenManager获取用户上下文
            var userContext = tokenManager.getUserContext(token);
            if (userContext == null) {
                log.warn("获取用户角色失败: 用户上下文为空, loginId={}, token={}", loginId, token);
                return List.of();
            }

            List<String> roles = userContext.getRoles();
            log.debug("获取用户角色列表: loginId={}, roles={}", loginId, roles);
            return roles != null ? roles : List.of();

        } catch (Exception e) {
            log.error("获取用户角色列表失败: loginId={}, loginType={}", loginId, loginType, e);
            return List.of();
        }
    }
} 