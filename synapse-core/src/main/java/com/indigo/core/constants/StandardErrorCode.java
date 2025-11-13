package com.indigo.core.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 标准错误码枚举
 * 提供框架内置的标准错误码
 * 
 * <p><b>注意：</b>此枚举中的错误码后期将维护到数据库中，请勿随意修改错误码值。
 * 如需添加新的错误码，请先确认数据库中不存在相同的错误码。
 * 
 * <p><b>错误码分类：</b>
 * <ul>
 *   <li>系统级错误码（SYSxxx）：系统通用错误，如参数错误、系统异常等</li>
 *   <li>业务级错误码（BUSxxx）：业务逻辑相关错误，如数据不存在、操作失败等</li>
 * </ul>
 * 
 * <p><b>安全认证相关错误码：</b>
 * 已迁移到 {@link com.indigo.security.constants.SecurityError}，请使用该类中的错误码。
 * 
 * @author 史偕成
 * @date 2025/03/21
 */
@Getter
@AllArgsConstructor
public enum StandardErrorCode implements ErrorCode {
    // ==================== 系统级错误码 ====================
    
    /**
     * 成功（HTTP 200）
     * 错误码：200
     */
    BASE_SUCCESS("SUCCESS"),
    
    /**
     * 基础错误
     * 错误码：SYS000
     */
    BASE_ERROR("SYS000"),
    
    /**
     * 非法状态
     * 错误码：SYS011
     */
    ILLEGAL_STATE("SYS011"),
    
    /**
     * 系统错误
     * 错误码：SYS001
     */
    SYSTEM_ERROR("SYS001"),
    
    /**
     * 参数错误
     * 错误码：SYS002
     */
    PARAM_ERROR("SYS002"),
    
    /**
     * 未授权（HTTP 401）
     * 错误码：SYS003
     * 注意：安全认证相关错误请使用 {@link com.indigo.security.constants.SecurityError}
     */
    UNAUTHORIZED("SYS003"),
    
    /**
     * 禁止访问（HTTP 403）
     * 错误码：SYS004
     * 注意：权限相关错误请使用 {@link com.indigo.security.constants.SecurityError#PERMISSION_DENIED}
     */
    FORBIDDEN("SYS004"),
    
    /**
     * 资源不存在（HTTP 404）
     * 错误码：SYS005
     */
    NOT_FOUND("SYS005"),
    
    /**
     * 方法不允许（HTTP 405）
     * 错误码：SYS006
     */
    METHOD_NOT_ALLOWED("SYS006"),
    
    /**
     * 请求超时（HTTP 408）
     * 错误码：SYS007
     */
    REQUEST_TIMEOUT("SYS007"),
    
    /**
     * 请求过多（HTTP 429）
     * 错误码：SYS008
     */
    TOO_MANY_REQUESTS("SYS008"),
    
    /**
     * 限流超出限制
     * 错误码：SYS012
     */
    RATE_LIMIT_EXCEEDED("SYS012"),
    
    /**
     * 服务不可用（HTTP 503）
     * 错误码：SYS009
     */
    SERVICE_UNAVAILABLE("SYS009"),
    
    /**
     * 网关错误（HTTP 502）
     * 错误码：SYS010
     */
    GATEWAY_ERROR("SYS010"),
    
    /**
     * 线程错误
     * 错误码：SYS013
     */
    THREAD_ERROR("SYS013"),

    // ==================== 业务级错误码 ====================
    
    /**
     * 业务错误
     * 错误码：BUS001
     */
    BUSINESS_ERROR("BUS001"),
    
    /**
     * 数据不存在
     * 错误码：BUS002
     */
    DATA_NOT_FOUND("BUS002"),
    
    /**
     * 数据已存在
     * 错误码：BUS003
     */
    DATA_ALREADY_EXISTS("BUS003"),
    
    /**
     * 数据无效
     * 错误码：BUS004
     */
    DATA_INVALID("BUS004"),
    
    /**
     * 操作失败
     * 错误码：BUS005
     */
    OPERATION_FAILED("BUS005"),
    
    /**
     * 操作超时
     * 错误码：BUS006
     */
    OPERATION_TIMEOUT("BUS006"),
    
    /**
     * 操作被禁止
     * 错误码：BUS007
     */
    OPERATION_FORBIDDEN("BUS007"),
    
    /**
     * 操作不允许
     * 错误码：BUS008
     */
    OPERATION_NOT_ALLOWED("BUS008"),
    
    /**
     * 操作无效
     * 错误码：BUS009
     */
    OPERATION_INVALID("BUS009"),
    
    /**
     * 操作冲突
     * 错误码：BUS010
     */
    OPERATION_CONFLICT("BUS010"),
    
    // 注意：其他业务级错误码请根据实际业务模块添加，如 IAM、WORKFLOW 等
    ;


    private final String code;
}
