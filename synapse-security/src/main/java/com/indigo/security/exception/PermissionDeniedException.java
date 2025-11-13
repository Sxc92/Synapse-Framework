package com.indigo.security.exception;

import com.indigo.core.exception.SynapseException;
import com.indigo.security.constants.SecurityError;

/**
 * 权限不足异常
 * 当用户没有足够权限访问资源时抛出
 * 
 * <p><b>使用示例：</b>
 * <pre>
 * if (!hasPermission("user:delete")) {
 *     throw new PermissionDeniedException("没有删除用户的权限");
 * }
 * </pre>
 * 
 * @author 史偕成
 * @date 2025/01/XX
 */
public class PermissionDeniedException extends SynapseException {
    
    /**
     * 构造函数
     * 使用默认错误码 {@link SecurityError#PERMISSION_DENIED}
     */
    public PermissionDeniedException() {
        super(SecurityError.PERMISSION_DENIED);
    }
    
    /**
     * 构造函数（自定义消息）
     * 
     * @param message 自定义消息
     */
    public PermissionDeniedException(String message) {
        super(SecurityError.PERMISSION_DENIED, message);
    }
    
    /**
     * 构造函数（带原因）
     * 
     * @param cause 异常原因
     */
    public PermissionDeniedException(Throwable cause) {
        super(SecurityError.PERMISSION_DENIED, cause);
    }
    
    /**
     * 构造函数（自定义消息，带原因）
     * 
     * @param message 自定义消息
     * @param cause 异常原因
     */
    public PermissionDeniedException(String message, Throwable cause) {
        super(SecurityError.PERMISSION_DENIED, message, cause);
    }
}

