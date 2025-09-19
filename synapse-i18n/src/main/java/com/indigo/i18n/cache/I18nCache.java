package com.indigo.i18n.cache;

import com.indigo.cache.core.CacheService;
import com.indigo.cache.model.CacheObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 国际化缓存管理
 * 使用synapse-cache模块的CacheService管理Redis中的国际化消息
 * 强制使用cache模块，缓存配置不可配置
 * 
 * @author 史偕成
 * @date 2025/01/27
 */
@Slf4j
@Component
public class I18nCache {
    
    @Autowired
    private CacheService cacheService;
    
    // 固定的缓存配置，不可配置
    private static final String MESSAGE_KEY_PREFIX = "i18n:messages";
    private static final String LOCALES_KEY = "i18n:locales";
    private static final long CACHE_TTL = 24 * 60 * 60; // 24小时
    
    /**
     * 获取消息模板
     */
    public String getMessageTemplate(String errorCode, Locale locale) {
        try {
            String cacheKey = cacheService.generateKey(MESSAGE_KEY_PREFIX, errorCode, locale.toString());
            Optional<String> template = cacheService.getData(cacheKey);
            return template.orElse(null);
        } catch (Exception e) {
            log.warn("Failed to get message template from Redis for code: {}, locale: {}", errorCode, locale, e);
            return null;
        }
    }
    
    /**
     * 设置消息模板
     */
    public void setMessageTemplate(String errorCode, Locale locale, String template) {
        try {
            String cacheKey = cacheService.generateKey(MESSAGE_KEY_PREFIX, errorCode, locale.toString());
            CacheObject<String> cacheObject = new CacheObject<>(cacheKey, template, CACHE_TTL);
            cacheService.save(cacheObject);
        } catch (Exception e) {
            log.warn("Failed to set message template to Redis for code: {}, locale: {}", errorCode, locale, e);
        }
    }
    
    /**
     * 删除消息模板
     */
    public void deleteMessageTemplate(String errorCode, Locale locale) {
        try {
            String cacheKey = cacheService.generateKey(MESSAGE_KEY_PREFIX, errorCode, locale.toString());
            cacheService.delete(cacheKey);
        } catch (Exception e) {
            log.warn("Failed to delete message template from Redis for code: {}, locale: {}", errorCode, locale, e);
        }
    }
    
    /**
     * 检查语言是否支持
     */
    public boolean isLocaleSupported(Locale locale) {
        try {
            List<Locale> supportedLocales = getSupportedLocales();
            return supportedLocales.contains(locale);
        } catch (Exception e) {
            log.warn("Failed to check locale support for: {}", locale, e);
            return false;
        }
    }
    
    /**
     * 获取支持的语言列表
     */
    public List<Locale> getSupportedLocales() {
        try {
            Optional<String> localesStr = cacheService.getData(LOCALES_KEY);
            if (localesStr.isPresent()) {
                return java.util.Arrays.stream(localesStr.get().split(","))
                    .map(Locale::forLanguageTag)
                    .collect(java.util.stream.Collectors.toList());
            }
            return List.of(Locale.SIMPLIFIED_CHINESE, Locale.ENGLISH);
        } catch (Exception e) {
            log.warn("Failed to get supported locales from Redis", e);
            return List.of(Locale.SIMPLIFIED_CHINESE, Locale.ENGLISH);
        }
    }
    
    /**
     * 设置支持的语言列表
     */
    public void setSupportedLocales(List<Locale> locales) {
        try {
            String localesStr = locales.stream()
                .map(Locale::toString)
                .collect(java.util.stream.Collectors.joining(","));
            CacheObject<String> cacheObject = new CacheObject<>(LOCALES_KEY, localesStr, CACHE_TTL);
            cacheService.save(cacheObject);
        } catch (Exception e) {
            log.warn("Failed to set supported locales to Redis", e);
        }
    }
}
