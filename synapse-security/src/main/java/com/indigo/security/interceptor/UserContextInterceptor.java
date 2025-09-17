package com.indigo.security.interceptor;

import com.indigo.cache.session.UserSessionService;
import com.indigo.core.context.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 简化的用户上下文拦截器
 * 只负责设置用户上下文到ThreadLocal，不重复Sa-Token的权限检查功能
 * 权限检查请使用Sa-Token的注解：@SaCheckLogin、@SaCheckPermission、@SaCheckRole
 *
 * @author 史偕成
 * @date 2025/03/21
 */
@Slf4j
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "org.springframework.web.servlet.HandlerInterceptor")
@ConditionalOnMissingBean(UserContextInterceptor.class)
public class UserContextInterceptor implements HandlerInterceptor {

    private final UserSessionService userSessionService;

    public UserContextInterceptor(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            // 从请求头获取 token
            String token = extractToken(request);
            
            if (StringUtils.hasText(token)) {
                // 从 Redis 中获取用户上下文
                UserContext userContext = userSessionService.getUserSession(token);
                
                if (userContext != null) {
                    // 设置到 ThreadLocal
                    UserContext.setCurrentUser(userContext);
                    log.info("用户上下文已设置: userId={}, username={}", 
                            userContext.getUserId(), userContext.getUsername());
                } else {
                    log.info("未找到用户上下文，token: {}", token);
                }
            } else {
                log.info("请求中未包含 token，URL: {}", request.getRequestURI());
            }
        } catch (Exception e) {
            log.error("设置用户上下文时发生异常", e);
        }
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理 ThreadLocal，防止内存泄漏
        UserContext.clearCurrentUser();
    }

    /**
     * 从请求头中提取 token
     */
    private String extractToken(HttpServletRequest request) {
        // 优先从 Authorization 头获取
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // 从自定义头获取
        String token = request.getHeader("X-Auth-Token");
        if (StringUtils.hasText(token)) {
            return token;
        }
        
        // 从参数获取
        token = request.getParameter("token");
        if (StringUtils.hasText(token)) {
            return token;
        }
        
        return null;
    }
} 