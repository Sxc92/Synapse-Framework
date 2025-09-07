package com.indigo.security.service;

import com.indigo.cache.core.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 登录安全防护服务
 * 提供登录失败次数限制、IP白名单/黑名单、异常登录检测等功能
 *
 * @author 史偕成
 * @date 2025/01/08
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(CacheService.class)
public class LoginSecurityService {

    private final CacheService cacheService;

    // 配置项
    @Value("${synapse.security.login.max-fail-count:5}")
    private int maxFailCount;

    @Value("${synapse.security.login.lock-duration:1800}")
    private long lockDurationSeconds;

    @Value("${synapse.security.login.fail-window:300}")
    private long failWindowSeconds;

    // 缓存键前缀
    private static final String LOGIN_FAIL_PREFIX = "login_fail:";
    private static final String ACCOUNT_LOCK_PREFIX = "account_lock:";
    private static final String IP_ATTEMPT_PREFIX = "ip_attempt:";

    /**
     * 检查账号是否被锁定
     */
    public boolean isAccountLocked(String username) {
        String lockKey = ACCOUNT_LOCK_PREFIX + username;
        return cacheService.hasKey(lockKey);
    }

    /**
     * 记录登录失败
     */
    public int recordLoginFail(String username, String ip) {
        String failKey = LOGIN_FAIL_PREFIX + username;
        String ipKey = IP_ATTEMPT_PREFIX + ip;

        Integer failCount = cacheService.getObject(failKey, Integer.class);
        failCount = (failCount == null) ? 1 : failCount + 1;
        cacheService.setObject(failKey, failCount, failWindowSeconds);

        Integer ipAttempts = cacheService.getObject(ipKey, Integer.class);
        ipAttempts = (ipAttempts == null) ? 1 : ipAttempts + 1;
        cacheService.setObject(ipKey, ipAttempts, failWindowSeconds);

        if (failCount >= maxFailCount) {
            lockAccount(username, ip);
            
            // TODO: 操作审计 - 记录账号锁定事件
            // auditService.logUserAction(username, "ACCOUNT_LOCKED", "login_security", "FAILED: 登录失败次数过多");
        }

        return failCount;
    }

    /**
     * 记录登录成功
     */
    public void recordLoginSuccess(String username, String ip) {
        String failKey = LOGIN_FAIL_PREFIX + username;
        cacheService.delete(failKey);
        
        // TODO: 操作审计 - 记录登录成功事件
        // auditService.logUserAction(username, "LOGIN_SUCCESS", "login_security", "SUCCESS");
        
        log.info("用户登录成功: username={}, ip={}", username, ip);
    }

    /**
     * 解锁账号
     */
    public boolean unlockAccount(String username) {
        String lockKey = ACCOUNT_LOCK_PREFIX + username;
        String failKey = LOGIN_FAIL_PREFIX + username;

        boolean unlocked = cacheService.delete(lockKey);
        if (unlocked) {
            cacheService.delete(failKey);
        }
        return unlocked;
    }

    private void lockAccount(String username, String ip) {
        String lockKey = ACCOUNT_LOCK_PREFIX + username;
        cacheService.setObject(lockKey, ip, lockDurationSeconds);
        log.warn("账号被锁定: username={}, ip={}, duration={}秒", username, ip, lockDurationSeconds);
    }
}
