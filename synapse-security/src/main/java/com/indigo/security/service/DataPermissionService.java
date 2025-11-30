package com.indigo.security.service;

/**
 * 数据权限服务接口
 * 
 * <p><b>注意：</b>此功能已暂时注释，待业务完整后扩展
 * 
 * @author 史偕成
 * @date 2025/01/09
 */
// TODO: 待业务完整后恢复数据权限功能
/*
import com.indigo.security.model.DataPermissionRule;
import com.indigo.core.context.UserContext;

import java.util.List;
import java.util.Map;

public interface DataPermissionService {

    Long addRule(DataPermissionRule rule);

    void updateRule(DataPermissionRule rule);

    void deleteRule(String ruleId);

    DataPermissionRule getRule(String ruleId);

    List<DataPermissionRule> getUserRules(String userId);

    List<DataPermissionRule> getRoleRules(String roleId);

    List<DataPermissionRule> getDepartmentRules(String deptId);

    boolean hasPermission(UserContext user, String resourceType, DataPermissionRule.PermissionType permissionType);

    String getDataScope(UserContext user, String resourceType);

    Map<String, Object> getCustomDataScope(UserContext user, String resourceType);

    Map<String, Boolean> batchCheckPermissions(UserContext user, List<String> resourceTypes, 
                                             DataPermissionRule.PermissionType permissionType);
} 
*/
