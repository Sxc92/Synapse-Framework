package com.indigo.core.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 用户上下文信息
 * 用于存储JWT token中的用户信息
 * 实现 Serializable 接口以支持 Redis 序列化
 *
 * @author 史偕成
 * @date 2025/03/21
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 线程本地变量，用于存储当前用户上下文
     */
    private static final ThreadLocal<UserContext> USER_CONTEXT_HOLDER = new ThreadLocal<>();


    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户名
     */
    private String account;

    /**
     * 用户昵称
     */
    private String realName;

    /**
     * 电子邮箱
     */
    private String email;

    /**
     * 手机号码
     */
    private String mobile;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 部门ID
     */
    private String deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 部门路径
     */
    private String deptPath;

    /**
     * 职级ID
     */
    private String positionId;

    /**
     * 职级名称
     */
    private String positionName;

    /**
     * 职级等级
     */
    private Integer positionLevel;

    /**
     * 用户部门职级信息列表
     */
    private List<UserDeptPositionInfo> deptPositions;

    /**
     * 角色列表
     */
    private List<String> roles;

    /**
     * 权限Id
     */
    private List<String> permissions;

    /**
     * 最后访问时间
     */
    private Long lastAccessTime;

    /**
     * 登录时间
     */
    private Long loginTime;

    /**
     * 访问令牌
     */
    private String token;

    /**
     * Token剩余时间（秒）
     */
    private Long tokenRemaining;

    /**
     * 获取当前用户上下文
     *
     * @return 当前用户上下文
     */
    public static UserContext getCurrentUser() {
        return USER_CONTEXT_HOLDER.get();
    }

    /**
     * 设置当前用户上下文
     *
     * @param userContext 用户上下文
     */
    public static void setCurrentUser(UserContext userContext) {
        USER_CONTEXT_HOLDER.set(userContext);
    }

    /**
     * 清除当前用户上下文
     */
    public static void clearCurrentUser() {
        USER_CONTEXT_HOLDER.remove();
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID，如果没有当前用户则返回null
     */
    public static String getCurrentUserId() {
        UserContext userContext = getCurrentUser();
        return userContext != null ? userContext.getUserId() : null;
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名，如果没有当前用户则返回null
     */
    public static String getCurrentAccount() {
        UserContext userContext = getCurrentUser();
        return userContext != null ? userContext.getAccount() : null;
    }

    /**
     * 获取当前用户部门ID
     *
     * @return 部门ID，如果没有当前用户则返回null
     */
    public static String getCurrentDeptId() {
        UserContext userContext = getCurrentUser();
        return userContext != null ? userContext.getDeptId() : null;
    }

    /**
     * 获取当前用户职级ID
     *
     * @return 职级ID，如果没有当前用户则返回null
     */
    public static String getCurrentPositionId() {
        UserContext userContext = getCurrentUser();
        return userContext != null ? userContext.getPositionId() : null;
    }

    /**
     * 获取当前用户职级等级
     *
     * @return 职级等级，如果没有当前用户则返回null
     */
    public static Integer getCurrentPositionLevel() {
        UserContext userContext = getCurrentUser();
        return userContext != null ? userContext.getPositionLevel() : null;
    }

    /**
     * 用户部门职级信息
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserDeptPositionInfo implements Serializable {

        private static final long serialVersionUID = 1L;
        /**
         * 部门ID
         */
        private String deptId;

        /**
         * 部门名称
         */
        private String deptName;

        /**
         * 部门路径
         */
        private String deptPath;

        /**
         * 职级ID
         */
        private String positionId;

        /**
         * 职级名称
         */
        private String positionName;

        /**
         * 职级等级
         */
        private Integer level;

        /**
         * 是否为主部门职级
         */
        private Boolean isPrimary;

        /**
         * 开始时间
         */
        private String startDate;

        /**
         * 结束时间
         */
        private String endDate;

        /**
         * 状态
         */
        private Integer status;
    }
}