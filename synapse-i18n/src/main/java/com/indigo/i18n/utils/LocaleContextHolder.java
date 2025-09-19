package com.indigo.i18n.utils;

import com.indigo.i18n.resolver.HeaderLocaleResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 语言环境上下文持有者
 * 用于获取当前请求的语言环境信息
 * 优先使用HeaderLocaleResolver，与Spring的LocaleResolver集成
 * 
 * @author 史偕成
 * @date 2025/01/27
 */
@Slf4j
public class LocaleContextHolder {
    
    /**
     * 语言环境ThreadLocal
     */
    private static final ThreadLocal<Locale> LOCALE_THREAD_LOCAL = new ThreadLocal<>();
    
    /**
     * 基于请求头的语言环境解析器
     */
    private static HeaderLocaleResolver headerLocaleResolver;
    
    /**
     * 设置HeaderLocaleResolver实例
     */
    public static void setHeaderLocaleResolver(HeaderLocaleResolver resolver) {
        LocaleContextHolder.headerLocaleResolver = resolver;
    }
    
    /**
     * 获取当前语言环境
     * 优先级：ThreadLocal > HeaderLocaleResolver > Spring LocaleResolver > 系统默认
     * 
     * @return 当前语言环境
     */
    public static Locale getCurrentLocale() {
        // 1. 优先从ThreadLocal获取
        Locale locale = LOCALE_THREAD_LOCAL.get();
        if (locale != null) {
            return locale;
        }
        
        // 2. 从HeaderLocaleResolver获取（主要方式）
        if (headerLocaleResolver != null) {
            try {
                locale = headerLocaleResolver.resolveLocale();
                if (locale != null) {
                    return locale;
                }
            } catch (Exception e) {
                log.debug("Failed to resolve locale from HeaderLocaleResolver", e);
            }
        }
        
        // 3. 从Spring LocaleResolver获取（备用方式）
        locale = getLocaleFromSpringResolver();
        if (locale != null) {
            return locale;
        }
        
        // 4. 使用系统默认语言环境
        return Locale.getDefault();
    }
    
    /**
     * 设置当前语言环境到ThreadLocal
     * 
     * @param locale 语言环境
     */
    public static void setCurrentLocale(Locale locale) {
        LOCALE_THREAD_LOCAL.set(locale);
    }
    
    /**
     * 清除ThreadLocal中的语言环境
     */
    public static void clearCurrentLocale() {
        LOCALE_THREAD_LOCAL.remove();
    }
    
    /**
     * 从Spring LocaleResolver获取语言环境
     * 
     * @return 语言环境，如果无法获取则返回null
     */
    private static Locale getLocaleFromSpringResolver() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            
            HttpServletRequest request = attributes.getRequest();
            if (request == null) {
                return null;
            }
            
            LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
            if (localeResolver != null) {
                return localeResolver.resolveLocale(request);
            }
            
            return null;
            
        } catch (Exception e) {
            log.debug("从Spring LocaleResolver获取语言环境失败", e);
            return null;
        }
    }
    
    /**
     * 检查是否支持指定的语言环境
     * 
     * @param locale 语言环境
     * @return 是否支持
     */
    public static boolean isSupportedLocale(Locale locale) {
        if (locale == null) {
            return false;
        }
        
        // 这里可以根据实际需求定义支持的语言环境列表
        // 例如：只支持中文和英文
        String language = locale.getLanguage();
        return "zh".equals(language) || "en".equals(language);
    }
    
    /**
     * 获取支持的语言环境列表
     * 
     * @return 支持的语言环境列表
     */
    public static Locale[] getSupportedLocales() {
        return new Locale[]{
            Locale.SIMPLIFIED_CHINESE,  // zh_CN
            Locale.TRADITIONAL_CHINESE, // zh_TW
            Locale.ENGLISH,             // en
            Locale.US                   // en_US
        };
    }
}