package com.indigo.i18n.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;

/**
 * 国际化配置属性
 * 
 * @author 史偕成
 * @date 2025/01/27
 */
@Data
@ConfigurationProperties(prefix = "synapse.i18n")
public class I18nProperties {
    
    /**
     * 是否启用国际化功能
     */
    private boolean enabled = true;
    
    /**
     * 默认语言环境
     */
    private Locale defaultLocale = Locale.SIMPLIFIED_CHINESE;
    
    /**
     * 语言环境请求头配置
     */
    private Header header = new Header();
    
    /**
     * 语言环境请求头配置
     */
    @Data
    public static class Header {
        /**
         * 语言环境请求头名称
         */
        private String localeHeaderName = "X-Locale";
        
        /**
         * 语言环境请求头名称（备用）
         */
        private String languageHeaderName = "X-Language";
        
        /**
         * 是否启用Accept-Language头解析
         */
        private boolean enableAcceptLanguage = true;
    }
}
