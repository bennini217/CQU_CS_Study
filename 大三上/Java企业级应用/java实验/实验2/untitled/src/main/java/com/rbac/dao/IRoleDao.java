package com.rbac.dao;

import com.rbac.model.Permission;
import com.rbac.model.Role;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Role (角色) 的数据访问对象 (DAO) 接口
 * * 定义了所有与 Role 相关的数据库操作
 */
public interface IRoleDao {

    // --- 1. role 表的基本操作 ---

    Role createRole(Role role) throws SQLException;

    Optional<Role> findRoleById(int id) throws SQLException;

    Optional<Role> findRoleByName(String name) throws SQLException;

    boolean deleteRole(int id) throws SQLException;

    List<Role> findAllRoles() throws SQLException;

    // --- 2. role_permissions (角色-权限) 关系表操作 ---

    /**
     * 给角色分配一个权限
     */
    boolean addPermissionToRole(int roleId, int permissionId) throws SQLException;

    /**
     * 移除角色的一个权限
     */
    boolean removePermissionFromRole(int roleId, int permissionId) throws SQLException;

    /**
     * 查找一个角色【直接】拥有的所有权限
     */
    Set<Permission> findPermissionsByRoleId(int roleId) throws SQLException;

    // --- 3. role_hierarchy (角色继承) 【RBAC1 核心】---

    /**
     * 设置一个角色继承另一个角色 (e.g., "经理" 继承 "员工")
     * @param seniorRoleId "经理" (高级)
     * @param juniorRoleId "员工" (低级)
     */
    boolean addRoleInheritance(int seniorRoleId, int juniorRoleId) throws SQLException;

    /**
     * 移除角色继承关系
     */
    boolean removeRoleInheritance(int seniorRoleId, int juniorRoleId) throws SQLException;

    /**
     * 查找一个角色【直接】继承的 (下一级) 所有角色
     */
    Set<Role> findJuniorRoles(int seniorRoleId) throws SQLException;

    /**
     * 查找一个角色【直接】被哪些角色继承 (上一级)
     */
    Set<Role> findSeniorRoles(int juniorRoleId) throws SQLException;
}