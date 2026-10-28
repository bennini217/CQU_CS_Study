package com.rbac.service.impl;

import com.rbac.dao.IRoleDao;
import com.rbac.dao.IUserDao;
import com.rbac.dao.IUserRoleDao;
import com.rbac.dao.impl.RoleDaoImpl;
import com.rbac.dao.impl.UserDaoImpl;
import com.rbac.dao.impl.UserRoleDaoImpl;
import com.rbac.model.Permission;
import com.rbac.model.Role;
import com.rbac.model.User;
import com.rbac.service.IAuthService;
import com.rbac.service.IRoleService;
import com.rbac.service.IUserService;
import com.rbac.util.LoggerUtil;
import com.rbac.util.PasswordUtil;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * IAuthService 接口的实现 (第3版 - 修复了业务逻辑校验)
 */
public class AuthServiceImpl implements IAuthService {

    // "安全总管" 需要指挥其他所有经理和工人
    private final IUserService userService;
    private final IRoleService roleService;
    private final IUserRoleDao userRoleDao;
    private final IUserDao userDao;
    private final IRoleDao roleDao;


    public AuthServiceImpl() {
        this.userService = new UserServiceImpl();
        this.roleService = new RoleServiceImpl();
        this.userRoleDao = new UserRoleDaoImpl();
        this.userDao = new UserDaoImpl();
        this.roleDao = new RoleDaoImpl();
    }

    /**
     * 实现 "登录" 逻辑
     */
    @Override
    public User login(String username, String password) throws Exception {
        Optional<User> userOpt = userService.getUserByUsername(username);

        if (userOpt.isEmpty()) {
            throw new Exception("登录失败：用户名或密码错误。");
        }

        User user = userOpt.get();

        if (PasswordUtil.checkPassword(password, user.getPasswordHash())) {
            LoggerUtil.audit("用户登录成功: " + username);
            return user;
        } else {
            LoggerUtil.audit("用户登录失败 (密码错误): " + username);
            throw new Exception("登录失败：用户名或密码错误。");
        }
    }

    /**
     * 实现 "权限校验" 逻辑
     */
    @Override
    public boolean checkPermission(User user, String permissionName) {
        if (user == null || permissionName == null) {
            return false;
        }

        try {
            Set<Role> directRoles = userRoleDao.findRolesByUserId(user.getId());
            Set<Permission> allPermissions = new HashSet<>();

            for (Role role : directRoles) {
                Set<Permission> permsForThisRole = roleService.getAllPermissionsForRole(role.getId());
                allPermissions.addAll(permsForThisRole);
            }

            // 【Java 8 Lambda 优化】: 检查权限是否存在
            return allPermissions.stream()
                    .anyMatch(p -> p.getName().equals(permissionName));

        } catch (SQLException e) {
            LoggerUtil.error("检查权限时数据库异常 (用户: " + user.getUsername() + "): " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * 【已修正 - 第3版】
     * 业务：给用户分配角色
     */
    @Override
    public void assignRoleToUser(int userId, int roleId) throws Exception {

        try {
            // 1. 检查用户是否存在
            if (userDao.findUserById(userId).isEmpty()) {
                throw new Exception("分配失败：用户 ID " + userId + " 不存在。");
            }

            // 2. 检查角色是否存在
            if (roleDao.findRoleById(roleId).isEmpty()) {
                throw new Exception("分配失败：角色 ID " + roleId + " 不存在。");
            }

            // 3. 【新增】检查用户是否已拥有该角色
            Set<Role> currentRoles = userRoleDao.findRolesByUserId(userId);
            // Java 8 Lambda 写法
            boolean hasRole = currentRoles.stream().anyMatch(r -> r.getId() == roleId);
            if (hasRole) {
                throw new Exception("分配失败：用户 " + userId + " 已经拥有角色 " + roleId);
            }

        } catch (SQLException e) {
            LoggerUtil.error("分配角色前的校验查询失败: " + e.getMessage(), e);
            throw new Exception("数据库查询错误，分配失败。");
        } catch (Exception e) {
            // 重新抛出我们的业务异常 (e.g., "用户不存在", "已拥有")
            throw e;
        }

        // 4. 校验全部通过，才执行操作
        try {
            userRoleDao.assignRoleToUser(userId, roleId);
        } catch (SQLException e) {
            LoggerUtil.error("分配角色给用户时数据库异常: " + e.getMessage(), e);
            throw new Exception("数据库错误，分配角色失败。");
        }
    }

    /**
     * 【已修正 - 第3版】
     * 业务：移除用户的角色
     */
    @Override
    public void removeRoleFromUser(int userId, int roleId) throws Exception {

        try {
            // 1. 检查用户是否存在
            if (userDao.findUserById(userId).isEmpty()) {
                throw new Exception("移除失败：用户 ID " + userId + " 不存在。");
            }

            // 2. 检查角色是否存在
            if (roleDao.findRoleById(roleId).isEmpty()) {
                throw new Exception("移除失败：角色 ID " + roleId + " 不存在。");
            }

            // 3. 【新增】检查用户是否真的拥有该角色
            Set<Role> currentRoles = userRoleDao.findRolesByUserId(userId);
            boolean hasRole = currentRoles.stream().anyMatch(r -> r.getId() == roleId);

            if (!hasRole) {
                throw new Exception("移除失败：用户 " + userId + " 并不拥有角色 " + roleId);
            }

        } catch (SQLException e) {
            LoggerUtil.error("移除角色前的校验查询失败: " + e.getMessage(), e);
            throw new Exception("数据库查询错误，移除失败。");
        } catch (Exception e) {
            // 重新抛出业务异常
            throw e;
        }

        // 4. 校验全部通过，执行操作
        try {
            boolean success = userRoleDao.removeRoleFromUser(userId, roleId);
            if (!success) {
                // 这一行理论上不应该被触发了，因为我们前面已经校验过了
                throw new Exception("数据库错误，移除失败。");
            }
        } catch (SQLException e) {
            LoggerUtil.error("移除用户角色时数据库异常: " + e.getMessage(), e);
            throw new Exception("数据库错误，移除角色失败。");
        }
    }
}