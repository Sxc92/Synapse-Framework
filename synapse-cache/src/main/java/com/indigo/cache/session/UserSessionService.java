package com.indigo.cache.session;

import com.indigo.core.context.UserContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 用户会话服务
 * 门面模式（Facade Pattern），协调各个管理器
 * 对外提供统一的会话管理接口
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Slf4j
public class UserSessionService {

    private final SessionManager sessionManager;
    private final CachePermissionManager permissionManager;
    private final StatisticsManager statisticsManager;

    public UserSessionService(SessionManager sessionManager,
                              CachePermissionManager permissionManager,
                              StatisticsManager statisticsManager) {
        this.sessionManager = sessionManager;
        this.permissionManager = permissionManager;
        this.statisticsManager = statisticsManager;
    }

    // ========== 会话管理相关方法 ==========

    /**
     * 存储用户会话信息
     *
     * @param token       访问令牌
     * @param userContext 用户上下文
     * @param expiration  过期时间（秒）
     */
    public void storeUserSession(String token, UserContext userContext, long expiration) {
        sessionManager.storeUserSession(token, userContext, expiration);
    }

    /**
     * 获取用户会话信息
     *
     * @param token 访问令牌
     * @return 用户上下文
     */
    public UserContext getUserSession(String token) {
        return sessionManager.getUserSession(token);
    }

    /**
     * 检查用户会话是否存在
     *
     * @param token 访问令牌
     * @return 是否存在
     */
    public boolean hasUserSession(String token) {
        return sessionManager.hasUserSession(token);
    }

    /**
     * 删除用户会话（跨管理器事务操作）
     *
     * @param token 访问令牌
     */
    public void removeUserSession(String token) {
        sessionManager.removeUserSession(token);
        permissionManager.removeUserPermissions(token);
        log.info("Removed complete user session and permissions for token: {}", token);
    }

    /**
     * 延长用户会话过期时间（跨管理器事务操作）
     *
     * @param token      访问令牌
     * @param expiration 新的过期时间（秒）
     */
    public void extendUserSession(String token, long expiration) {
        sessionManager.extendUserSession(token, expiration);
        permissionManager.extendUserPermissions(token, expiration);
        log.info("Extended complete user session and permissions for token: {}", token);
    }

    /**
     * 获取token剩余时间
     *
     * @param token 访问令牌
     * @return 剩余时间（秒），如果token不存在返回-1
     */
    public long getTokenRemainingTime(String token) {
        return sessionManager.getTokenRemainingTime(token);
    }

    /**
     * 续期token
     *
     * @param token    访问令牌
     * @param duration 续期时间（秒）
     * @return 是否续期成功
     */
    public boolean renewToken(String token, long duration) {
        boolean sessionRenewed = sessionManager.renewToken(token, duration);
        if (sessionRenewed) {
            permissionManager.extendUserPermissions(token, duration);
        }
        return sessionRenewed;
    }

    // ========== Token管理相关方法 ==========

    /**
     * 存储token
     * 
     * @param token 令牌
     * @param userId 用户ID
     * @param expireSeconds 过期时间（秒）
     */
    public void storeToken(String token, String userId, long expireSeconds) {
        sessionManager.storeToken(token, userId, expireSeconds);
    }
    
    /**
     * 验证token并获取用户ID
     * 
     * @param token 令牌
     * @return 用户ID，token无效时返回null
     */
    public String validateToken(String token) {
        return sessionManager.validateToken(token);
    }
    
    /**
     * 刷新token过期时间
     * 
     * @param token 令牌
     * @param expireSeconds 新的过期时间（秒）
     * @return 是否刷新成功
     */
    public boolean refreshToken(String token, long expireSeconds) {
        return sessionManager.refreshToken(token, expireSeconds);
    }
    
    /**
     * 删除token
     * 
     * @param token 令牌
     */
    public void removeToken(String token) {
        sessionManager.removeToken(token);
    }
    
    /**
     * 检查token是否存在
     * 
     * @param token 令牌
     * @return 是否存在
     */
    public boolean tokenExists(String token) {
        return sessionManager.tokenExists(token);
    }
    
    /**
     * 获取token剩余过期时间
     * 
     * @param token 令牌
     * @return 剩余过期时间（秒），-1表示永不过期，-2表示不存在
     */
    public long getTokenTtl(String token) {
        return sessionManager.getTokenTtl(token);
    }

    // ========== 会话数据管理相关方法 ==========
    
    /**
     * 存储用户会话数据
     * 
     * @param userId 用户ID
     * @param sessionData 会话数据
     * @param expireSeconds 过期时间（秒）
     */
    public void storeUserSessionData(String userId, Object sessionData, long expireSeconds) {
        sessionManager.storeUserSessionData(userId, sessionData, expireSeconds);
    }
    
    /**
     * 获取用户会话数据
     * 
     * @param userId 用户ID
     * @param clazz 数据类型
     * @return 会话数据
     */
    public <T> T getUserSessionData(String userId, Class<T> clazz) {
        return sessionManager.getUserSessionData(userId, clazz);
    }
    
