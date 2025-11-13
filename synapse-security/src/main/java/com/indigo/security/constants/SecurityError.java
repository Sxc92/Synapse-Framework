package com.indigo.security.constants;

import com.indigo.core.constants.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 安全认证相关错误码枚举
 * 提供安全认证、权限控制相关的标准错误码
 * 
 * <p><b>注意：</b>此枚举中的错误码后期将维护到数据库中，请勿随意修改错误码值。
 * 如需添加新的错误码，请先确认数据库中不存在相同的错误码。
 * 
 * <p><b>错误码分类：</b>
 * <ul>
 *   <li>SEC001-SEC019：安全认证相关错误码（从 StandardErrorCode 迁移）</li>
 *   <li>SECURITY001-SECURITY006：认证请求相关错误码</li>
 * </ul>
 * 
 * @author 史偕成
 * @date 2025/11/10 13:42
 */
@Getter
@AllArgsConstructor
public enum SecurityError implements ErrorCode {

    // ==================== 安全认证相关错误码（从 StandardErrorCode 迁移） ====================
    
    /**
     * 安全错误
     * 错误码：SEC001
     */
    SECURITY_ERROR("SEC001"),

    /**
     * 未登录
     * 错误码：SEC002
     */
    NOT_LOGIN("SEC002"),

    /**
     * 登录失败
     * 错误码：SEC003
     */
    LOGIN_FAILED("SEC003"),

    /**
     * 登录已过期
     * 错误码：SEC004
     */
    LOGIN_EXPIRED("SEC004"),

    /**
     * Token无效
     * 错误码：SEC005
     */
    TOKEN_INVALID("SEC005"),

    /**
     * Token已过期
     * 错误码：SEC006
     */
    TOKEN_EXPIRED("SEC006"),

    /**
     * Token缺失
     * 错误码：SEC007
     */
    TOKEN_MISSING("SEC007"),

    /**
     * 权限不足
     * 错误码：SEC008
     */
    PERMISSION_DENIED("SEC008"),

    /**
     * 角色不足
     * 错误码：SEC009
     */
    ROLE_DENIED("SEC009"),

    /**
     * 账户已禁用
     * 错误码：SEC010
     */
    ACCOUNT_DISABLED("SEC010"),

    /**
     * 账户已锁定
     * 错误码：SEC011
     */
    ACCOUNT_LOCKED("SEC011"),

    /**
     * 需要多因素认证
     * 错误码：SEC012
     */
    MFA_REQUIRED("SEC012"),

    /**
     * 多因素认证失败
     * 错误码：SEC013
     */
    MFA_FAILED("SEC013"),

    /**
     * 会话无效
     * 错误码：SEC014
     */
    SESSION_INVALID("SEC014"),

    /**
     * 会话已过期
     * 错误码：SEC015
     */
    SESSION_EXPIRED("SEC015"),

    /**
     * OAuth错误
     * 错误码：SEC016
     */
    OAUTH_ERROR("SEC016"),

    /**
     * OAuth客户端无效
     * 错误码：SEC017
     */
    OAUTH_INVALID_CLIENT("SEC017"),

    /**
     * OAuth授权无效
     * 错误码：SEC018
     */
    OAUTH_INVALID_GRANT("SEC018"),

    /**
     * OAuth访问被拒绝
     * 错误码：SEC019
     */
    OAUTH_ACCESS_DENIED("SEC019"),

    // ==================== 认证请求相关错误码 ====================

    /**
     * 认证请求不能为空
     * 错误码：SECURITY001
     */
    AUTH_REQUEST_INVALID("SECURITY001"),

    /**
     * 认证请求信息不完整
     * 错误码：SECURITY002
     */
    AUTH_REQUEST_INCOMPLETE("SECURITY002"),

    /**
     * 认证Token无效
     * 错误码：SECURITY003
     */
    AUTH_TOKEN_INVALID("SECURITY003"),

    /**
     * 该用户没有配置任何角色
     * 错误码：SECURITY004
     */
    AUTH_USER_HAS_NO_ROLE("SECURITY004"),

    /**
     * 该用户没有配置任何资源
     * 错误码：SECURITY005
     */
    AUTH_USER_HAS_NO_RESOURCE("SECURITY005"),

    /**
     * 认证Token不存在
     * 错误码：SECURITY005
     * 注意：与 AUTH_USER_HAS_NO_RESOURCE 错误码重复，建议使用 TOKEN_MISSING (SEC007)
     */
    AUTH_TOKEN_NOT_EXIST("SECURITY005"),

    /**
     * 认证Token为空
     * 错误码：SECURITY006
     * 注意：建议使用 TOKEN_MISSING (SEC007)
     */
    AUTH_TOKEN_NULL("SECURITY006"),
    ;

    private final String code;

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessageKey() {
        return ErrorCode.super.getMessageKey();
    }
}
