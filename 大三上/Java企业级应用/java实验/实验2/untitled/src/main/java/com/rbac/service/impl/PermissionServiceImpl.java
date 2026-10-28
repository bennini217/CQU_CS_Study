package com.rbac.service.impl;

import com.rbac.dao.IPermissionDao;
import com.rbac.dao.impl.PermissionDaoImpl;
import com.rbac.model.Permission;
import com.rbac.service.IPermissionService;
import com.rbac.util.LoggerUtil;

import java.sql.SQLException;
import java.util.List;

/**
 * IPermissionService 接口的实现
 */
public class PermissionServiceImpl implements IPermissionService {

    // 经理需要工人
    private final IPermissionDao permissionDao;

    public PermissionServiceImpl() {
        this.permissionDao = new PermissionDaoImpl();
    }

    @Override
    public Permission createPermission(String name, String description) throws Exception {
        // 1. 业务规则: 检查权限名是否已存在
        if (permissionDao.findPermissionByName(name).isPresent()) {
            throw new Exception("创建失败：权限名 '" + name + "' 已被占用。");
        }

        Permission newPermission = new Permission();
        newPermission.setName(name);
        newPermission.setDescription(description);

        try {
            return permissionDao.createPermission(newPermission);
        } catch (SQLException e) {
            LoggerUtil.error("创建权限时数据库异常: " + e.getMessage(), e);
            throw new Exception("数据库错误，创建权限失败。");
        }
    }

    @Override
    public List<Permission> getAllPermissions() {
        try {
            return permissionDao.findAllPermissions();
        } catch (SQLException e) {
            LoggerUtil.error("获取所有权限时数据库异常: " + e.getMessage(), e);
            return List.of(); // 返回空列表
        }
    }

    @Override
    public boolean deletePermission(int id) throws Exception {
        try {
            return permissionDao.deletePermission(id);
        } catch (SQLException e) {
            LoggerUtil.error("删除权限时数据库异常: " + e.getMessage(), e);
            throw new Exception("数据库错误，删除权限失败。");
        }
    }
}