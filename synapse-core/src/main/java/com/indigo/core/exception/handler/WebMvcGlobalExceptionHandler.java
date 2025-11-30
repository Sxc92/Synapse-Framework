package com.indigo.core.exception.handler;

import com.indigo.core.entity.Result;
import com.indigo.core.exception.SynapseException;
import com.indigo.core.constants.StandardErrorCode;
import com.indigo.core.i18n.MessageResolver;
import com.indigo.core.i18n.LocaleContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Locale;
import java.util.Set;

/**
 * WebMVC 全局异常处理器
 * 处理传统 Servlet 环境下的所有异常
 * 统一使用 SynapseException 处理所有异常类型
 *
 * @author 史偕成
 * @date 2025/12/19
 **/
@Slf4j
@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WebMvcGlobalExceptionHandler {

    @Autowired(required = false)
    private MessageResolver messageResolver;
    
    @Autowired(required = false)
    private LocaleContext localeContext;

    @ExceptionHandler(SynapseException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleSynapseException(SynapseException e) {
        log.error("SynapseException: code={}, message={}", e.getCode(), e.getMessage(), e);
        
        String message;
        String customMessage = e.getMessage();
        String errorCode = e.getCode();
        
        // 如果异常消息不等于错误码，说明有自定义消息，优先使用自定义消息
        if (customMessage != null && !customMessage.equals(errorCode)) {
            message = customMessage;
        } else {
            // 否则使用国际化消息
            Locale locale = getCurrentLocale();
            String messageKey = getMessageKey(errorCode);
            message = resolveMessage(messageKey, locale, e.getArgs());
        }
        
        return Result.error(e.getCode(), message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("参数校验失败: {}", e.getMessage());
        
        StringBuilder errorMessage = new StringBuilder();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errorMessage.append(fieldError.getField())
                       .append(": ")
                       .append(fieldError.getDefaultMessage())
                       .append("; ");
        }
        
        return Result.error("VALIDATION_ERROR", errorMessage.toString());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("约束校验失败: {}", e.getMessage());
        
        StringBuilder errorMessage = new StringBuilder();
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        for (ConstraintViolation<?> violation : violations) {
            errorMessage.append(violation.getPropertyPath())
                       .append(": ")
                       .append(violation.getMessage())
                       .append("; ");
        }
        
        return Result.error("VALIDATION_ERROR", errorMessage.toString());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleIllegalStateException(IllegalStateException e) {
        log.warn("IllegalStateException: message={}", e.getMessage());
        
        Locale locale = getCurrentLocale();
        String messageKey = getMessageKey(StandardErrorCode.ILLEGAL_STATE.getCode());
        String message = resolveMessage(messageKey, locale);
        
        return Result.error(StandardErrorCode.ILLEGAL_STATE.getCode(), message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e) {
        log.error("Unexpected error occurred", e);
        
        Locale locale = getCurrentLocale();
        String messageKey = getMessageKey(StandardErrorCode.BASE_ERROR.getCode());
        String message = resolveMessage(messageKey, locale);
        
        return Result.error(StandardErrorCode.BASE_ERROR.getCode(), message);
    }
    
    /**
     * 获取当前语言环境
     */
    private Locale getCurrentLocale() {
        if (localeContext != null) {
            return localeContext.getCurrentLocale();
        }
        return Locale.getDefault();
    }
    
    /**
     * 解析消息
     */
    private String resolveMessage(String messageKey, Locale locale, Object... args) {
        if (messageResolver != null) {
            return messageResolver.resolveMessage(messageKey, locale, args);
        }
        // 如果没有配置国际化解析器，返回消息键
        return messageKey;
    }
    
    /**
     * 根据错误码获取消息键
     * 格式：error.{错误码小写}
     */
    private String getMessageKey(String errorCode) {
        return "error." + errorCode.toLowerCase();
    }
}