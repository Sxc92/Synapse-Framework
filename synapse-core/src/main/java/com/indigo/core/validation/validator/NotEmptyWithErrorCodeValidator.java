package com.indigo.core.validation.validator;

import cn.hutool.core.collection.CollUtil;
import com.indigo.core.constants.ErrorCode;
import com.indigo.core.exception.Ex;
import com.indigo.core.validation.NotEmptyWithErrorCode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collection;

/**
 * 自定义验证器：非空验证（支持自定义错误码）
 * 验证失败时抛出框架的统一异常
 *
 * @author 史偕成
 * @date 2025/11/29
 */
public class NotEmptyWithErrorCodeValidator implements ConstraintValidator<NotEmptyWithErrorCode, Object> {

    private ErrorCode errorCode;
    private String message;

    @Override
    @SuppressWarnings("unchecked")
    public void initialize(NotEmptyWithErrorCode annotation) {
        // 通过反射获取枚举值（因为注解属性不能直接使用接口类型）
        try {
            Class<? extends Enum<? extends ErrorCode>> enumClass = annotation.errorCodeEnum();
            Enum<? extends ErrorCode> enumValue = Enum.valueOf(
                (Class<? extends Enum>) enumClass,
                annotation.errorCodeName()
            );
            this.errorCode = (ErrorCode) enumValue;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid error code enum: " + annotation.errorCodeEnum().getName() + "." + annotation.errorCodeName(),
                e
            );
        }
        this.message = annotation.message();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // 如果值为 null，验证失败
        if (value == null) {
            throwException();
            return false;
        }

        // 如果是集合类型，检查是否为空
        if (value instanceof Collection) {
            if (CollUtil.isEmpty((Collection<?>) value)) {
                throwException();
                return false;
            }
        }

        // 如果是字符串类型，检查是否为空
        if (value instanceof String) {
            if (((String) value).trim().isEmpty()) {
                throwException();
                return false;
            }
        }

        return true;
    }

    /**
     * 抛出框架的统一异常
     * 使用 Ex 工具类抛出 SynapseException
     */
    private void throwException() {
        // 如果有自定义消息，使用带消息的方法；否则使用默认方法
        if (message != null && !message.trim().isEmpty()) {
            Ex.throwEx(errorCode, message);
        } else {
            Ex.throwEx(errorCode);
        }
    }
}

