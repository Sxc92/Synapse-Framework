package com.indigo.core.entity.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础VO类
 * 用于非分页查询的返回对象映射
 * 提供通用的字段和功能
 *
 * @author 史偕成
 * @date 2025/12/19
 */
@Data
public abstract class BaseVO implements Serializable {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 创建人ID
     */
    private Long createBy;
    
    /**
     * 更新人ID
     */
    private Long updateBy;
}
