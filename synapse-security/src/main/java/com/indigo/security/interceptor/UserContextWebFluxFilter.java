package com.indigo.security.interceptor;

import com.indigo.cache.session.UserSessionService;
import com.indigo.core.context.UserContext;
import com.indigo.security.config.SecurityProperties;
import com.indigo.security.utils.TokenConfigHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 用户上下文WebFlux过滤器
 * 用于在 WebFlux 应用中从 token 获取用户信息，并设置到响应式上下文
 * 
 * <p><b>使用场景：</b>
 * <ul>
 *   <li>仅当下游服务是 WebFlux 应用时需要使用此过滤器</li>
 *   <li>如果下游服务是 Servlet 应用，请使用 {@link UserContextInterceptor}</li>
 *   <li>Gateway 是 WebFlux 应用，但 Gateway 本身不需要此过滤器（Gateway 使用 TokenAuthFilter）</li>
 * </ul>
 * 
 * <p><b>工作流程：</b>
 * <ol>
 *   <li>从请求头或查询参数中提取 token（Authorization Bearer、X-Auth-Token 或查询参数）</li>
 *   <li>使用 UserSessionService 从 Redis 获取用户上下文</li>
 *   <li>将用户上下文设置到响应式上下文中，供业务代码使用</li>
 * </ol>
 * 
 * <p><b>注意：</b>
 * <ul>
 *   <li>Gateway 只传递 token，不传递用户信息，下游服务统一从 token 获取</li>
 *   <li>此过滤器只负责设置用户上下文，不进行权限检查</li>
 *   <li>权限检查请使用Sa-Token的注解：@SaCheckLogin、@SaCheckPermission、@SaCheckRole</li>
 * </ul>
 *
 * @author 史偕成
 * @date 2025/03/21
 */
@Slf4j
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnBean(UserSessionService.class)
public class UserContextWebFluxFilter implements WebFilter {

    private final UserSessionService userSessionService;
    private final SecurityProperties securityProperties;

    public UserContextWebFluxFilter(UserSessionService userSessionService,
                                    SecurityProperties securityProperties) {
        this.userSessionService = userSessionService;
        this.securityProperties = securityProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        try {
            // 从请求中提取 token
            String token = extractToken(exchange);
            
            if (StringUtils.hasText(token)) {
                // 从 Redis 获取用户上下文
                UserContext userContext = userSessionService.getUserSession(token);
                
                if (userContext != null) {
                    // 设置到响应式上下文和 ThreadLocal（兼容性）
                    UserContext.setCurrentUser(userContext);
                    
                    return chain.filter(exchange)
                            .contextWrite(context -> context.put(UserContext.class, userContext))
                            .doFinally(signalType -> {
                                // 清理上下文，防止内存泄漏
                                UserContext.clearCurrentUser();
                            });
                } else {
                    log.debug("未找到用户上下文，token: {}, URL: {}", token, exchange.getRequest().getURI());
                }
            } else {
                log.debug("请求中未包含 token，URL: {}", exchange.getRequest().getURI());
            }
        } catch (Exception e) {
            log.error("设置用户上下文时发生异常，URL: {}", exchange.getRequest().getURI(), e);
        }
        
        return chain.filter(exchange);
    }

    /**
     * 从请求中提取 token
     * 优先级：Authorization Bearer > X-Auth-Token > 查询参数 token
     */
    private String extractToken(ServerWebExchange exchange) {
        // 获取配置值
        String tokenHeaderName = TokenConfigHelper.getTokenHeaderName(securityProperties);
        String tokenPrefix = TokenConfigHelper.getTokenPrefix(securityProperties);
        int prefixLength = TokenConfigHelper.getTokenPrefixLength(securityProperties);
        String xAuthTokenHeader = TokenConfigHelper.getXAuthTokenHeader(securityProperties);
        String tokenQueryParam = TokenConfigHelper.getTokenQueryParam(securityProperties);

        // 1. 优先从 Authorization 头获取
        String authHeader = exchange.getRequest().getHeaders().getFirst(tokenHeaderName);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(tokenPrefix)) {
            return authHeader.substring(prefixLength).trim();
        }
        
        // 2. 从自定义头获取
        String token = exchange.getRequest().getHeaders().getFirst(xAuthTokenHeader);
        if (StringUtils.hasText(token)) {
            return token.trim();
        }
        
        // 3. 从查询参数获取
        token = exchange.getRequest().getQueryParams().getFirst(tokenQueryParam);
        if (StringUtils.hasText(token)) {
            return token.trim();
        }
        
        return null;
    }
} 