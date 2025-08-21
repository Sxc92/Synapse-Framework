package com.indigo.security.core;

import com.indigo.core.entity.Result;
import com.indigo.core.context.UserContext;
import com.indigo.security.model.AuthRequest;
import com.indigo.security.model.AuthResponse;

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
    Result<AuthResponse> authenticate(AuthRequest authRequest);

    /**
     * 续期Token
     * 通过Sa-Token框架续期
     *
     * @param token Token值
     * @return 续期结果
     */
    Result<AuthResponse> renewToken(String token);

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
} 