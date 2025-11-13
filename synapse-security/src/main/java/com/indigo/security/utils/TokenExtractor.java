package com.indigo.security.utils;

import com.indigo.security.config.SecurityProperties;
import com.indigo.security.constants.SecurityConstants;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * Token 提取工具类
 * 提供统一的方法从 HTTP 请求中提取 token
 * 
 * 支持多种提取方式（按优先级）：
 * 1. 从请求属性中获取（UserContextInterceptor 设置的）
 * 2. 从 Authorization 请求头获取（支持自定义前缀）
 * 3. 从自定义请求头获取（X-Auth-Token）
 * 4. 从查询参数获取（token）
 * 
 * <p><b>注意：</b>此工具类只在 WebMVC 环境中使用，通过 {@link WebMvcSecurityConfig} 中的 @Bean 方法创建。
 * 不在 WebFlux 环境中加载，避免 Servlet API 依赖问题。
 * 
 * @author 史偕成
 * @date 2025/01/10
 */
@RequiredArgsConstructor
public class TokenExtractor {

    private final SecurityProperties securityProperties;

    /**
     * 从 HttpServletRequest 中提取 token
     * 
     * @param request HTTP 请求
     * @return token 字符串，如果获取不到则返回 null
     */
    public String extractToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        // 获取配置值
        String tokenHeaderName = TokenConfigHelper.getTokenHeaderName(securityProperties);
        String tokenPrefix = TokenConfigHelper.getTokenPrefix(securityProperties);
        int prefixLength = TokenConfigHelper.getTokenPrefixLength(securityProperties);
        String xAuthTokenHeader = TokenConfigHelper.getXAuthTokenHeader(securityProperties);
        String tokenQueryParam = TokenConfigHelper.getTokenQueryParam(securityProperties);

        // 1. 优先从请求属性中获取（UserContextInterceptor 设置的）
        Object tokenObj = request.getAttribute(SecurityConstants.REQUEST_ATTR_TOKEN);
        if (tokenObj instanceof String token && StringUtils.hasText(token)) {
            return token.trim();
        }

        // 2. 从请求头中获取（Authorization Bearer）
        String authHeader = request.getHeader(tokenHeaderName);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(tokenPrefix)) {
            return authHeader.substring(prefixLength).trim();
        }

        // 3. 从自定义请求头获取（X-Auth-Token）
        String tokenHeader = request.getHeader(xAuthTokenHeader);
        if (StringUtils.hasText(tokenHeader)) {
            return tokenHeader.trim();
        }

        // 4. 从查询参数中获取
        String tokenParam = request.getParameter(tokenQueryParam);
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam.trim();
        }

        return null;
    }
}

