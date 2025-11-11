package com.indigo.security.constants;

import com.indigo.core.constants.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author 史偕成
 * @date 2025/11/10 13:42
 **/
@Getter
@AllArgsConstructor
public enum SecurityError implements ErrorCode {

    /**
     * 认证请求不能为空
     */
    AUTH_REQUEST_INVALID("SECURITY001"),

    /**
     * 认证请求信息不完整
     */
    AUTH_REQUEST_INCOMPLETE("SECURITY002"),

    /**
     * 该用户没有配置任何角色
     */
    AUTH_USER_HAS_NO_ROLE("SECURITY004"),

    /**
     * 该用户没有配置任何资源
     */
    AUTH_USER_HAS_NO_RESOURCE("SECURITY005"),

    /**
     * 认证Token无效
     */
    AUTH_TOKEN_INVALID("SECURITY003"),

    /**
     * 认证Token已过期
     */
    AUTH_TOKEN_EXPIRED("SECURITY004"),

    /**
     * 认证Token不存在
     */
    AUTH_TOKEN_NOT_EXIST("SECURITY005"),

    /**
     * 认证Token为空
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
