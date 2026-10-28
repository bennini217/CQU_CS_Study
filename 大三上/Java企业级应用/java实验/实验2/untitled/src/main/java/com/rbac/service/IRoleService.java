package com.rbac.service;

import com.rbac.model.Permission;
import com.rbac.model.Role;

import java.util.List;
import java.util.Set;

/**
 * Role (角色) 的业务逻辑服务 (Service) 接口
 * * 定义了所有与角色相关的 "业务操作"
 */
public interface IRoleService {

    /**
     * 创建一个新角色
     * @param name 角色名
     * @param description 描述
     * @return 创建的角色
     * @throws Exception 如果角色名已存在
     */
    Role createRole(String name, String description) throws Exception;

    /**
     * 删除角色
     * @param roleId 角色 ID
     * @return true 如果成功
     * @throws Exception 如果角色不存在或删除失败
     */
    boolean deleteRole(int roleId) throws Exception;

    /**
     * 获取所有角色
     * @return 角色列表
     */
    List<Role> getAllRoles();

    /**
     * 给角色分配一个权限
     * @param roleId 角色 ID
     * @param permissionId 权限 ID
     * @throws Exception 如果角色或权限不存在
     */
    void assignPermissionToRole(int roleId, int permissionId) throws Exception;

    /**
     * 设置角色继承关系 (e.g., "经理" 继承 "员工")
     * @param seniorRoleId "经理" (高级) ID
     * @param juniorRoleId "员工" (低级) ID
     * @throws Exception 如果角色不存在，或产生循环继承 (A->B, B->A)
     */
    void setRoleInheritance(int seniorRoleId, int juniorRoleId) throws Exception;

    /**
     * 【RBAC1 核心】
     * 获取一个角色所拥有的【所有】权限
     * (包括它自己直接拥有的，以及它所有下级角色间接拥有的)
     * @param roleId 角色 ID
     * @return 一个包含所有权限的 Set (自动去重)
     */
    Set<Permission> getAllPermissionsForRole(int roleId);
}