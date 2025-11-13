package com.indigo.security.utils;

import com.indigo.security.config.SecurityProperties;
import org.springframework.util.StringUtils;

/**
 * Token 配置辅助工具类
 * 提供便捷方法获取 Token 相关配置，支持默认值降级
 * 
 * @author 史偕成
 * @date 2025/01/10
 */
public class TokenConfigHelper {

    // 默认值常量
    private static final String DEFAULT_PREFIX = "Bearer ";
    private static final String DEFAULT_QUERY_PARAM = "token";
    private static final String DEFAULT_HEADER_NAME = "Authorization";
    private static final String DEFAULT_X_AUTH_TOKEN_HEADER = "X-Auth-Token";

    /**
     * 获取 Token 前缀
     * 
     * @param securityProperties 安全配置属性
     * @return Token 前缀，如果配置为空则返回默认值 "Bearer "
     */
    public static String getTokenPrefix(SecurityProperties securityProperties) {
        if (securityProperties == null || securityProperties.getToken() == null) {
            return DEFAULT_PREFIX;
        }
        String prefix = securityProperties.getToken().getPrefix();
        return StringUtils.hasText(prefix) ? prefix : DEFAULT_PREFIX;
    }

    /**
     * 获取 Token 前缀长度
     * 
     * @param securityProperties 安全配置属性
     * @return Token 前缀长度
     */
    public static int getTokenPrefixLength(SecurityProperties securityProperties) {
        return getTokenPrefix(securityProperties).length();
    }

    /**
     * 获取 Token 查询参数名
     * 
     * @param securityProperties 安全配置属性
     * @return Token 查询参数名，如果配置为空则返回默认值 "token"
     */
    public static String getTokenQueryParam(SecurityProperties securityProperties) {
        if (securityProperties == null || securityProperties.getToken() == null) {
            return DEFAULT_QUERY_PARAM;
        }
        String queryParam = securityProperties.getToken().getQueryParam();
        return StringUtils.hasText(queryParam) ? queryParam : DEFAULT_QUERY_PARAM;
    }

    /**
     * 获取 Authorization 请求头名称
     * 
     * @param securityProperties 安全配置属性
     * @return 请求头名称，如果配置为空则返回默认值 "Authorization"
     */
    public static String getTokenHeaderName(SecurityProperties securityProperties) {
        if (securityProperties == null || securityProperties.getToken() == null) {
            return DEFAULT_HEADER_NAME;
        }
        String headerName = securityProperties.getToken().getHeaderName();
        return StringUtils.hasText(headerName) ? headerName : DEFAULT_HEADER_NAME;
    }

    /**
     * 获取 X-Auth-Token 请求头名称
     * 
     * @param securityProperties 安全配置属性
     * @return 请求头名称，如果配置为空则返回默认值 "X-Auth-Token"
     */
    public static String getXAuthTokenHeader(SecurityProperties securityProperties) {
        if (securityProperties == null || securityProperties.getToken() == null) {
            return DEFAULT_X_AUTH_TOKEN_HEADER;
        }
        String xAuthTokenHeader = securityProperties.getToken().getXAuthTokenHeader();
        return StringUtils.hasText(xAuthTokenHeader) ? xAuthTokenHeader : DEFAULT_X_AUTH_TOKEN_HEADER;
    }
}

