# BaseVO 灵活使用指南

## 问题背景

原始的 `BaseVO` 硬编码了字段名，如果用户使用不同的数据库字段命名规范，会导致映射失败。

## 解决方案

### 方案1：使用 @FieldMapping 注解（推荐）

在继承 `BaseVO` 的子类中使用 `@FieldMapping` 注解来指定实际的数据库字段名：

```java
@EqualsAndHashCode(callSuper = true)
@Data
public class UserVO extends BaseVO<String> {
    
    // 如果数据库字段名不同
    @FieldMapping(value = "created_at")
    private LocalDateTime createTime;
    
    @FieldMapping(value = "updated_at") 
    private LocalDateTime modifyTime;
    
    // 如果数据库中没有对应字段
    @FieldMapping(ignore = true)
    private String createUser;
    
    @FieldMapping(ignore = true)
    private String modifyUser;
    
    // 其他业务字段
    private String userName;
}
```

### 方案2：使用预定义的 BaseVO 变体

我们提供了多种 `BaseVO` 变体，适应不同的命名规范：

#### StandardBaseVO - 标准命名
```java
// 数据库字段：create_time, modify_time, create_user, modify_user
public class UserVO extends StandardBaseVO<String> {
    private String userName;
}
```

#### SimpleBaseVO - 简化命名
```java
// 数据库字段：created_at, updated_at
public class UserVO extends SimpleBaseVO<String> {
    private String userName;
}
```

### 方案3：自定义 BaseVO

如果以上方案都不满足需求，可以创建自己的 BaseVO：

```java
@Data
public abstract class CustomBaseVO<T> implements Serializable {
    
    private T id;
    
    @FieldMapping(value = "your_create_time_field")
    private LocalDateTime createTime;
    
    @FieldMapping(value = "your_update_time_field")
    private LocalDateTime modifyTime;
    
    // 其他字段...
}
```

## 使用建议

1. **优先使用方案1**：通过 `@FieldMapping` 注解灵活映射
2. **如果命名规范固定**：使用预定义的 BaseVO 变体
3. **如果需求特殊**：创建自定义 BaseVO

## 注意事项

- 使用 `@FieldMapping(ignore = true)` 可以忽略不存在的字段
- 使用 `@FieldMapping(value = "actual_field_name")` 可以指定实际的数据库字段名
- 如果不使用注解，会使用默认的驼峰转下划线规则

## 示例

```java
// 完整的示例
@EqualsAndHashCode(callSuper = true)
@Data
public class ProductVO extends BaseVO<Long> {
    
    // 使用自定义字段映射
    @FieldMapping(value = "product_name")
    private String productName;
    
    @FieldMapping(value = "product_price")
    private BigDecimal price;
    
    // 忽略不存在的字段
    @FieldMapping(ignore = true)
    private String createUser;
    
    @FieldMapping(ignore = true)
    private String modifyUser;
    
    // 使用默认映射
    private String description; // 映射到 description
}
```
