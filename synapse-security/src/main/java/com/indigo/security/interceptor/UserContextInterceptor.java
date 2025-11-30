package com.indigo.security.interceptor;

import cn.hutool.core.collection.CollUtil;
import com.indigo.cache.session.UserSessionService;
import com.indigo.core.context.UserContext;
import com.indigo.core.entity.Result;
import com.indigo.core.utils.JsonUtils;
import com.indigo.security.annotation.RequireLogin;
import com.indigo.security.annotation.RequirePermission;
import com.indigo.security.annotation.RequireRole;
import com.indigo.security.config.SecurityProperties;
import com.indigo.security.constants.SecurityConstants;
import com.indigo.security.constants.SecurityError;
import com.indigo.security.exception.NotLoginException;
import com.indigo.security.utils.GatewaySignatureUtils;
import com.indigo.security.utils.InternalSignatureUtils;
import com.indigo.security.utils.TokenExtractor;
import com.indigo.security.utils.UserContextCodec;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 
 * 用户上下文拦截器
 * 负责从请求中的 token 获取用户信息，并设置到 ThreadLocal 中
 * 工作流程：
 * 1. 从请求头或查询参数中提取 token（Authorization Bearer、X-Auth-Token 或查询参数）
 * 2. 使用 UserSessionService 从 Redis 获取用户上下文
 * 3. 将用户上下文设置到 ThreadLocal 中，供业务代码使用
 * 注意：
 * - 此拦截器只负责设置用户上下文到ThreadLocal，不进行权限检查
 * - 权限检查请使用自定义注解：@RequireLogin、@RequirePermission、@RequireRole
 * - Gateway 只传递 token，不传递用户信息，下游服务统一从 token 获取
 *
 * @author 史偕成
 * @date 2025/03/21
 */
/**
 * 用户上下文拦截器
 *
 * <p><b>注意：</b>此拦截器不再使用 @Component 注解，改为在 {@link com.indigo.security.config.SecurityAutoConfiguration}
 * 中通过 @Bean 方法创建，这样可以更好地控制 Bean 的创建顺序和依赖关系。
 *
 * <p>拦截器会在 {@link com.indigo.security.config.WebMvcSecurityConfig} 中自动注册到 Spring MVC。
 */
@Slf4j
public class UserContextInterceptor implements HandlerInterceptor {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final SecurityProperties securityProperties;
    private final TokenExtractor tokenExtractor;
    private final UserSessionService userSessionService;

    /**
     * 构造函数
     *
     * @param securityProperties 安全配置属性（必须）
     * @param tokenExtractor Token 提取工具（必须）
     * @param userSessionService 用户会话服务（可选，用于滑动过期刷新 token）
     */
    public UserContextInterceptor(SecurityProperties securityProperties,
                                  TokenExtractor tokenExtractor,
                                  UserSessionService userSessionService) {
        this.securityProperties = securityProperties;
        this.tokenExtractor = tokenExtractor;
        this.userSessionService = userSessionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            // 1. 优先检查是否是内部服务调用
            if (isInternalServiceCall(request)) {
                // 内部服务调用：验证签名即可，不需要用户上下文
                if (verifyInternalServiceSignature(request)) {
                    log.debug("内部服务调用验证通过: service={}, URL={}", 
                            request.getHeader(SecurityConstants.X_INTERNAL_SERVICE_HEADER), 
                            request.getRequestURI());
                    // 内部调用不需要设置用户上下文，直接放行
                    return true;
                } else {
                    log.warn("内部服务调用签名验证失败: URL={}", request.getRequestURI());
                    return handleNotLogin(response);
                }
            }

            // 2. 外部请求（Gateway 转发）：从请求头获取用户上下文
            String token = extractToken(request);

            if (StringUtils.hasText(token)) {
                // 从请求头解析用户上下文（Gateway 传递）
                UserContext userContext = extractUserContextFromHeader(request, token);

                if (userContext != null) {
                    // 1. 设置到 ThreadLocal（供业务代码使用）
                    UserContext.setCurrentUser(userContext);
                    
                    // 2. 将 token 存储到请求属性中（供 PermissionService 等组件使用）
                    request.setAttribute(SecurityConstants.REQUEST_ATTR_TOKEN, token);
                    
                    // 3. 更新权限列表（如果从 Header 获取，直接存储到缓存）
                    updatePermissionsFromHeader(request, token);
                    
                    // 4. 滑动过期：检查并刷新 token（如果启用）
                    refreshTokenIfNeeded(token);
                    
                    log.debug("用户上下文已设置: userId={}, account={}, URL={}, source=gateway",
                            userContext.getUserId(), userContext.getAccount(), request.getRequestURI());
                } else {
                    // Gateway 传递的请求必须包含用户上下文，否则拒绝
                    log.warn("Gateway 请求缺少用户上下文: token={}, URL={}", token, request.getRequestURI());
                    return handleNotLogin(response);
                }
            } else {
                // 没有 token，根据安全模式处理
                return handleRequestWithoutToken(request, response, handler);
            }
        } catch (NotLoginException e) {
            // 检测到未登录异常，说明需要登录
            log.warn("检测到未登录: URL={}, message={}", request.getRequestURI(), e.getMessage());
            return handleNotLogin(response);
        } catch (Exception e) {
            log.error("设置用户上下文时发生异常，URL: {}", request.getRequestURI(), e);
        }

