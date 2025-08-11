package com.indigo.cache.session;

import com.indigo.core.context.UserContext;

/**
 * 会话管理器接口
 * 定义用户会话管理的核心操作，包括会话、Token和会话数据管理
 *
 * @author 史偕成
 * @date 2024/12/19
 */
public interface SessionManager {

    // ========== 用户会话管理 ==========
    
    /**
     * 存储用户会话信息
     *
     * @param token       访问令牌
     * @param userContext 用户上下文
     * @param expiration  过期时间（秒）
     */
    void storeUserSession(String token, UserContext userContext, long expiration);

    /**
     * 获取用户会话信息
     *
     * @param token 访问令牌
     * @return 用户上下文
     */
    UserContext getUserSession(String token);

    /**
     * 检查用户会话是否存在
     *
     * @param token 访问令牌
     * @return 是否存在
     */
    boolean hasUserSession(String token);

    /**
     * 删除用户会话
     *
     * @param token 访问令牌
     */
    void removeUserSession(String token);

    /**
     * 延长用户会话过期时间
     *
     * @param token      访问令牌
     * @param expiration 新的过期时间（秒）
     */
    void extendUserSession(String token, long expiration);

    /**
     * 获取token剩余时间
     *
     * @param token 访问令牌
     * @return 剩余时间（秒），如果token不存在返回-1
     */
    long getTokenRemainingTime(String token);

    /**
     * 续期token
     *
     * @param token    访问令牌
     * @param duration 续期时间（秒）
     * @return 是否续期成功
     */
    boolean renewToken(String token, long duration);

    // ========== Token基础管理 ==========
    
    /**
     * 存储token（直接token存储，不同于会话存储）
     * 
     * @param token 令牌
     * @param userId 用户ID
     * @param expireSeconds 过期时间（秒）
     */
    void storeToken(String token, String userId, long expireSeconds);
    
    /**
     * 验证token并获取用户ID
     * 
     * @param token 令牌
     * @return 用户ID，token无效时返回null
     */
    String validateToken(String token);
    
    /**
     * 刷新token过期时间
     * 
     * @param token 令牌
     * @param expireSeconds 新的过期时间（秒）
     * @return 是否刷新成功
     */
    boolean refreshToken(String token, long expireSeconds);
    
    /**
     * 删除token（用户登出）
     * 
     * @param token 令牌
     */
    void removeToken(String token);
    
    /**
     * 检查token是否存在
     * 
     * @param token 令牌
     * @return 是否存在
     */
    boolean tokenExists(String token);
    
    /**
     * 获取token剩余过期时间
     * 
     * @param token 令牌
     * @return 剩余过期时间（秒），-1表示永不过期，-2表示不存在
     */
    long getTokenTtl(String token);

    // ========== 会话数据管理 ==========
    
    /**
     * 存储用户会话数据（基于用户ID）
     * 
     * @param userId 用户ID
     * @param sessionData 会话数据
     * @param expireSeconds 过期时间（秒）
     */
    void storeUserSessionData(String userId, Object sessionData, long expireSeconds);
    
    /**
     * 获取用户会话数据（基于用户ID）
     * 
     * @param userId 用户ID
     * @param clazz 数据类型
     * @return 会话数据
     */
    <T> T getUserSessionData(String userId, Class<T> clazz);
    
    /**
     * 删除用户会话数据（基于用户ID）
     * 
     * @param userId 用户ID
     */
    void removeUserSessionData(String userId);
    

} 