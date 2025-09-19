package com.indigo.i18n.resolver;

import com.indigo.i18n.config.I18nProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;

/**
 * 基于请求头的语言环境解析器
 * 只支持请求头方式获取语言环境，请求头key可配置
 * 
 * @author 史偕成
 * @date 2025/01/27
 */
@Slf4j
public class HeaderLocaleResolver {
    
    @Autowired
    private I18nProperties i18nProperties;
    
    /**
     * 解析当前请求的语言环境
     * 优先级：配置的请求头 > Accept-Language头 > 默认语言环境
     * 
     * @return 语言环境
     */
    public Locale resolveLocale() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return i18nProperties.getDefaultLocale();
            }
            
            HttpServletRequest request = attributes.getRequest();
            if (request == null) {
                return i18nProperties.getDefaultLocale();
            }
            
            // 1. 优先从配置的请求头获取
            String localeHeader = request.getHeader(i18nProperties.getHeader().getLocaleHeaderName());
            if (localeHeader != null && !localeHeader.isEmpty()) {
                Locale locale = parseLocale(localeHeader);
                if (locale != null) {
                    return locale;
                }
            }
            
            // 2. 从备用的语言环境请求头获取
            String languageHeader = request.getHeader(i18nProperties.getHeader().getLanguageHeaderName());
            if (languageHeader != null && !languageHeader.isEmpty()) {
                Locale locale = parseLocale(languageHeader);
                if (locale != null) {
                    return locale;
                }
            }
            
            // 3. 如果启用了Accept-Language头解析
            if (i18nProperties.getHeader().isEnableAcceptLanguage()) {
                Locale requestLocale = request.getLocale();
                if (requestLocale != null) {
                    return requestLocale;
                }
            }
            
            // 4. 使用默认语言环境
            return i18nProperties.getDefaultLocale();
            
        } catch (Exception e) {
            log.warn("Failed to resolve locale from request headers", e);
            return i18nProperties.getDefaultLocale();
        }
    }
    
    /**
     * 解析语言环境字符串
     * 支持格式：zh_CN, zh-CN, zh, en_US, en-US, en
     * 
     * @param localeStr 语言环境字符串
     * @return 语言环境对象
     */
    private Locale parseLocale(String localeStr) {
        if (localeStr == null || localeStr.isEmpty()) {
            return null;
        }
        
        try {
            // 处理下划线和连字符
            String normalized = localeStr.replace('-', '_');
            String[] parts = normalized.split("_");
            
            if (parts.length == 1) {
                // 只有语言代码，如：zh, en
                return new Locale(parts[0]);
            } else if (parts.length == 2) {
                // 语言代码和国家代码，如：zh_CN, en_US
                return new Locale(parts[0], parts[1]);
            } else if (parts.length == 3) {
                // 语言代码、国家代码和变体，如：zh_CN_Hans
                return new Locale(parts[0], parts[1], parts[2]);
            }
            
            return null;
            
        } catch (Exception e) {
            log.warn("Failed to parse locale string: {}", localeStr, e);
            return null;
        }
    }
}
