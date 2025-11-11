package com.indigo.security.core;

import com.indigo.core.entity.Result;
import com.indigo.core.context.UserContext;
import com.indigo.security.model.AuthRequest;
import com.indigo.security.model.AuthResponse;

import java.util.List;

/**
 * 简化的认证服务接口
 * 直接使用Sa-Token框架处理所有类型的认证
 *
 * @author 史偕成
 * @date 2025/12/19
 */
public interface AuthenticationService {

    /**
     * 用户登录认证
     * 根据认证类型使用Sa-Token框架处理
     *
     * @param authRequest 认证请求
     * @return 认证结果
     */
    AuthResponse authenticate(AuthRequest authRequest);

    /**
     * 续期Token
     * 通过Sa-Token框架续期
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
} 