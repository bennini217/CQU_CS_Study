package com.rbac.dao;

import com.rbac.model.Permission;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Permission (权限) 的数据访问对象 (DAO) 接口
 */
public interface IPermissionDao {

    /**
     * 创建一个新权限
     * @param permission (name, description)
     * @return 包含 id 的 Permission 对象
     */
    Permission createPermission(Permission permission) throws SQLException;

    /**
     * 根据权限名 (唯一标识) 查找
     * @param name e.g., "PERM_USER_DELETE"
     */
    Optional<Permission> findPermissionByName(String name) throws SQLException;

    /**
     * 根据 ID 查找
     */
    Optional<Permission> findPermissionById(int id) throws SQLException;

    /**
     * 查找所有权限
     */
    List<Permission> findAllPermissions() throws SQLException;

    /**
     * 删除一个权限
     * @param id 权限 ID
     */
    boolean deletePermission(int id) throws SQLException;
}