    /**
     * 删除用户会话数据
     * 
     * @param userId 用户ID
     */
    public void removeUserSessionData(String userId) {
        sessionManager.removeUserSessionData(userId);
    }

    // ========== 权限管理相关方法 ==========

    /**
     * 存储用户权限信息
     *
     * @param token       访问令牌
     * @param permissions 权限列表
     * @param expiration  过期时间（秒）
     */
    public void storeUserPermissions(String token, List<String> permissions, long expiration) {
        permissionManager.storeUserPermissions(token, permissions, expiration);
    }

    /**
     * 存储用户角色信息
     *
     * @param token      访问令牌
     * @param roles      角色列表
     * @param expiration 过期时间（秒）
     */
    public void storeUserRoles(String token, List<String> roles, long expiration) {
        permissionManager.storeUserRoles(token, roles, expiration);
    }

    /**
     * 获取用户权限列表
     *
     * @param token 访问令牌
     * @return 权限列表
     */
    public List<String> getUserPermissions(String token) {
        return permissionManager.getUserPermissions(token);
    }

    /**
     * 获取用户角色列表
     *
     * @param token 访问令牌
     * @return 角色列表
     */
    public List<String> getUserRoles(String token) {
        return permissionManager.getUserRoles(token);
    }

    /**
     * 检查用户是否有指定权限
     *
     * @param token      访问令牌
     * @param permission 权限标识
     * @return 是否有权限
     */
    public boolean hasPermission(String token, String permission) {
        return permissionManager.hasPermission(token, permission);
    }

    /**
     * 检查用户是否有指定角色
     *
     * @param token 访问令牌
     * @param role  角色标识
     * @return 是否有角色
     */
    public boolean hasRole(String token, String role) {
        return permissionManager.hasRole(token, role);
    }

    // ========== 统计管理相关方法 ==========

    /**
     * 获取所有在线用户列表
     *
     * @return 在线用户列表
     */
    public List<UserContext> getOnlineUsers() {
        return statisticsManager.getOnlineUsers();
    }

    /**
     * 获取指定租户的在线用户列表
     *
     * @param tenantId 租户ID
     * @return 在线用户列表
     */
    public List<UserContext> getOnlineUsersByTenant(Long tenantId) {
        return statisticsManager.getOnlineUsersByTenant(tenantId);
    }

    /**
     * 获取指定部门的在线用户列表
     *
     * @param deptId 部门ID
     * @return 在线用户列表
     */
    public List<UserContext> getOnlineUsersByDept(Long deptId) {
        return statisticsManager.getOnlineUsersByDept(deptId);
    }

    /**
     * 获取指定角色的在线用户列表
     *
     * @param role 角色标识
     * @return 在线用户列表
     */
    public List<UserContext> getOnlineUsersByRole(String role) {
        return statisticsManager.getOnlineUsersByRole(role);
    }

    /**
     * 获取在线用户数量
     *
     * @return 在线用户数量
     */
    public long getOnlineUserCount() {
        return statisticsManager.getOnlineUserCount();
    }

    /**
     * 检查用户是否在线
     *
     * @param userId 用户ID
     * @return 是否在线
     */
    public boolean isUserOnline(Long userId) {
        return statisticsManager.isUserOnline(userId);
    }

    /**
     * 强制用户下线
     *
     * @param userId 用户ID
     * @return 是否成功下线
     */
    public boolean forceUserOffline(Long userId) {
        return statisticsManager.forceUserOffline(userId);
    }

    /**
     * 批量强制用户下线
     *
     * @param userIds 用户ID列表
     * @return 成功下线的用户数量
     */
    public int forceUsersOffline(List<Long> userIds) {
        return statisticsManager.forceUsersOffline(userIds);
    }

    /**
     * 获取用户会话统计信息
     *
     * @return 会话统计信息
     */
    public StatisticsManager.UserSessionStats getUserSessionStats() {
        return statisticsManager.getUserSessionStats();
    }

    /**
     * 获取指定时间范围内的活跃用户
     *
     * @param minutes 时间范围（分钟）
     * @return 活跃用户列表
     */
    public List<UserContext> getActiveUsers(int minutes) {
        return statisticsManager.getActiveUsers(minutes);
    }

    /**
     * 获取用户登录统计信息
     *
     * @return 登录统计信息
     */
    public StatisticsManager.LoginStats getLoginStats() {
        return statisticsManager.getLoginStats();
    }

    /**
     * 获取用户在线时长
     *
     * @param userId 用户ID
     * @return 在线时长（毫秒）
     */
    public Long getUserOnlineDuration(Long userId) {
        return statisticsManager.getUserOnlineDuration(userId);
    }

    /**
     * 获取所有在线用户的在线时长
     *
     * @return 用户在线时长映射
     */
    public Map<String, Long> getAllUsersOnlineDuration() {
        return statisticsManager.getAllUsersOnlineDuration();
    }
} 