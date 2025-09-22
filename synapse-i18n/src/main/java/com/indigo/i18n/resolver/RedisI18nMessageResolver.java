package com.indigo.i18n.resolver;

import com.indigo.i18n.cache.I18nCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

/**
 * Redis国际化消息解析器
 * 从Redis获取国际化消息
 * 
 * @author 史偕成
 * @date 2025/01/27
 */
@Slf4j
@Component
public class RedisI18nMessageResolver implements I18nMessageResolver {
    
    @Autowired
    private I18nCache i18nCache;
    
    @Override
    public String resolveMessage(String code, Locale locale, Object... args) {
        try {
            // 1. 从Redis获取消息模板
            String template = i18nCache.getMessageTemplate(code, locale);
            
            if (template != null) {
                // 2. 格式化消息
                return formatMessage(template, args);
            }
            
            // 3. 回退到消息代码
            return code;
        } catch (Exception e) {
            log.warn("Failed to resolve message for code: {}, locale: {}", code, locale, e);
            return code;
        }
    }
    
    @Override
    public String resolveMessage(String code, Object... args) {
        // 使用默认语言环境
        Locale locale = Locale.getDefault();
        return resolveMessage(code, locale, args);
    }
    
    @Override
    public boolean isLocaleSupported(Locale locale) {
        try {
            return i18nCache.isLocaleSupported(locale);
        } catch (Exception e) {
            log.warn("Failed to check locale support for: {}", locale, e);
            return false;
        }
    }
    
    @Override
    public List<Locale> getSupportedLocales() {
        try {
            return i18nCache.getSupportedLocales();
        } catch (Exception e) {
            log.warn("Failed to get supported locales", e);
            return java.util.List.of(Locale.SIMPLIFIED_CHINESE, Locale.ENGLISH);
        }
    }
    
    /**
     * 格式化消息
     */
    private String formatMessage(String template, Object[] args) {
        if (args == null || args.length == 0) {
            return template;
        }
        
        try {
            return MessageFormat.format(template, args);
        } catch (Exception e) {
            log.warn("Failed to format message template: {}", template, e);
            return template;
        }
    }
}
