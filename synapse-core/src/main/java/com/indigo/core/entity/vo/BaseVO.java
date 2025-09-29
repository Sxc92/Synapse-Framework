package com.indigo.core.entity.vo;

import com.indigo.core.annotation.FieldMapping;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础VO类
 * 用于非分页查询的返回对象映射
 * 提供通用的字段和功能
 * 
 * <h3>字段映射说明：</h3>
 * <p>所有字段都使用@FieldMapping注解，支持灵活的数据库字段映射：</p>
 * <ul>
 *   <li>如果数据库字段名与VO字段名不同，可以通过@FieldMapping(value="实际字段名")指定</li>
 *   <li>如果数据库中没有对应字段，可以使用@FieldMapping(ignore=true)忽略</li>
 *   <li>如果使用默认的驼峰转下划线规则，可以不使用注解</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 如果数据库字段名不同
 * @FieldMapping(value = "created_at")
 * private LocalDateTime createTime;
 * 
 * // 如果数据库中没有对应字段
 * @FieldMapping(ignore = true)
 * private T createUser;
 * 
 * // 使用默认映射（驼峰转下划线）
 * private LocalDateTime modifyTime; // 映射到 modify_time
 * }</pre>
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public abstract class BaseVO<T> implements Serializable {
    
    /**
     * 主键ID
     * 默认映射到 id 字段
     */
    private T id;
    
    /**
     * 创建时间
     * 默认映射到 create_time 字段
     * 如果数据库字段名不同，请使用 @FieldMapping(value="实际字段名")
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     * 默认映射到 modify_time 字段
     * 如果数据库字段名不同，请使用 @FieldMapping(value="实际字段名")
     */
    private LocalDateTime modifyTime;
    
    /**
     * 创建人ID
     * 默认映射到 create_user 字段
     * 如果数据库字段名不同，请使用 @FieldMapping(value="实际字段名")
     */
    private T createUser;
    
    /**
     * 更新人ID
     * 默认映射到 modify_user 字段
     * 如果数据库字段名不同，请使用 @FieldMapping(value="实际字段名")
     */
    private T modifyUser;
}
