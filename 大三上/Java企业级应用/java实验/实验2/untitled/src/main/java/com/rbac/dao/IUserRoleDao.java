package com.rbac.dao;

import com.rbac.model.Role;

import java.sql.SQLException;
import java.util.Set;

/**
 * User-Role 关系表 (user_roles) 的数据访问对象 (DAO) 接口
 * * 专门管理用户和角色的多对多映射
 */
public interface IUserRoleDao {

    /**
     * 给一个用户分配一个角色
     * @param userId 用户 ID
     * @param roleId 角色 ID
     */
    boolean assignRoleToUser(int userId, int roleId) throws SQLException;

    /**
     * 移除一个用户的角色
     * @param userId 用户 ID
     * @param roleId 角色 ID
     */
    boolean removeRoleFromUser(int userId, int roleId) throws SQLException;

    /**
     * 查找一个用户【直接】拥有的所有角色
     * @param userId 用户 ID
     * @return 包含所有角色的 Set
     */
    Set<Role> findRolesByUserId(int userId) throws SQLException;
}