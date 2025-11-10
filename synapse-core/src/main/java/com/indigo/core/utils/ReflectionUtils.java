package com.indigo.core.utils;

/**
 * 反射工具类
 * 提供实体类实例创建等反射操作
 * 
 * @author 史偕成
 * @date 2025/01/07
 */
public class ReflectionUtils {
    
    /**
     * 创建实体实例
     * 优先尝试使用 Builder 模式（Lombok @SuperBuilder），如果失败则使用无参构造函数
     * 
     * <h3>使用场景：</h3>
     * <ul>
     *   <li>支持 Lombok @SuperBuilder 注解的实体类</li>
     *   <li>支持标准无参构造函数的实体类</li>
     * </ul>
     * 
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 使用 Builder 模式创建
     * User user = ReflectionUtils.createEntityInstance(User.class);
     * 
     * // 使用无参构造函数创建
     * Order order = ReflectionUtils.createEntityInstance(Order.class);
     * }</pre>
     * 
     * @param <T> 实体类型
     * @param entityClass 实体类
     * @return 实体实例
     * @throws IllegalArgumentException 如果无法创建实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T createEntityInstance(Class<T> entityClass) {
        if (entityClass == null) {
            throw new IllegalArgumentException("Entity class cannot be null");
        }
        
        try {
            // 优先尝试使用 Builder 模式（Lombok @SuperBuilder）
            try {
                java.lang.reflect.Method builderMethod = entityClass.getMethod("builder");
                Object builder = builderMethod.invoke(null);
                java.lang.reflect.Method buildMethod = builder.getClass().getMethod("build");
                return (T) buildMethod.invoke(builder);
            } catch (NoSuchMethodException | java.lang.reflect.InvocationTargetException | IllegalAccessException e) {
                // Builder 模式失败，尝试无参构造函数
                return entityClass.getDeclaredConstructor().newInstance();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Failed to create entity instance for class: " + entityClass.getName() + 
                ". Make sure the class has a no-arg constructor or builder() method.", e);
        }
    }
}

