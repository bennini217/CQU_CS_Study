package com.rbac.service;

import com.rbac.model.User;

/**
 * 认证 (Authentication) 和 授权 (Authorization) 服务接口
 * * 系统的 "安全总管"
 */
public interface IAuthService {

    /**
     * 认证 (Authentication): 用户登录
     * @param username 明文用户名
     * @param password 明文密码
     * @return 登录成功的 User 对象
     * @throws Exception 如果用户名或密码错误
     */
    User login(String username, String password) throws Exception;

    /**
     * 授权 (Authorization): 检查权限
     * * 【项目最核心的方法】 *
     * 检查一个用户是否拥有执行某个特定操作的权限
     *
     * @param user           登录的用户对象
     * @param permissionName 权限的唯一标识 (e.g., "PERM_USER_DELETE")
     * @return true 如果有权限, false 如果没有
     */
    boolean checkPermission(User user, String permissionName);

    /**
     * 业务：给用户分配角色
     * @param userId 用户 ID
     * @param roleId 角色 ID
     * @throws Exception 如果用户或角色不存在
     */
    void assignRoleToUser(int userId, int roleId) throws Exception;

    /**
     * 业务：移除用户的角色
     * @param userId 用户 ID
     * @param roleId 角色 ID
     */
    void removeRoleFromUser(int userId, int roleId) throws Exception;
}