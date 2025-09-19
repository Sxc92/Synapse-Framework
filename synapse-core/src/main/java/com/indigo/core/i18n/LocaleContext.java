package com.indigo.core.i18n;

import java.util.Locale;

/**
 * 语言环境上下文持有者接口
 * 定义在core模块中，避免core依赖i18n模块
 * 
 * @author 史偕成
 * @date 2025/01/27
 */
public interface LocaleContext {
    
    /**
     * 获取当前语言环境
     * 
     * @return 当前语言环境
     */
    Locale getCurrentLocale();
    
    /**
     * 设置当前语言环境
     * 
     * @param locale 语言环境
     */
    void setCurrentLocale(Locale locale);
    
    /**
     * 清除当前语言环境
     */
    void clearCurrentLocale();
}
