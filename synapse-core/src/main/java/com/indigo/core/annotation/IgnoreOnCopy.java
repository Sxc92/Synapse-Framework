package com.indigo.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记字段在 DTO 到 Entity 复制时忽略
 * 
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>审计字段（createTime, modifyTime, createUser, modifyUser）</li>
 *   <li>系统字段（id, revision, deleted）</li>
 *   <li>业务字段（某些字段在更新时不应该被覆盖）</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @Data
 * public class AddOrModifyMenuDTO extends BaseDTO<String> {
 *     private String code;
 *     private String name;
 *     
 *     // 更新时忽略此字段（不允许修改）
 *     @IgnoreOnCopy(CopyScenario.UPDATE_ONLY)
 *     private String systemId;
 *     
 *     // 所有场景都忽略
 *     @IgnoreOnCopy
 *     private String internalFlag;
 * }
 * }</pre>
 * 
 * @author 史偕成
 * @date 2025/01/XX
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IgnoreOnCopy {
    
    /**
     * 忽略场景
     * 
     * @return 忽略场景数组，默认为所有场景
     */
    CopyScenario[] value() default {CopyScenario.ALL};
    
    /**
     * 复制场景枚举
     */
    enum CopyScenario {
        /**
         * 所有场景都忽略（新增和更新都忽略）
         */
        ALL,
        
        /**
         * 仅在新增时忽略
         */
        INSERT_ONLY,
        
        /**
         * 仅在更新时忽略
         */
        UPDATE_ONLY
    }
}

