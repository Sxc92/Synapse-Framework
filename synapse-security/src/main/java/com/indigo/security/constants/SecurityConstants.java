package com.indigo.security.constants;

/**
 * 安全模块常量类
 * 统一管理 Gateway 和微服务之间传递的请求头、属性键等常量
 * 
 * @author 史偕成
 * @date 2025/01/10
 */
public final class SecurityConstants {

    private SecurityConstants() {
        // 工具类，禁止实例化
    }

    // ===========================================
    // HTTP 请求头常量（可配置，请使用配置）
    // ===========================================

    /**
     * X-User-Context 请求头
     * Gateway 传递编码后的用户上下文信息
     */
    public static final String X_USER_CONTEXT_HEADER = "X-User-Context";

    /**
     * X-User-Permissions 请求头
     * Gateway 传递用户权限列表（逗号分隔）
     */
    public static final String X_USER_PERMISSIONS_HEADER = "X-User-Permissions";

    /**
     * X-Gateway-Signature 请求头
     * Gateway 传递的签名，用于防篡改验证
     */
    public static final String X_GATEWAY_SIGNATURE_HEADER = "X-Gateway-Signature";

    /**
     * X-Gateway-Timestamp 请求头
     * Gateway 传递的时间戳，用于防重放攻击
     */
    public static final String X_GATEWAY_TIMESTAMP_HEADER = "X-Gateway-Timestamp";

    /**
     * X-Internal-Service 请求头
     * 内部服务调用标识，标识调用来源服务
     */
    public static final String X_INTERNAL_SERVICE_HEADER = "X-Internal-Service";

    /**
     * X-Internal-Timestamp 请求头
     * 内部服务调用的时间戳，用于防重放攻击
     */
    public static final String X_INTERNAL_TIMESTAMP_HEADER = "X-Internal-Timestamp";

    /**
     * X-Internal-Signature 请求头
     * 内部服务调用的签名，用于验证调用来源
     */
    public static final String X_INTERNAL_SIGNATURE_HEADER = "X-Internal-Signature";

    // ===========================================
    // 请求属性键常量
    // ===========================================

    /**
     * 请求属性中存储 token 的 key
     * 供 PermissionService 等组件使用
     */
    public static final String REQUEST_ATTR_TOKEN = "SYNAPSE_REQUEST_TOKEN";

    /**
     * Exchange 属性中存储 token 的 key
     * Gateway 中使用
     */
    public static final String EXCHANGE_ATTR_TOKEN = "token";

    /**
     * Exchange 属性中存储用户上下文的 key
     * Gateway 中使用
     */
    public static final String EXCHANGE_ATTR_USER_CONTEXT = "userContext";

}

