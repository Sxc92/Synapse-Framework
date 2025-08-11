package com.indigo.core.exception.handler;

import com.indigo.core.entity.Result;
import com.indigo.core.exception.*;
import com.indigo.core.exception.enums.ErrorCode;
import com.indigo.core.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * WebFlux 全局异常处理器
 * 处理响应式环境下的所有异常，包括Gateway异常
 *
 * @author 史偕成
 * @date 2025/12/19
 **/
@Slf4j
@Order(-1)
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        // 如果响应已经提交，直接返回错误
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        // 设置响应头
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 根据异常类型处理
        Result<?> result;
        HttpStatus status;

        if (ex instanceof I18nException e) {
            log.error("I18nException: code={}, message={}", e.getCode(), e.getMessage(), e);
            result = Result.error(e.getCode(), e.getMessage());
            status = HttpStatus.BAD_REQUEST;
        } else if (ex instanceof BusinessException e) {
            log.warn("BusinessException: code={}, message={}", e.getCode(), e.getMessage());
            result = Result.error(e.getCode(), e.getMessage());
            status = HttpStatus.BAD_REQUEST;
        } else if (ex instanceof IAMException e) {
            log.error("IAMException: code={}, message={}", e.getCode(), e.getMessage(), e);
            result = Result.error(e.getCode(), e.getMessage());
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        } else if (ex instanceof IllegalStateException) {
            log.warn("IllegalStateException: message={}", ex.getMessage());
            result = Result.error(ErrorCode.ILLEGAL_STATE.getCode(), ex.getMessage());
            status = HttpStatus.BAD_REQUEST;
        } else if (ex instanceof AssertException e) {
            log.warn("AssertException: code={}, message={}", e.getCode(), e.getMessage());
            result = Result.error(e.getCode(), e.getMessage());
            status = HttpStatus.BAD_REQUEST;
        } else if (ex instanceof RateLimitException e) {
            log.warn("RateLimitException: message={}", e.getMessage());
            result = Result.error(ErrorCode.RATE_LIMIT_EXCEEDED.getCode(), e.getMessage());
            status = HttpStatus.TOO_MANY_REQUESTS;
        } else if (ex instanceof GatewayException e) {
            log.warn("GatewayException: code={}, message={}", e.getErrorCode(), e.getMessage());
            result = Result.error(e.getErrorCode(), e.getMessage());
            
            // 根据错误码设置状态码
            switch (e.getErrorCode()) {
                case "TOKEN_MISSING":
                case "TOKEN_INVALID":
                case "TOKEN_EXPIRED":
                    status = HttpStatus.UNAUTHORIZED;
                    break;
                case "PERMISSION_DENIED":
                    status = HttpStatus.FORBIDDEN;
                    break;
                case "RATE_LIMIT_EXCEEDED":
                    status = HttpStatus.TOO_MANY_REQUESTS;
                    break;
                case "SERVICE_UNAVAILABLE":
                    status = HttpStatus.SERVICE_UNAVAILABLE;
                    break;
                default:
                    status = HttpStatus.BAD_REQUEST;
            }
        } else if (ex instanceof IllegalArgumentException) {
            log.warn("IllegalArgumentException: message={}", ex.getMessage());
            result = Result.error("INVALID_PARAMETER", "请求参数错误: " + ex.getMessage());
            status = HttpStatus.BAD_REQUEST;
        } else if (ex instanceof java.lang.SecurityException) {
            log.warn("SecurityException: message={}", ex.getMessage());
            result = Result.error("ACCESS_DENIED", "访问被拒绝: " + ex.getMessage());
            status = HttpStatus.FORBIDDEN;
        } else if (ex instanceof RuntimeException) {
            log.error("RuntimeException: message={}", ex.getMessage(), ex);
            result = Result.error("RUNTIME_ERROR", "运行时错误: " + ex.getMessage());
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        } else {
            log.error("Unexpected error occurred", ex);
            result = Result.error(ErrorCode.BASE_ERROR.getCode(), ErrorCode.BASE_ERROR.getMessage());
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        
        // 设置状态码
        response.setStatusCode(status);
        
        // 序列化响应
        String jsonResponse = JsonUtils.toJsonString(result);
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
