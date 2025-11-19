package com.indigo.security.aspect;

import com.indigo.security.annotation.Logical;
import com.indigo.security.annotation.RequireLogin;
import com.indigo.security.annotation.RequirePermission;
import com.indigo.security.annotation.RequireRole;
import com.indigo.security.config.SecurityProperties;
import com.indigo.security.core.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 权限检查切面
 * 通过 AOP 拦截自定义权限注解，进行权限检查
 * 
 * <p><b>支持的注解：</b>
 * <ul>
 *   <li>{@link RequireLogin} - 要求登录</li>
 *   <li>{@link RequireRole} - 要求角色</li>
 *   <li>{@link RequirePermission} - 要求权限</li>
 * </ul>
 * 
 * <p><b>优先级：</b>
 * 方法级注解优先于类级注解
 * 
 * <p><b>使用示例：</b>
 * <pre>
 * // 方法级使用
 * {@code @RequireLogin}
 * public Result&lt;UserVO&gt; getProfile() {
 *     // ...
 * }
 * 
 * // 类级使用
 * {@code @RequireLogin}
 * {@code @RestController}
 * public class UserController {
 *     // ...
 * }
 * </pre>
 * 
 * @author 史偕成
 * @date 2025/01/XX
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
@Order(1) // 确保在其他切面之前执行
public class PermissionAspect {

    private final PermissionService permissionService;
    private final SecurityProperties securityProperties;

    /**
     * 拦截 @RequireLogin 注解
     */
    @Before("@annotation(com.indigo.security.annotation.RequireLogin) || " +
            "@within(com.indigo.security.annotation.RequireLogin)")
    public void checkLogin(JoinPoint joinPoint) {
        // 如果安全模式为 DISABLED，跳过所有权限检查
        if (isSecurityDisabled()) {
            log.debug("安全模式为 DISABLED，跳过登录检查: method={}", joinPoint.getSignature().getName());
            return;
        }
        
        RequireLogin annotation = getAnnotation(joinPoint, RequireLogin.class);
        if (annotation != null) {
            log.debug("检查登录状态: method={}", joinPoint.getSignature().getName());
            permissionService.checkLogin();
        }
    }

    /**
     * 拦截 @RequireRole 注解
     */
    @Before("@annotation(com.indigo.security.annotation.RequireRole) || " +
            "@within(com.indigo.security.annotation.RequireRole)")
    public void checkRole(JoinPoint joinPoint) {
        // 如果安全模式为 DISABLED，跳过所有权限检查
        if (isSecurityDisabled()) {
            log.debug("安全模式为 DISABLED，跳过角色检查: method={}", joinPoint.getSignature().getName());
            return;
        }
        
        RequireRole annotation = getAnnotation(joinPoint, RequireRole.class);
        if (annotation != null) {
            String[] roles = annotation.value();
            Logical logical = annotation.logical();
            log.debug("检查角色: method={}, roles={}, logical={}", 
                    joinPoint.getSignature().getName(), List.of(roles), logical);
            permissionService.checkRole(roles, logical);
        }
    }

    /**
     * 拦截 @RequirePermission 注解
     */
    @Before("@annotation(com.indigo.security.annotation.RequirePermission) || " +
            "@within(com.indigo.security.annotation.RequirePermission)")
    public void checkPermission(JoinPoint joinPoint) {
        // 如果安全模式为 DISABLED，跳过所有权限检查
        if (isSecurityDisabled()) {
            log.debug("安全模式为 DISABLED，跳过权限检查: method={}", joinPoint.getSignature().getName());
            return;
        }
        
        RequirePermission annotation = getAnnotation(joinPoint, RequirePermission.class);
        if (annotation != null) {
            String[] permissions = annotation.value();
            Logical logical = annotation.logical();
            log.debug("检查权限: method={}, permissions={}, logical={}", 
                    joinPoint.getSignature().getName(), List.of(permissions), logical);
            permissionService.checkPermission(permissions, logical);
        }
    }

    /**
     * 检查安全模式是否为 DISABLED
     * 
     * @return 如果安全模式为 DISABLED 返回 true，否则返回 false
     */
    private boolean isSecurityDisabled() {
        if (securityProperties == null) {
            return false;
        }
        return securityProperties.getMode() == SecurityProperties.SecurityMode.DISABLED;
    }

    /**
     * 获取注解（优先从方法获取，如果方法没有则从类获取）
     * 
     * @param joinPoint 连接点
     * @param annotationClass 注解类型
     * @param <T> 注解类型
     * @return 注解实例，如果不存在则返回 null
     */
    private <T extends Annotation> T getAnnotation(JoinPoint joinPoint, Class<T> annotationClass) {
        // 1. 优先从方法获取
        if (joinPoint.getSignature() instanceof MethodSignature methodSignature) {
            Method method = methodSignature.getMethod();
            T annotation = AnnotationUtils.findAnnotation(method, annotationClass);
            if (annotation != null) {
                return annotation;
            }
        }

        // 2. 从类获取
        Class<?> targetClass = joinPoint.getTarget().getClass();
        return AnnotationUtils.findAnnotation(targetClass, annotationClass);
    }
}

