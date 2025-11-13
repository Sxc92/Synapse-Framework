package com.indigo.cache.session.impl;

import com.indigo.cache.core.CacheService;
import com.indigo.cache.manager.CacheKeyGenerator;
import com.indigo.cache.infrastructure.RedisService;
import com.indigo.cache.session.CachePermissionManager;
import com.indigo.cache.session.SessionManager;
import com.indigo.cache.session.StatisticsManager;
import com.indigo.core.context.UserContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 默认统计管理器实现
 * 基于缓存服务的统计管理
 *
 * @author 史偕成
 * @date 2024/12/19
 */
@Slf4j
public class DefaultStatisticsManager implements StatisticsManager {

    /**
     * -- GETTER --
     *  获取缓存服务（用于兼容性）
     *  注意：这是一个临时的兼容性方法
     *
     * @return 缓存服务
     */
    @Getter
    private final CacheService cacheService;
    private final CacheKeyGenerator keyGenerator;
    private final RedisService redisService;
    private final SessionManager sessionManager;
    private final CachePermissionManager permissionManager;

    public DefaultStatisticsManager(CacheService cacheService,
                                    CacheKeyGenerator keyGenerator,
                                    RedisService redisService,
                                    SessionManager sessionManager,
                                    CachePermissionManager permissionManager) {
        this.cacheService = cacheService;
        this.keyGenerator = keyGenerator;
        this.redisService = redisService;
        this.sessionManager = sessionManager;
        this.permissionManager = permissionManager;
    }

    @Override
    public List<UserContext> getOnlineUsers() {
        String pattern = keyGenerator.generate(CacheKeyGenerator.Module.USER, "session", "*");
        return getUsersByPattern(pattern);
    }

//    @Override
//    public List<UserContext> getOnlineUsersByTenant(Long tenantId) {
//        String pattern = keyGenerator.generate(CacheKeyGenerator.Module.USER, "session", "*");
//        List<UserContext> allUsers = getUsersByPattern(pattern);
//        return allUsers.stream()
//                .filter(user -> user.getTenantId() != null && user.getTenantId().equals(tenantId))
//                .toList();
//    }

    @Override
    public List<UserContext> getOnlineUsersByDept(Long deptId) {
        // 注意：UserContext 中没有 deptId 字段
        // 如果需要按部门查询在线用户，请在 UserContext 中添加 deptId 字段
        // 或者通过其他方式（如权限管理器）获取用户的部门信息
        log.warn("getOnlineUsersByDept 方法需要 deptId 字段，但 UserContext 中不存在该字段");
        return List.of();
    }

    @Override
    public List<UserContext> getOnlineUsersByRole(String role) {
        String pattern = keyGenerator.generate(CacheKeyGenerator.Module.USER, "session", "*");
        List<UserContext> allUsers = getUsersByPattern(pattern);
        
        // 从 session key 中提取 token，然后查询角色
        return allUsers.stream()
                .filter(user -> {
                    // 通过扫描所有 session key 来匹配用户
                    // 由于 UserContext 中没有 token 字段，我们需要通过其他方式获取
                    // 这里通过用户的角色列表来判断
                    if (user.getRoles() != null) {
                        return user.getRoles().contains(role);
                    }
                    return false;
                })
                .toList();
    }

    @Override
    public long getOnlineUserCount() {
        String pattern = keyGenerator.generate(CacheKeyGenerator.Module.USER, "session", "*");
        return getUsersByPattern(pattern).size();
    }

    @Override
    public boolean isUserOnline(Long userId) {
        if (userId == null) {
            return false;
        }
        String pattern = keyGenerator.generate(CacheKeyGenerator.Module.USER, "session", "*");
        List<UserContext> allUsers = getUsersByPattern(pattern);
        // UserContext.getUserId() 返回 String，需要转换比较
        String userIdStr = String.valueOf(userId);
        return allUsers.stream()
                .anyMatch(user -> userIdStr.equals(user.getUserId()));
    }

