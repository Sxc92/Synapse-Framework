package com.indigo.core.entity.vo;

import com.indigo.core.annotation.FieldMapping;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 标准BaseVO
 * 使用标准的字段命名：create_time, modify_time, create_user, modify_user
 * 
 * @author 史偕成
 * @date 2025/12/20
 */
@Data
public abstract class StandardBaseVO<T> implements Serializable {
    
    private T id;
    
    @FieldMapping(value = "create_time")
    private LocalDateTime createTime;
    
    @FieldMapping(value = "modify_time")
    private LocalDateTime modifyTime;
    
    @FieldMapping(value = "create_user")
    private T createUser;
    
    @FieldMapping(value = "modify_user")
    private T modifyUser;
}
