package com.indigo.core.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

/**
 * BeanPostProcessor 警告过滤器
 * 过滤 Spring 框架的 BeanPostProcessor 相关警告，这些警告不影响功能
 * 
 * <p><b>过滤的警告类型：</b>
 * <ul>
 *   <li>"BeanPostProcessorChecker" 相关的警告</li>
 *   <li>"not eligible for getting processed by all BeanPostProcessors" 警告</li>
 * </ul>
 * 
 * <p><b>为什么需要过滤：</b>
 * 这些警告是因为 BeanFactoryPostProcessor 在 BeanPostProcessor 之前执行导致的，
 * 属于 Spring 框架的正常行为，不影响实际功能。
 * 
 * @author 史偕成
 * @date 2025/11/13
 */
public class BeanPostProcessorWarningFilter extends TurboFilter {
    
    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        // 只过滤 WARN 级别的日志
        if (level != Level.WARN) {
            return FilterReply.NEUTRAL;
        }
        
        // 检查是否是 PostProcessorRegistrationDelegate 相关的日志
        String loggerName = logger.getName();
        if (loggerName != null && loggerName.contains("PostProcessorRegistrationDelegate")) {
            // 检查日志消息是否包含 BeanPostProcessorChecker 相关的内容
            if (format != null && (
                format.contains("BeanPostProcessorChecker") ||
                format.contains("not eligible for getting processed by all BeanPostProcessors")
            )) {
                // 过滤掉这些警告
                return FilterReply.DENY;
            }
        }
        
        return FilterReply.NEUTRAL;
    }
}

