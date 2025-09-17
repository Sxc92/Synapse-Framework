package com.indigo.security.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 数据权限规则模型
 * 用于定义用户或角色对数据的访问规则
 *
 * @author 史偕成
 * @date 2025/01/09
 */
@Data
@Builder
public class DataPermissionRule {

    /**
     * 规则ID
     */
    private Long ruleId;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 权限编码（支持通配符）
     * 示例：
     * - user:*          所有用户权限
     * - user:read       用户查看权限
     * - order:approve   订单审批权限
     * - finance:*       所有财务权限
     */
    private String permissionCode;

    /**
     * 规则类型
     */
    private RuleType ruleType;

    /**
     * 主体类型（用户/角色/部门）
     */
    private SubjectType subjectType;

    /**
     * 主体ID（用户ID/角色ID/部门ID）
     */
    private Long subjectId;

    /**
     * 资源类型（表名/实体名）
     */
    private String resourceType;

    /**
     * 权限类型（读/写/删除等）
     */
    private List<PermissionType> permissionTypes;

    /**
     * 数据范围类型
     */
    private DataScopeType dataScopeType;

    /**
     * 自定义数据范围（SQL条件）
     */
    private String customScope;

    /**
     * 数据范围SQL表达式
     * 支持动态参数：
     * - #{userId}       当前用户ID
     * - #{deptId}       当前用户部门ID
     * - #{positionId}   当前用户职级ID
     * - #{deptPath}     当前用户部门路径
     * - #{positionLevel} 当前用户职级等级
     */
    private String dataScopeExpression;

    /**
     * 规则条件（JSON格式）
     */
    private Map<String, Object> conditions;

    /**
     * 规则优先级（数字越小优先级越高）
     */
    private Integer priority;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 规则类型枚举
     */
    public enum RuleType {
        /**
         * 允许访问
         */
        ALLOW,
        /**
         * 拒绝访问
         */
        DENY
    }

    /**
     * 主体类型枚举
     */
    public enum SubjectType {
        /**
         * 用户
         */
        USER,
        /**
         * 角色
         */
        ROLE,
        /**
         * 部门
         */
        DEPARTMENT,
        /**
         * 职级
         */
        POSITION,
        /**
         * 部门职级组合
         */
        DEPT_POSITION
    }

    /**
     * 权限类型枚举
     */
    public enum PermissionType {
        /**
         * 读取权限
         */
        READ,
        /**
         * 写入权限
         */
        WRITE,
        /**
         * 删除权限
         */
        DELETE,
        /**
         * 全部权限
         */
        ALL
    }

    /**
     * 数据范围类型枚举
     */
    public enum DataScopeType {
        /**
         * 全部数据
         */
        ALL,
        /**
         * 本部门数据
         */
        DEPARTMENT,
        /**
         * 本部门及下级部门数据
         */
        DEPARTMENT_AND_BELOW,
        /**
         * 本职级及下级职级数据
         */
        POSITION_AND_BELOW,
        /**
         * 仅本人数据
         */
        PERSONAL,
        /**
         * 自定义数据范围
         */
        CUSTOM
    }
} 