package com.indigo.core.exception.handler;

import com.indigo.core.entity.Result;
import com.indigo.core.exception.*;
import com.indigo.core.exception.enums.ErrorCode;
import com.indigo.i18n.resolver.I18nMessageResolver;
import com.indigo.i18n.utils.LocaleContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;

/**
 * WebMVC 全局异常处理器
 * 处理传统 Servlet 环境下的所有异常
 * 使用I18n模块获取国际化消息
 *
 * @author 史偕成
 * @date 2025/12/19
 **/
@Slf4j
@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WebMvcGlobalExceptionHandler {

    @Autowired
    private I18nMessageResolver i18nMessageResolver;

    @ExceptionHandler(I18nException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleI18nException(I18nException e) {
        log.error("I18nException: code={}, message={}", e.getCode(), e.getMessage(), e);
        
        // 获取异常指定的语言环境
        Locale locale = e.getLocale() != null ? e.getLocale() : LocaleContextHolder.getCurrentLocale();
        
        // 使用I18n模块解析消息
        String message = i18nMessageResolver.resolveMessage(e.getCode(), locale, e.getArgs());
        
        return Result.error(e.getCode(), message);
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: code={}, message={}", e.getCode(), e.getMessage());
        
        // 获取当前语言环境
        Locale locale = LocaleContextHolder.getCurrentLocale();
        
        // 使用I18n模块解析消息
        String message = i18nMessageResolver.resolveMessage(e.getCode(), locale, e.getArgs());
        
        return Result.error(e.getCode(), message);
    }

    @ExceptionHandler(IAMException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleIAMException(IAMException e) {
        log.error("IAMException: code={}, message={}", e.getCode(), e.getMessage(), e);
        
        Locale locale = LocaleContextHolder.getCurrentLocale();
        String message = i18nMessageResolver.resolveMessage(e.getCode(), locale, e.getArgs());
        
        return Result.error(e.getCode(), message);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleIllegalStateException(IllegalStateException e) {
        log.warn("IllegalStateException: message={}", e.getMessage());
        
        Locale locale = LocaleContextHolder.getCurrentLocale();
        String message = i18nMessageResolver.resolveMessage(ErrorCode.ILLEGAL_STATE.getCode(), locale);
        
        return Result.error(ErrorCode.ILLEGAL_STATE.getCode(), message);
    }

    @ExceptionHandler(AssertException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleAssertException(AssertException e) {
        log.warn("AssertException: code={}, message={}", e.getCode(), e.getMessage());
        
        Locale locale = LocaleContextHolder.getCurrentLocale();
        String message = i18nMessageResolver.resolveMessage(e.getCode(), locale, e.getArgs());
        
        return Result.error(e.getCode(), message);
    }

    @ExceptionHandler(RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Result<?> handleRateLimitException(RateLimitException e) {
        log.warn("RateLimitException: message={}", e.getMessage());
        
        Locale locale = LocaleContextHolder.getCurrentLocale();
        String message = i18nMessageResolver.resolveMessage(ErrorCode.RATE_LIMIT_EXCEEDED.getCode(), locale);
        
        return Result.error(ErrorCode.RATE_LIMIT_EXCEEDED.getCode(), message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e) {
        log.error("Unexpected error occurred", e);
        
        Locale locale = LocaleContextHolder.getCurrentLocale();
        String message = i18nMessageResolver.resolveMessage(ErrorCode.BASE_ERROR.getCode(), locale);
        
        return Result.error(ErrorCode.BASE_ERROR.getCode(), message);
    }
}