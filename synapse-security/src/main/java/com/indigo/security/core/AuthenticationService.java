package com.indigo.security.core;

import com.indigo.core.entity.Result;
import com.indigo.core.context.UserContext;
import com.indigo.security.model.AuthRequest;
import com.indigo.security.model.AuthResponse;

import java.util.List;

/**
 * 简化的认证服务接口
 * 使用自研的 TokenService 处理所有类型的认证
 *
 * @author 史偕成
 * @date 2025/12/19
 */
public interface AuthenticationService {

    /**
     * 用户登录认证
     * 根据认证类型使用 TokenService 处理
     *
     * @param authRequest 认证请求
     * @return 认证结果
     */
    AuthResponse authenticate(AuthRequest authRequest);

    /**
     * 续期Token
     * 通过 TokenService 续期
     *
     * @param token Token值
     * @return 续期结果
     */
    AuthResponse renewToken(String token);

    /**
     * 获取当前用户信息
     *
     * @return 当前用户上下文
     */
    UserContext getCurrentUser();

    /**
     * 用户登出
     *
     * @return 登出结果
     */
    Result<Void> logout();

    // ========== 菜单、资源、系统管理相关方法 ==========

    /**
     * 存储用户权限数据（菜单、资源、系统）
     * 业务端在登录后调用此方法存储权限数据
     *
     * @param token      访问令牌
     * @param menus      菜单列表
     * @param resources  资源列表
     * @param systems    系统列表
     * @param expiration 过期时间（秒）
     * @param <TMenu>    菜单类型
     * @param <TResource> 资源类型
     * @param <TSystem>   系统类型
     */
    <TMenu, TResource, TSystem> void storeUserPermissionData(
            String token,
            List<TMenu> menus,
            List<TResource> resources,
            List<TSystem> systems,
            long expiration
    );

    /**
     * 获取用户菜单列表
     *
     * @param token 访问令牌
     * @param clazz 菜单类型
     * @param <T>   菜单类型
     * @return 菜单列表
     */
    <T> Result<List<T>> getUserMenus(String token, Class<T> clazz);

    /**
     * 获取用户资源列表
     *
     * @param token 访问令牌
     * @param clazz 资源类型
     * @param <T>   资源类型
     * @return 资源列表
     */
    <T> Result<List<T>> getUserResources(String token, Class<T> clazz);

    /**
     * 获取用户系统列表
     *
     * @param token 访问令牌
     * @param clazz 系统类型
     * @param <T>   系统类型
     * @return 系统列表
     */
    <T> Result<List<T>> getUserSystems(String token, Class<T> clazz);

    /**
     * 存储用户系统菜单树列表
     * 业务端在登录后调用此方法存储系统菜单树数据
     *
     * @param token      访问令牌
     * @param systemMenuTree 系统菜单树列表
     * @param expiration 过期时间（秒）
     * @param <T>        系统菜单树类型
     */
    <T> void storeUserSystemMenuTree(
            String token,
            List<T> systemMenuTree,
            long expiration
    );

    /**
     * 获取用户系统菜单树列表
     *
     * @param token 访问令牌
     * @param clazz 系统菜单树类型
     * @param <T>   系统菜单树类型
     * @return 系统菜单树列表
     */
    <T> Result<List<T>> getUserSystemMenuTree(String token, Class<T> clazz);

    /**
     * 获取用户信息（包含用户基本信息和权限数据）
     * 从 UserContext 获取用户基本信息，从缓存获取权限和系统菜单树
     *
     * @param token 访问令牌
     * @param builder 用户信息构建器，用于将 UserContext、权限列表和系统菜单树组装成目标类型
     * @param <T> 用户信息类型
     * @return 用户信息
     */
    <T> Result<T> getUserInfo(String token, UserInfoBuilder<T> builder);

    /**
     * 用户信息构建器接口
     * 用于将 UserContext、权限列表和系统菜单树组装成目标类型
     *
     * @param <T> 目标类型
     */
    @FunctionalInterface
    interface UserInfoBuilder<T> {
        /**
         * 构建用户信息
         *
         * @param userContext 用户上下文
         * @param permissions 权限列表
         * @param systemMenuTree 系统菜单树列表
         * @return 用户信息对象
         */
        T build(UserContext userContext, List<String> permissions, List<?> systemMenuTree);
    }
} 