        return true;
    }

    /**
     * 从请求头解析用户上下文（Gateway 传递）
     * 
     * @param request HTTP 请求
     * @param token 用户 token
     * @return 用户上下文，如果解析失败或签名验证失败返回 null
     */
    private UserContext extractUserContextFromHeader(HttpServletRequest request, String token) {
        SecurityProperties.GatewaySignatureConfig signatureConfig = 
                securityProperties != null ? securityProperties.getGatewaySignature() : null;
        
        if (signatureConfig == null || !signatureConfig.isEnableContextPassing()) {
            return null;
        }

        // 1. 从请求头获取编码的用户上下文
        String encodedContext = request.getHeader(SecurityConstants.X_USER_CONTEXT_HEADER);
        if (!StringUtils.hasText(encodedContext)) {
            return null;
        }

        // 2. 解码用户上下文
        UserContext userContext = UserContextCodec.decode(encodedContext);
        if (userContext == null) {
            log.warn("解码用户上下文失败: URL={}", request.getRequestURI());
            return null;
        }

        // 3. 验证签名（如果启用）
        if (signatureConfig.isEnabled() && StringUtils.hasText(signatureConfig.getSecret())) {
            String signature = request.getHeader(SecurityConstants.X_GATEWAY_SIGNATURE_HEADER);
            String timestampStr = request.getHeader(SecurityConstants.X_GATEWAY_TIMESTAMP_HEADER);
            
            if (!StringUtils.hasText(signature) || !StringUtils.hasText(timestampStr)) {
                log.warn("缺少签名或时间戳: URL={}", request.getRequestURI());
                return null;
            }

            try {
                long timestamp = Long.parseLong(timestampStr);
                
                // 检查时间戳有效性（防止重放攻击）
                if (!GatewaySignatureUtils.isTimestampValid(timestamp, signatureConfig.getValidityWindow())) {
                    log.warn("时间戳无效或已过期: timestamp={}, URL={}", timestamp, request.getRequestURI());
                    return null;
                }
                
                // 验证签名
                boolean isValid = GatewaySignatureUtils.verifySignature(
                        signatureConfig.getSecret(),
                        token,
                        userContext.getUserId(),
                        timestamp,
                        signature
                );
                
                if (!isValid) {
                    log.warn("签名验证失败: userId={}, URL={}", userContext.getUserId(), request.getRequestURI());
                    return null;
                }
                
                log.debug("签名验证成功: userId={}, URL={}", userContext.getUserId(), request.getRequestURI());
            } catch (NumberFormatException e) {
                log.warn("时间戳格式错误: timestamp={}, URL={}", timestampStr, request.getRequestURI());
                return null;
            }
        }

        return userContext;
    }

    /**
     * 判断是否是内部服务调用
     * 
     * @param request HTTP 请求
     * @return 是否是内部服务调用
     */
    private boolean isInternalServiceCall(HttpServletRequest request) {
        String internalServiceHeader = request.getHeader(SecurityConstants.X_INTERNAL_SERVICE_HEADER);
        return StringUtils.hasText(internalServiceHeader);
    }

    /**
     * 验证内部服务调用签名
     * 
     * @param request HTTP 请求
     * @return 验证是否通过
     */
    private boolean verifyInternalServiceSignature(HttpServletRequest request) {
        SecurityProperties.InternalServiceConfig config = 
                securityProperties != null ? securityProperties.getInternalService() : null;
        
        if (config == null || !config.isEnabled()) {
            log.warn("内部服务调用验证未启用: URL={}", request.getRequestURI());
            return false;
        }

        // 1. 获取请求头
        String serviceName = request.getHeader(SecurityConstants.X_INTERNAL_SERVICE_HEADER);
        String timestampStr = request.getHeader(SecurityConstants.X_INTERNAL_TIMESTAMP_HEADER);
        String signature = request.getHeader(SecurityConstants.X_INTERNAL_SIGNATURE_HEADER);

        if (!StringUtils.hasText(serviceName) || !StringUtils.hasText(timestampStr) || 
            !StringUtils.hasText(signature)) {
            log.warn("内部服务调用缺少必要的请求头: URL={}", request.getRequestURI());
            return false;
        }

        // 2. 检查服务白名单
        java.util.Map<String, String> allowedServices = config.getAllowedServices();
        if (allowedServices != null && !allowedServices.isEmpty()) {
            if (!allowedServices.containsKey(serviceName)) {
                log.warn("内部服务调用不在白名单中: service={}, URL={}", serviceName, request.getRequestURI());
                return false;
            }
        }

        // 3. 获取服务密钥
        String secret = allowedServices != null && allowedServices.containsKey(serviceName) 
                ? allowedServices.get(serviceName) 
                : config.getSecret();
        
        if (!StringUtils.hasText(secret)) {
            log.warn("内部服务调用密钥未配置: service={}, URL={}", serviceName, request.getRequestURI());
            return false;
        }

        // 4. 验证时间戳
        try {
            long timestamp = Long.parseLong(timestampStr);
            if (!InternalSignatureUtils.isTimestampValid(timestamp, config.getValidityWindow())) {
                log.warn("内部服务调用时间戳无效或已过期: timestamp={}, URL={}", timestamp, request.getRequestURI());
                return false;
            }

            // 5. 验证签名
            boolean isValid = InternalSignatureUtils.verifySignature(secret, serviceName, timestamp, signature);
            if (!isValid) {
                log.warn("内部服务调用签名验证失败: service={}, URL={}", serviceName, request.getRequestURI());
            }
            return isValid;
        } catch (NumberFormatException e) {
            log.warn("内部服务调用时间戳格式错误: timestamp={}, URL={}", timestampStr, request.getRequestURI());
            return false;
        }
    }

    /**
     * 从请求头更新权限列表并存储到缓存
     * 
     * <p><b>注意：</b>permissions 不再存储在 UserContext 中，而是直接存储到缓存中。
     * 
     * @param request HTTP 请求
     * @param token 用户 token
     */
    private void updatePermissionsFromHeader(HttpServletRequest request, String token) {
        // 1. 检查 UserSessionService 是否可用
        if (userSessionService == null) {
            log.debug("UserSessionService 未注入，跳过从请求头更新权限列表");
            return;
        }

        // 2. 从请求头获取权限列表
        String encodedPermissions = request.getHeader(SecurityConstants.X_USER_PERMISSIONS_HEADER);
        if (!StringUtils.hasText(encodedPermissions)) {
            return;
        }

        // 3. 解析权限列表
            List<String> permissions = Arrays.asList(encodedPermissions.split(","));
        
        // 4. 获取 token 的剩余时间作为过期时间
        long expiration = 7200L; // 默认 2 小时
        try {
            long remainingTime = userSessionService.getTokenRemainingTime(token);
            if (remainingTime > 0) {
                expiration = remainingTime;
            } else {
                // 如果获取不到剩余时间，使用配置的默认过期时间
                SecurityProperties.TokenConfig tokenConfig = 
                        securityProperties != null ? securityProperties.getToken() : null;
                if (tokenConfig != null && tokenConfig.getTimeout() > 0) {
                    expiration = tokenConfig.getTimeout();
                }
            }
        } catch (Exception e) {
            log.debug("获取 token 剩余时间失败，使用默认过期时间: token={}", token, e);
        }

        // 5. 存储权限到缓存
        try {
            userSessionService.storeUserPermissions(token, permissions, expiration);
            log.debug("从请求头更新权限列表并存储到缓存: token={}, permissions={}, expiration={}s", 
                    token, permissions, expiration);
        } catch (Exception e) {
            log.error("从请求头更新权限列表失败: token={}, permissions={}", token, permissions, e);
        }
    }

    /**
     * 处理没有 token 的请求
     * 根据安全模式（DISABLED、STRICT、PERMISSIVE）决定是否允许访问
     *
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param handler 处理器
     * @return 是否允许继续处理请求
     * @throws IOException IO 异常
     */
    private boolean handleRequestWithoutToken(HttpServletRequest request, 
                                               HttpServletResponse response, 
                                               Object handler) throws IOException {
        String requestPath = request.getRequestURI();
        SecurityProperties.SecurityMode mode = securityProperties.getMode();
        
        log.debug("请求中未包含 token，URL: {}, 安全模式: {}", requestPath, mode);

        // 1. DISABLED 模式：不进行安全控制，直接放行
        if (mode == SecurityProperties.SecurityMode.DISABLED) {
            log.debug("安全模式为 DISABLED，跳过所有安全控制，允许访问: URL={}", requestPath);
            return true;
        }

        // 2. 检查是否在白名单中（如果有配置）
        if (isWhiteListPath(requestPath)) {
            log.debug("路径在白名单中，允许访问: URL={}", requestPath);
            return true;
        }

        // 3. 检查该路径是否需要认证（通过自定义注解）
        if (isLoginRequired(request, handler)) {
            log.warn("请求需要登录但未提供 token: URL={}, 安全模式: {}", requestPath, mode);
            return handleNotLogin(response);
        }

        // 4. 如果没有明确的认证要求，根据安全模式决定
        return switch (mode) {
            case STRICT -> {
                // 严格模式：默认要求登录（防止直接访问子服务绕过网关）
                log.warn("严格模式：请求未提供 token，要求登录: URL={}", requestPath);
                yield handleNotLogin(response);
            }
            case PERMISSIVE -> {
                // 宽松模式：允许匿名访问
                log.debug("宽松模式：请求未提供 token，允许匿名访问: URL={}", requestPath);
                yield true;
            }
            case DISABLED -> {
                // DISABLED 模式已在前面处理，这里不应该到达
                log.warn("安全模式为 DISABLED，但到达了不应该到达的分支: URL={}", requestPath);
                yield true;
            }
        };
    }

    /**
     * 检查请求是否需要登录
     * 通过检查 handler 方法上的自定义注解来判断
     *
     * @param request HTTP 请求
     * @param handler 处理器
     * @return 是否需要登录
     */
    private boolean isLoginRequired(HttpServletRequest request, Object handler) {
        // 如果不是 HandlerMethod，无法检查注解
        // 在严格模式下，默认要求登录（防止绕过网关）
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            log.debug("Handler 不是 HandlerMethod，无法检查注解: handler={}, URL={}",
                    handler != null ? handler.getClass().getName() : "null", request.getRequestURI());
            // 在严格模式下，默认要求登录
            return securityProperties.getMode() == SecurityProperties.SecurityMode.STRICT;
        }

        Method method = handlerMethod.getMethod();
        Class<?> beanType = handlerMethod.getBeanType();

        // 检查方法上是否有自定义认证注解
        RequireLogin methodLogin = AnnotationUtils.findAnnotation(method, RequireLogin.class);
        RequirePermission methodPermission = AnnotationUtils.findAnnotation(method, RequirePermission.class);
        RequireRole methodRole = AnnotationUtils.findAnnotation(method, RequireRole.class);

        if (methodLogin != null || methodPermission != null || methodRole != null) {
            // 方法上有认证注解，需要登录
            log.debug("方法上有自定义认证注解，需要登录: method={}, URL={}",
                    method.getName(), request.getRequestURI());
            return true;
        }

        // 检查类上是否有自定义认证注解
        RequireLogin classLogin = AnnotationUtils.findAnnotation(beanType, RequireLogin.class);
        RequirePermission classPermission = AnnotationUtils.findAnnotation(beanType, RequirePermission.class);
        RequireRole classRole = AnnotationUtils.findAnnotation(beanType, RequireRole.class);

        if (classLogin != null || classPermission != null || classRole != null) {
            // 类上有认证注解，需要登录
            log.debug("类上有自定义认证注解，需要登录: class={}, URL={}",
                    beanType.getName(), request.getRequestURI());
            return true;
        }

        // 如果没有找到认证注解，根据安全模式决定
        log.debug("未找到认证注解，由安全模式决定: URL={}", request.getRequestURI());
        return false;
    }

    /**
     * 处理未登录情况，返回统一的错误响应
     *
     * @param response HTTP 响应
     * @return false，表示拦截请求
     */
    private boolean handleNotLogin(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // 使用统一的 Result 格式返回错误
        Result<Void> errorResult = Result.error(SecurityError.TOKEN_MISSING);
        String errorResponse = JsonUtils.toJsonString(errorResult);

        response.getWriter().write(errorResponse);
        response.getWriter().flush();

        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理 ThreadLocal，防止内存泄漏
        UserContext.clearCurrentUser();
    }

    /**
     * 检查路径是否在白名单中
     * 从 SecurityProperties 中读取白名单配置
     *
     * @param path 请求路径
     * @return 是否在白名单中
     */
    private boolean isWhiteListPath(String path) {
        // 如果白名单配置为 null，直接返回 false
        if (securityProperties.getWhiteList() == null) {
            return false;
        }

        // 如果白名单未启用，直接返回 false
        if (!securityProperties.getWhiteList().isEnabled()) {
            return false;
        }

        // 获取所有白名单路径（默认路径 + 配置路径）
        List<String> whiteListPaths = securityProperties.getWhiteList().getAllPaths();

        // 如果白名单路径为空，返回 false
        if (CollUtil.isEmpty(whiteListPaths)) {
            return false;
        }

        // 检查路径是否匹配任何白名单模式
        boolean matched = whiteListPaths.stream()
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, path));

        if (matched) {
            log.debug("路径匹配白名单: path={}, pattern={}", path,
                    whiteListPaths.stream()
                            .filter(pattern -> PATH_MATCHER.match(pattern, path))
                            .findFirst()
                            .orElse(""));
        }

        return matched;
    }

    /**
     * 从请求中提取 token
     * 使用 TokenExtractor 工具类统一提取
     */
    private String extractToken(HttpServletRequest request) {
        return tokenExtractor.extractToken(request);
    }

    /**
     * 滑动过期：检查并刷新 token（如果启用且需要刷新）
     * 
     * <p>刷新策略：
     * <ul>
     *   <li>如果 token 剩余时间少于刷新阈值，则自动刷新 token</li>
     *   <li>刷新时将 token 过期时间延长到配置的续期时长</li>
     *   <li>这样可以确保活跃用户的会话不会过期，同时避免长期不活跃的会话占用资源</li>
     * </ul>
     * 
     * @param token 用户 token
     */
    private void refreshTokenIfNeeded(String token) {
        // 1. 检查是否启用滑动过期
        SecurityProperties.TokenConfig tokenConfig = securityProperties != null ? securityProperties.getToken() : null;
        if (tokenConfig == null || !tokenConfig.isEnableSlidingExpiration()) {
            return;
        }

        // 2. 检查 UserSessionService 是否可用
        if (userSessionService == null) {
            log.debug("UserSessionService 未注入，跳过滑动过期刷新: token={}", token);
            return;
        }

        try {
            // 3. 获取 token 剩余时间（单位：秒）
            long remainingTime = userSessionService.getTokenRemainingTime(token);
            
            // 4. 如果剩余时间小于 0（token 不存在或已过期），不进行刷新
            if (remainingTime < 0) {
                log.debug("Token 不存在或已过期，跳过滑动过期刷新: token={}, remainingTime={}s", token, remainingTime);
                return;
            }

            // 5. 检查是否需要刷新（剩余时间少于刷新阈值）
            long refreshThreshold = tokenConfig.getRefreshThreshold();
            log.debug("检查 Token 是否需要刷新: token={}, remainingTime={}s, refreshThreshold={}s", 
                    token, remainingTime, refreshThreshold);
            
            if (remainingTime < refreshThreshold) {
                // 6. 刷新 token（延长到续期时长）
                long renewalDuration = tokenConfig.getRenewalDuration();
                log.info("Token 剩余时间少于阈值，开始刷新: token={}, remainingTime={}s, renewalDuration={}s", 
                        token, remainingTime, renewalDuration);
                
                boolean success = userSessionService.renewToken(token, renewalDuration);
                
                if (success) {
                    log.info("Token 滑动过期刷新成功: token={}, remainingTime={}s, renewedTo={}s", 
                            token, remainingTime, renewalDuration);
                } else {
                    log.warn("Token 滑动过期刷新失败: token={}, remainingTime={}s", token, remainingTime);
                }
            } else {
                log.debug("Token 剩余时间充足，无需刷新: token={}, remainingTime={}s, threshold={}s", 
                        token, remainingTime, refreshThreshold);
            }
        } catch (Exception e) {
            log.error("滑动过期刷新 token 时发生异常: token={}", token, e);
        }
    }
} 
