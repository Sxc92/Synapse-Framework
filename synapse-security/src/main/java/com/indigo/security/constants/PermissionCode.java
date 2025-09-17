package com.indigo.security.constants;

/**
 * 权限编码常量类
 * 权限编码规范：模块:资源:操作
 * 
 * 示例：
 * - user:create    用户创建
 * - user:read      用户查看
 * - user:update    用户修改
 * - user:delete    用户删除
 * - user:export    用户导出
 * - user:import    用户导入
 * - order:approve  订单审批
 * - order:reject   订单拒绝
 * - finance:audit  财务审核
 * - report:view    报表查看
 *
 * @author 史偕成
 * @date 2025/01/09
 */
public class PermissionCode {

    /**
     * 用户管理权限
     */
    public static class User {
        public static final String CREATE = "user:create";
        public static final String READ = "user:read";
        public static final String UPDATE = "user:update";
        public static final String DELETE = "user:delete";
        public static final String EXPORT = "user:export";
        public static final String IMPORT = "user:import";
        public static final String RESET_PASSWORD = "user:reset-password";
        public static final String ASSIGN_ROLE = "user:assign-role";
        public static final String UNLOCK = "user:unlock";
        public static final String DISABLE = "user:disable";
        public static final String ENABLE = "user:enable";
        
        /**
         * 用户模块所有权限
         */
        public static final String ALL = "user:*";
    }

    /**
     * 角色管理权限
     */
    public static class Role {
        public static final String CREATE = "role:create";
        public static final String READ = "role:read";
        public static final String UPDATE = "role:update";
        public static final String DELETE = "role:delete";
        public static final String ASSIGN_PERMISSION = "role:assign-permission";
        public static final String ASSIGN_USER = "role:assign-user";
        
        /**
         * 角色模块所有权限
         */
        public static final String ALL = "role:*";
    }

    /**
     * 权限管理权限
     */
    public static class Permission {
        public static final String CREATE = "permission:create";
        public static final String READ = "permission:read";
        public static final String UPDATE = "permission:update";
        public static final String DELETE = "permission:delete";
        public static final String SYNC = "permission:sync";
        
        /**
         * 权限模块所有权限
         */
        public static final String ALL = "permission:*";
    }

    /**
     * 部门管理权限
     */
    public static class Department {
        public static final String CREATE = "department:create";
        public static final String READ = "department:read";
        public static final String UPDATE = "department:update";
        public static final String DELETE = "department:delete";
        public static final String MOVE = "department:move";
        public static final String MERGE = "department:merge";
        
        /**
         * 部门模块所有权限
         */
        public static final String ALL = "department:*";
    }

    /**
     * 职级管理权限
     */
    public static class Position {
        public static final String CREATE = "position:create";
        public static final String READ = "position:read";
        public static final String UPDATE = "position:update";
        public static final String DELETE = "position:delete";
        public static final String ASSIGN_USER = "position:assign-user";
        
        /**
         * 职级模块所有权限
         */
        public static final String ALL = "position:*";
    }



    /**
     * 报表管理权限
     */
    public static class Report {
        public static final String VIEW = "report:view";
        public static final String EXPORT = "report:export";
        public static final String CREATE = "report:create";
        public static final String UPDATE = "report:update";
        public static final String DELETE = "report:delete";
        public static final String SHARE = "report:share";
        
        /**
         * 报表模块所有权限
         */
        public static final String ALL = "report:*";
    }

    /**
     * 系统管理权限
     */
    public static class System {
        public static final String CONFIG = "system:config";
        public static final String LOG = "system:log";
        public static final String MONITOR = "system:monitor";
        public static final String BACKUP = "system:backup";
        public static final String RESTORE = "system:restore";
        public static final String CLEANUP = "system:cleanup";
        
        /**
         * 系统模块所有权限
         */
        public static final String ALL = "system:*";
    }

    /**
     * 审计管理权限
     */
    public static class Audit {
        public static final String VIEW = "audit:view";
        public static final String EXPORT = "audit:export";
        public static final String DELETE = "audit:delete";
        public static final String ARCHIVE = "audit:archive";
        
        /**
         * 审计模块所有权限
         */
        public static final String ALL = "audit:*";
    }

    /**
     * 通知管理权限
     */
    public static class Notification {
        public static final String SEND = "notification:send";
        public static final String READ = "notification:read";
        public static final String DELETE = "notification:delete";
        public static final String CONFIG = "notification:config";
        
        /**
         * 通知模块所有权限
         */
        public static final String ALL = "notification:*";
    }

    /**
     * 工作流管理权限
     */
    public static class Workflow {
        public static final String CREATE = "workflow:create";
        public static final String READ = "workflow:read";
        public static final String UPDATE = "workflow:update";
        public static final String DELETE = "workflow:delete";
        public static final String DEPLOY = "workflow:deploy";
        public static final String EXECUTE = "workflow:execute";
        public static final String APPROVE = "workflow:approve";
        public static final String REJECT = "workflow:reject";
        
        /**
         * 工作流模块所有权限
         */
        public static final String ALL = "workflow:*";
    }

    /**
     * 检查权限编码是否匹配
     * 支持通配符匹配
     *
     * @param rulePermissionCode 规则权限编码
     * @param targetPermissionCode 目标权限编码
     * @return 是否匹配
     */
    public static boolean matches(String rulePermissionCode, String targetPermissionCode) {
        if (rulePermissionCode == null || targetPermissionCode == null) {
            return false;
        }
        
        // 支持通配符匹配
        if (rulePermissionCode.endsWith(":*")) {
            String module = rulePermissionCode.substring(0, rulePermissionCode.length() - 2);
            return targetPermissionCode.startsWith(module + ":");
        }
        
        // 精确匹配
        return rulePermissionCode.equals(targetPermissionCode);
    }

    /**
     * 检查是否为通配符权限
     *
     * @param permissionCode 权限编码
     * @return 是否为通配符权限
     */
    public static boolean isWildcard(String permissionCode) {
        return permissionCode != null && permissionCode.endsWith(":*");
    }

    /**
     * 从权限编码中提取模块名
     *
     * @param permissionCode 权限编码
     * @return 模块名
     */
    public static String extractModule(String permissionCode) {
        if (permissionCode == null || !permissionCode.contains(":")) {
            return null;
        }
        return permissionCode.substring(0, permissionCode.indexOf(":"));
    }

    /**
     * 从权限编码中提取操作名
     *
     * @param permissionCode 权限编码
     * @return 操作名
     */
    public static String extractOperation(String permissionCode) {
        if (permissionCode == null || !permissionCode.contains(":")) {
            return null;
        }
        return permissionCode.substring(permissionCode.indexOf(":") + 1);
    }
}
