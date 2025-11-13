package com.indigo.security.exception;

import com.indigo.core.exception.SynapseException;
import com.indigo.security.constants.SecurityError;

/**
 * 未登录异常
 * 当用户未登录或登录已过期时抛出
 * 
 * <p><b>使用示例：</b>
 * <pre>
 * if (userContext == null) {
 *     throw new NotLoginException();
 * }
 * </pre>
 * 
 * @author 史偕成
 * @date 2025/01/XX
 */
public class NotLoginException extends SynapseException {
    
    /**
     * 构造函数
     * 使用默认错误码 {@link SecurityError#NOT_LOGIN}
     */
    public NotLoginException() {
        super(SecurityError.NOT_LOGIN);
    }
    
    /**
     * 构造函数（自定义消息）
     * 
     * @param message 自定义消息
     */
    public NotLoginException(String message) {
        super(SecurityError.NOT_LOGIN, message);
    }
    
    /**
     * 构造函数（带原因）
     * 
     * @param cause 异常原因
     */
    public NotLoginException(Throwable cause) {
        super(SecurityError.NOT_LOGIN, cause);
    }
    
    /**
     * 构造函数（自定义消息，带原因）
     * 
     * @param message 自定义消息
     * @param cause 异常原因
     */
    public NotLoginException(String message, Throwable cause) {
        super(SecurityError.NOT_LOGIN, message, cause);
    }
}

