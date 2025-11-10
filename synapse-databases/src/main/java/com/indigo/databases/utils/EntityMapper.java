package com.indigo.databases.utils;

import com.indigo.core.annotation.IgnoreOnCopy;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Entity 映射工具类
 * 提供 DTO 到 Entity 的自动映射功能
 * 
 * <h3>功能特性：</h3>
 * <ul>
 *   <li>自动忽略审计字段（createTime, modifyTime, createUser, modifyUser）</li>
 *   <li>自动忽略系统字段（id, revision, deleted）</li>
 *   <li>支持 @IgnoreOnCopy 注解</li>
 *   <li>支持自定义忽略字段列表</li>
 *   <li>支持新增/更新场景区分</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 新增场景
 * Menu menu = Menu.builder().build();
 * EntityMapper.copyFromDTO(dto, menu);
 * iMenuService.save(menu);
 * 
 * // 更新场景
 * Menu menu = iMenuService.getById(id);
 * EntityMapper.copyFromDTOForUpdate(dto, menu);
 * iMenuService.updateById(menu);
 * 
 * // 自定义忽略字段
 * EntityMapper.copyFromDTO(dto, menu, "customField1", "customField2");
 * }</pre>
 * 
 * @author 史偕成
 * @date 2025/01/XX
 */
public class EntityMapper {
    
    /**
     * 默认忽略的字段（审计字段和系统字段）
     */
    private static final Set<String> DEFAULT_IGNORE_FIELDS = Set.of(
        "id", "revision", "deleted",
        "createTime", "modifyTime", "createUser", "modifyUser"
    );
    
    /**
     * 从 DTO 复制属性到 Entity（新增场景）
     * 
     * @param source DTO 对象
     * @param target Entity 对象
     * @param <T> Entity 类型
     * @return 复制后的 Entity 对象
     */
    public static <T> T copyFromDTO(Object source, T target) {
        return copyFromDTO(source, target, CopyMode.INSERT);
    }
    
    /**
     * 从 DTO 复制属性到 Entity（更新场景）
     * 
     * @param source DTO 对象
     * @param target Entity 对象
     * @param <T> Entity 类型
     * @return 复制后的 Entity 对象
     */
    public static <T> T copyFromDTOForUpdate(Object source, T target) {
        return copyFromDTO(source, target, CopyMode.UPDATE);
    }
    
    /**
     * 从 DTO 复制属性到 Entity（支持自定义忽略字段）
     * 
     * @param source DTO 对象
     * @param target Entity 对象
     * @param ignoreFields 自定义忽略字段列表
     * @param <T> Entity 类型
     * @return 复制后的 Entity 对象
     */
    public static <T> T copyFromDTO(Object source, T target, String... ignoreFields) {
        Set<String> allIgnoreFields = new HashSet<>(DEFAULT_IGNORE_FIELDS);
        if (ignoreFields != null && ignoreFields.length > 0) {
            allIgnoreFields.addAll(Arrays.asList(ignoreFields));
        }
        
        BeanUtils.copyProperties(source, target, allIgnoreFields.toArray(new String[0]));
        return target;
    }
    
    /**
     * 从 DTO 复制属性到 Entity（完整版本，支持模式区分）
     * 
     * @param source DTO 对象
     * @param target Entity 对象
     * @param mode 复制模式（INSERT/UPDATE）
     * @param <T> Entity 类型
     * @return 复制后的 Entity 对象
     */
    public static <T> T copyFromDTO(Object source, T target, CopyMode mode) {
        if (source == null || target == null) {
            return target;
        }
        
        Set<String> ignoreFields = getIgnoreFields(source, target, mode);
        BeanUtils.copyProperties(source, target, ignoreFields.toArray(new String[0]));
        return target;
    }
    
    /**
     * 获取需要忽略的字段列表
     * 
     * @param source DTO 对象
     * @param target Entity 对象
     * @param mode 复制模式
     * @return 忽略字段集合
     */
    private static Set<String> getIgnoreFields(Object source, Object target, CopyMode mode) {
        Set<String> ignoreFields = new HashSet<>(DEFAULT_IGNORE_FIELDS);
        
        // 处理 @IgnoreOnCopy 注解（从 source DTO）
        processIgnoreOnCopyAnnotation(source, ignoreFields, mode);
        
        // 处理 @IgnoreOnCopy 注解（从 target Entity）
        processIgnoreOnCopyAnnotation(target, ignoreFields, mode);
        
        return ignoreFields;
    }
    
    /**
     * 处理 @IgnoreOnCopy 注解
     * 
     * @param obj 对象（DTO 或 Entity）
     * @param ignoreFields 忽略字段集合
     * @param mode 复制模式
     */
    private static void processIgnoreOnCopyAnnotation(Object obj, Set<String> ignoreFields, CopyMode mode) {
        if (obj == null) {
            return;
        }
        
        Class<?> clazz = obj.getClass();
        
        // 遍历当前类及其所有父类的字段
        while (clazz != null && clazz != Object.class) {
            Field[] fields = clazz.getDeclaredFields();
            
            for (Field field : fields) {
                IgnoreOnCopy annotation = field.getAnnotation(IgnoreOnCopy.class);
                if (annotation != null) {
                    IgnoreOnCopy.CopyScenario[] scenarios = annotation.value();
                    
                    // 检查是否需要忽略
                    boolean shouldIgnore = false;
                    for (IgnoreOnCopy.CopyScenario scenario : scenarios) {
                        if (scenario == IgnoreOnCopy.CopyScenario.ALL) {
                            shouldIgnore = true;
                            break;
                        } else if (scenario == IgnoreOnCopy.CopyScenario.INSERT_ONLY && mode == CopyMode.INSERT) {
                            shouldIgnore = true;
                            break;
                        } else if (scenario == IgnoreOnCopy.CopyScenario.UPDATE_ONLY && mode == CopyMode.UPDATE) {
                            shouldIgnore = true;
                            break;
                        }
                    }
                    
                    if (shouldIgnore) {
                        ignoreFields.add(field.getName());
                    }
                }
            }
            
            // 继续处理父类
            clazz = clazz.getSuperclass();
        }
    }
    
    /**
     * 复制模式枚举
     */
    public enum CopyMode {
        /**
         * 新增模式
         */
        INSERT,
        
        /**
         * 更新模式
         */
        UPDATE
    }
}

