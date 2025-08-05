package com.indigo.core.exception;

/**
 * Gateway 自定义异常
 * 用于处理 Gateway 中的特定异常情况
 *
 * @author 史偕成
 * @date 2025/01/07
 */
public class GatewayException extends RuntimeException {

    private final String errorCode;

    public GatewayException(String message) {
        super(message);
        this.errorCode = "GATEWAY_ERROR";
    }

    public GatewayException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public GatewayException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "GATEWAY_ERROR";
    }

    public GatewayException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
} 