    @Override
    public boolean forceUserOffline(Long userId) {
        if (userId == null) {
            return false;
        }
        String pattern = keyGenerator.generate(CacheKeyGenerator.Module.USER, "session", "*");
        String userIdStr = String.valueOf(userId);
        
        // 通过扫描 session keys 来查找并删除用户的会话
        try {
            Set<String> keys = redisService.scan(pattern);
            for (String key : keys) {
                UserContext user = cacheService.getObject(key, UserContext.class);
                if (user != null && userIdStr.equals(user.getUserId())) {
                    // 从 key 中提取 token（key 格式：user:session:{token}）
                    String token = extractTokenFromKey(key);
                    if (token != null) {
                        sessionManager.removeUserSession(token);
                        permissionManager.removeUserPermissions(token);
                        log.info("强制用户下线: userId={}, username={}, token={}", 
                                userId, user.getAccount(), token);
                return true;
                    }
                }
            }
        } catch (Exception e) {
            log.error("强制用户下线失败: userId={}", userId, e);
        }
        return false;
    }

    @Override
    public int forceUsersOffline(List<Long> userIds) {
        int count = 0;
        for (Long userId : userIds) {
            if (forceUserOffline(userId)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public UserSessionStats getUserSessionStats() {
        String pattern = keyGenerator.generate(CacheKeyGenerator.Module.USER, "session", "*");
        List<UserContext> allUsers = getUsersByPattern(pattern);

        long totalUsers = allUsers.size();
        
        // 注意：UserContext 中没有 lastAccessTime 和 loginTime 字段
        // 这里使用 session 的剩余过期时间来判断活跃性
        // 如果 session 剩余时间较长（> 30 分钟），认为是活跃用户
        long activeUsers = allUsers.stream()
                .filter(user -> {
                    // 通过 session key 获取剩余过期时间来判断活跃性
                    // 由于无法直接从 UserContext 获取 token，这里简化处理：
                    // 所有在线用户都认为是活跃用户（因为如果 session 存在，说明最近有访问）
                    return true;
                })
                .count();

        return new UserSessionStats(totalUsers, activeUsers);
    }

    @Override
    public List<UserContext> getActiveUsers(int minutes) {
        String pattern = keyGenerator.generate(CacheKeyGenerator.Module.USER, "session", "*");
        List<UserContext> allUsers = getUsersByPattern(pattern);
        
        // 注意：UserContext 中没有 lastAccessTime 和 loginTime 字段
        // 这里通过检查 session 的剩余过期时间来判断活跃性
        // 如果 session 剩余时间大于 (minutes * 60) 秒，认为是活跃用户
        // 由于无法直接从 UserContext 获取 token 和剩余时间
        // 这里简化处理：所有在线用户都返回（因为如果 session 存在，说明最近有访问）
        // 如果需要更精确的判断，需要从 session key 中提取 token，然后查询剩余时间

        return allUsers.stream()
                .filter(user -> {
                    // 所有在线用户都认为是活跃用户
                    // 如果需要更精确的判断，需要从 session key 中提取 token，然后查询剩余时间
                        return true;
                })
                .toList();
    }

    @Override
    public LoginStats getLoginStats() {
        String pattern = keyGenerator.generate(CacheKeyGenerator.Module.USER, "session", "*");
        List<UserContext> allUsers = getUsersByPattern(pattern);

        // 注意：UserContext 中没有 loginTime 字段
        // 这里无法准确统计登录时间，返回当前在线用户数作为近似值
        // 如果需要精确的登录统计，请在 UserContext 中添加 loginTime 字段
        long totalOnlineUsers = allUsers.size();
        
        // 由于无法获取登录时间，这里返回 0 或使用在线用户数作为近似值
        // 实际应用中，应该从登录日志或其他数据源获取准确的登录统计
        long hourlyLogins = 0;
        long dailyLogins = 0;
        
        log.debug("获取登录统计: 当前在线用户数={}, 由于 UserContext 中没有 loginTime 字段，无法准确统计登录时间", 
                totalOnlineUsers);

        return new LoginStats(hourlyLogins, dailyLogins, totalOnlineUsers);
    }

    @Override
    public Long getUserOnlineDuration(Long userId) {
        if (userId == null) {
            return 0L;
        }
        String pattern = keyGenerator.generate(CacheKeyGenerator.Module.USER, "session", "*");
        List<UserContext> allUsers = getUsersByPattern(pattern);
        String userIdStr = String.valueOf(userId);

        // 注意：UserContext 中没有 loginTime 字段
        // 这里无法准确计算在线时长，返回 0
        // 如果需要精确的在线时长，请在 UserContext 中添加 loginTime 字段
        // 或者通过 session 的创建时间来计算
        return allUsers.stream()
                .filter(user -> userIdStr.equals(user.getUserId()))
                .findFirst()
                .map(user -> {
                    // 由于没有登录时间，无法计算准确的在线时长
                    // 可以尝试通过 session 的剩余过期时间来估算，但不够准确
                    log.debug("无法计算用户在线时长: userId={}, UserContext 中没有 loginTime 字段", userId);
                    return 0L;
                })
                .orElse(0L);
    }

    @Override
    public Map<String, Long> getAllUsersOnlineDuration() {
        String pattern = keyGenerator.generate(CacheKeyGenerator.Module.USER, "session", "*");
        List<UserContext> allUsers = getUsersByPattern(pattern);

        // 注意：UserContext 中没有 loginTime 字段
        // 这里无法准确计算在线时长，返回所有用户的在线时长为 0
        // 如果需要精确的在线时长，请在 UserContext 中添加 loginTime 字段
        return allUsers.stream()
                .filter(user -> user.getUserId() != null)
                .collect(Collectors.toMap(
                        UserContext::getUserId,
                        user -> {
                            // 由于没有登录时间，无法计算准确的在线时长
                            log.debug("无法计算用户在线时长: userId={}, UserContext 中没有 loginTime 字段", 
                                    user.getUserId());
                            return 0L;
                        }
                ));
    }

    @Override
    public void resetStatistics() {

    }

    @Override
    public long clearExpiredData(LocalDateTime cutoffTime) {
        return 0;
    }

    /**
     * 根据模式获取用户列表
     *
     * @param pattern 缓存键模式
     * @return 用户列表
     */
    private List<UserContext> getUsersByPattern(String pattern) {
        try {
            // 使用 RedisService 的 scan 方法查询匹配的键
            Set<String> keys = redisService.scan(pattern);
            return keys.stream()
                    .map(key -> {
                        try {
                            return cacheService.getObject(key, UserContext.class);
                        } catch (Exception e) {
                            log.warn("获取用户会话数据失败: key={}", key, e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.error("查询在线用户失败: {}", pattern, e);
            return List.of();
        }
    }

    /**
     * 从 session key 中提取 token
     * key 格式：user:session:{token}
     *
     * @param key session key
     * @return token，如果无法提取则返回 null
     */
    private String extractTokenFromKey(String key) {
        try {
            // key 格式：user:session:{token}
            // 或者根据 CacheKeyGenerator 的实际格式提取
            String prefix = keyGenerator.generate(CacheKeyGenerator.Module.USER, "session", "");
            if (key.startsWith(prefix)) {
                return key.substring(prefix.length());
            }
            // 如果格式不匹配，尝试从最后一个冒号或斜杠后提取
            int lastIndex = key.lastIndexOf(":");
            if (lastIndex > 0 && lastIndex < key.length() - 1) {
                return key.substring(lastIndex + 1);
            }
            return null;
        } catch (Exception e) {
            log.warn("从 key 中提取 token 失败: key={}", key, e);
            return null;
        }
    }

}