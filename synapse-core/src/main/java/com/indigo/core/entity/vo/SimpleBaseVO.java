package com.indigo.core.entity.vo;

import com.indigo.core.annotation.FieldMapping;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 简化BaseVO
 * 只包含必要的字段，其他字段通过@FieldMapping(ignore=true)忽略
 * 
 * @author 史偕成
 * @date 2025/12/20
 */
@Data
public abstract class SimpleBaseVO<T> implements Serializable {
    
    private T id;
    
    @FieldMapping(value = "created_at")
    private LocalDateTime createTime;
    
    @FieldMapping(value = "updated_at")
    private LocalDateTime modifyTime;
    
    // 如果数据库中没有这些字段，可以忽略
    @FieldMapping(ignore = true)
    private T createUser;
    
    @FieldMapping(ignore = true)
    private T modifyUser;
}
