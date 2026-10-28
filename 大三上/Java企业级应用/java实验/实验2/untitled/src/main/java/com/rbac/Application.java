package com.rbac;

import com.rbac.dao.impl.PermissionDaoImpl;
import com.rbac.dao.impl.RoleDaoImpl;
import com.rbac.model.Permission;
import com.rbac.model.Role;
import com.rbac.model.User;
import com.rbac.service.*;
import com.rbac.service.impl.*;
import com.rbac.ui.MainMenu;
import com.rbac.util.DatabaseUtil;
import com.rbac.util.LoggerUtil;

import java.util.Optional;

public class Application {

    /**
     * 【已升级】: 智能的系统数据初始化/迁移
     * * 检查并创建核心权限
     * * 检查并创建 SuperAdmin 角色
     * * 检查并创建 admin 用户
     * * 检查并确保 SuperAdmin 拥有所有核心权限
     */
    private static void initializeSystemData() {
        try {
            // 拿到所有 "经理"
            IUserService userService = new UserServiceImpl();
            IRoleService roleService = new RoleServiceImpl();
            IPermissionService permService = new PermissionServiceImpl();
            IAuthService authService = new AuthServiceImpl();

            LoggerUtil.info("正在检查并初始化/迁移系统核心数据...");

            // --- 1. 确保核心权限存在 ---
            // (我们使用一个辅助方法，让代码更干净)
            Permission pUserMgmt = ensurePermissionExists(permService, "PERM_USER_MGMT", "用户管理菜单");
            Permission pRoleMgmt = ensurePermissionExists(permService, "PERM_ROLE_MGMT", "角色管理菜单");
            Permission pPermMgmt = ensurePermissionExists(permService, "PERM_PERM_MGMT", "权限管理菜单");

            // --- 【新增】: 确保 "操作权限" 存在 ---
            Permission pUserAssignRole = ensurePermissionExists(permService, "PERM_USER_ASSIGN_ROLE", "为用户分配角色");
            Permission pUserRemoveRole = ensurePermissionExists(permService, "PERM_USER_REMOVE_ROLE", "移除用户角色");
            Permission pRoleAssignPerm = ensurePermissionExists(permService, "PERM_ROLE_ASSIGN_PERM", "为角色分配权限");
            Permission pRoleSetInherit = ensurePermissionExists(permService, "PERM_ROLE_SET_INHERIT", "设置角色继承");
            Permission pUserList = ensurePermissionExists(permService, "PERM_USER_LIST", "查看用户列表"); // 升级版 UI 需要

            // --- 2. 确保 SuperAdmin 角色存在 ---
            Role superAdminRole;
            Optional<Role> roleOpt = new RoleDaoImpl().findRoleByName("SuperAdmin"); // (不推荐直接用 DAO，但这里最快)
            if (roleOpt.isEmpty()) {
                superAdminRole = roleService.createRole("SuperAdmin", "系统超级管理员");
                LoggerUtil.info("创建了 SuperAdmin 角色。");
            } else {
                superAdminRole = roleOpt.get();
            }

            // --- 3. 确保 SuperAdmin 拥有所有权限 (关键！) ---
            // (这会自动跳过已有的权限)
            roleService.assignPermissionToRole(superAdminRole.getId(), pUserMgmt.getId());
            roleService.assignPermissionToRole(superAdminRole.getId(), pRoleMgmt.getId());
            roleService.assignPermissionToRole(superAdminRole.getId(), pPermMgmt.getId());

            // --- 【新增】: 确保 SuperAdmin 拥有新的 "操作权限" ---
            roleService.assignPermissionToRole(superAdminRole.getId(), pUserAssignRole.getId());
            roleService.assignPermissionToRole(superAdminRole.getId(), pUserRemoveRole.getId());
            roleService.assignPermissionToRole(superAdminRole.getId(), pRoleAssignPerm.getId());
            roleService.assignPermissionToRole(superAdminRole.getId(), pRoleSetInherit.getId());
            roleService.assignPermissionToRole(superAdminRole.getId(), pUserList.getId());


            // --- 4. 确保 admin 用户存在 ---
            if (userService.getUserByUsername("admin").isEmpty()) {
                User adminUser = userService.createUser("admin", "admin");
                authService.assignRoleToUser(adminUser.getId(), superAdminRole.getId());

                System.out.println("=========================================");
                System.out.println("  系统首次初始化成功！");
                System.out.println("  已创建超级管理员: admin / admin");
                System.out.println("=========================================");
                LoggerUtil.audit("系统初始化成功, 创建了 admin 用户和 SuperAdmin 角色。");
            }

            LoggerUtil.info("核心数据检查完毕，系统准备就绪。");

        } catch (Exception e) {
            System.err.println("!!! 系统初始化数据失败 !!!");
            e.printStackTrace();
            System.exit(1); // 启动失败
        }
    }

    /**
     * 【新增】: 辅助方法，检查权限是否存在，不存在则创建
     */
    private static Permission ensurePermissionExists(IPermissionService permService, String name, String description) throws Exception {
        Optional<Permission> permOpt = new PermissionDaoImpl().findPermissionByName(name); // (不推荐用DAO，但...)

        if (permOpt.isPresent()) {
            return permOpt.get(); // 存在，直接返回
        } else {
            // 不存在，创建它
            LoggerUtil.info("检测到新权限缺失, 正在创建: " + name);
            return permService.createPermission(name, description);
        }
    }


    /**
     * 【修改】: 我们的 main 方法 (无变化, 但现在更安全了)
     */
    public static void main(String[] args) {

        // 1. 初始化数据库 (创建表)
        DatabaseUtil.initializeDatabase();

        // 2. 【已升级】初始化/迁移系统数据
        initializeSystemData();

        // 3. 记录日志
        LoggerUtil.info("RBAC 权限管理系统已启动。");

        // 4. 启动我们的 UI
        MainMenu menu = new MainMenu();
        menu.run();
    }
}