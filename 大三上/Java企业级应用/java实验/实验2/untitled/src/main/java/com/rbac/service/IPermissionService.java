package com.rbac.service;

import com.rbac.model.Permission;

import java.util.List;

/**
 * Permission (权限) 的业务逻辑服务 (Service) 接口
 */
public interface IPermissionService {

    /**
     * 创建一个新权限
     * @param name 权限的唯一标识 (e.g., "PERM_USER_DELETE")
     * @param description 描述
     * @return 创建的权限
     * @throws Exception 如果权限名已存在
     */
    Permission createPermission(String name, String description) throws Exception;

    /**
     * 获取所有权限
     */
    List<Permission> getAllPermissions();

    /**
     * 删除一个权限
     */
    boolean deletePermission(int id) throws Exception;
}