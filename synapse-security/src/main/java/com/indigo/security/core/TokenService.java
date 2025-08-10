package com.indigo.security.core;

import com.indigo.security.model.TokenInfo;
import com.indigo.security.model.UserPrincipal;

/**
 * 高级Token管理服务接口
 * 负责Token的高级操作，如刷新、撤销、黑名单管理等
 * 与TokenManager形成分层架构：
 * - TokenManager: 基础Token操作（登录、验证、续期）
 * - TokenService: 高级Token操作（刷新、撤销、黑名单）
 *
 * @author 史偕成
 * @date 2024/12/19
 */
public interface TokenService {

    /**
     * 刷新访问令牌
     * 使用刷新令牌生成新的访问令牌
     *
     * @param refreshToken 刷新令牌
     * @return 新的令牌信息
     */
    TokenInfo refreshToken(String refreshToken);

    /**
     * 撤销访问令牌
     * 将令牌加入黑名单，使其失效
     *
     * @param token 访问令牌
     * @return 是否成功撤销
     */
    boolean revokeToken(String token);

    /**
     * 撤销用户的所有令牌
     * 用户登出或账号被禁用时使用
     *
     * @param userId 用户ID
     * @return 撤销的令牌数量
     */
    int revokeAllUserTokens(Long userId);

    /**
     * 检查令牌是否在黑名单中
     *
     * @param token 访问令牌
     * @return 是否在黑名单中
     */
    boolean isTokenBlacklisted(String token);

    /**
     * 获取令牌详细信息
     * 包含令牌的元数据信息
     *
     * @param token 访问令牌
     * @return 令牌信息
     */
    TokenInfo getTokenInfo(String token);

    /**
     * 清理过期的黑名单令牌
     * 定期清理任务调用
     *
     * @return 清理的令牌数量
     */
    int cleanupExpiredBlacklistedTokens();
} 