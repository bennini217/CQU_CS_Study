package com.rbac.ui;

import com.rbac.model.Permission;
import com.rbac.model.Role;
import com.rbac.model.User;
import com.rbac.service.*;
import com.rbac.service.impl.*;
import com.rbac.util.LoggerUtil;

import java.sql.SQLException; // 【新增】
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.HashSet;
import com.rbac.dao.impl.UserRoleDaoImpl; // 【新增】

/**
 * 命令行主菜单和 UI 逻辑
 * 【已升级】: 增加了 "操作权限" 检查，满足实验要求
 */
public class MainMenu {

    // --- (服务和 Scanner... 无变化) ---
    private final IUserService userService;
    private final IRoleService roleService;
    private final IPermissionService permissionService;
    private final IAuthService authService;
    private final Scanner scanner;
    private User currentUser = null;

    public MainMenu() {
        this.userService = new UserServiceImpl();
        this.roleService = new RoleServiceImpl();
        this.permissionService = new PermissionServiceImpl();
        this.authService = new AuthServiceImpl();
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        while (true) {
            if (currentUser == null) {
                showLoginMenu();
            } else {
                showMainMenu();
            }
        }
    }

    // --- (登录/注册... 无变化) ---
    private void showLoginMenu() {
        System.out.println("\n--- 登录菜单 ---");
        System.out.println("1. 登录");
        System.out.println("2. 注册新用户");
        System.out.println("0. 退出系统");
        System.out.print("请输入选项: ");

        int choice = readIntInput();
        scanner.nextLine();

        switch (choice) {
            case 1: handleLogin(); break;
            case 2: handleRegister(); break;
            case 0:
                System.out.println("感谢使用，再见！");
                System.exit(0);
            default:
                System.out.println("无效选项，请重试。");
        }
    }

    private void handleLogin() {
        try {
            System.out.print("请输入用户名: ");
            String username = scanner.nextLine();
            System.out.print("请输入密码: ");
            String password = scanner.nextLine();

            this.currentUser = authService.login(username, password);
            System.out.println("登录成功！欢迎您, " + currentUser.getUsername());
        } catch (Exception e) {
            System.out.println("\n[登录失败]: " + e.getMessage());
            LoggerUtil.audit("登录尝试失败: " + e.getMessage());
        }
    }

    private void handleRegister() {
        try {
            System.out.print("请输入新用户名: ");
            String username = scanner.nextLine();
            System.out.print("请输入新密码: ");
            String password = scanner.nextLine();

            User newUser = userService.createUser(username, password);
            System.out.println("\n[注册成功]: 用户 '" + newUser.getUsername() + "' 创建成功！请登录。");
            LoggerUtil.audit("新用户注册: " + username);
        } catch (Exception e) {
            System.out.println("\n[注册失败]: " + e.getMessage());
        }
    }

    // --- (主菜单... 无变化) ---
    private void showMainMenu() {
        System.out.println("\n--- 主菜单 (登录为: " + currentUser.getUsername() + ") ---");

        // 检查 "菜单权限"
        if (checkPerm("PERM_USER_MGMT")) System.out.println("1. 用户管理");
        if (checkPerm("PERM_ROLE_MGMT")) System.out.println("2. 角色管理");
        if (checkPerm("PERM_PERM_MGMT")) System.out.println("3. 权限管理");

        System.out.println("8. 查看我的权限");
        System.out.println("9. 登出");
        System.out.print("请输入选项: ");

        int choice = readIntInput();
        scanner.nextLine();

        switch (choice) {
            case 1:
                // 再次检查 "菜单权限" (安全双重检查)
                if (checkPerm("PERM_USER_MGMT")) handleUserManagement();
                else System.out.println("权限不足！");
                break;
            case 2:
                if (checkPerm("PERM_ROLE_MGMT")) handleRoleManagement();
                else System.out.println("权限不足！");
                break;
            case 3:
                if (checkPerm("PERM_PERM_MGMT")) handlePermissionManagement();
                else System.out.println("权限不足！");
                break;
            case 8:
                handleShowMyPermissions();
                break;
            case 9:
                System.out.println("用户 " + currentUser.getUsername() + " 已登出。");
                LoggerUtil.audit("用户登出: " + currentUser.getUsername());
                this.currentUser = null;
                break;
            default:
                System.out.println("无效选项。");
        }
    }

    private boolean checkPerm(String permissionName) {
        return authService.checkPermission(this.currentUser, permissionName);
    }


    // ========================================
    // 【已升级】子菜单: 1. 用户管理 (增加 "操作权限" 检查)
    // ========================================

    private void handleUserManagement() {
        System.out.println("\n--- 1. 用户管理 ---");

        // 【升级】: 只有拥有 "操作权限"，才能看到 "按钮"
        if (checkPerm("PERM_USER_ASSIGN_ROLE")) System.out.println("1. 分配角色给用户");
        if (checkPerm("PERM_USER_REMOVE_ROLE")) System.out.println("2. 移除用户的角色");
        if (checkPerm("PERM_USER_LIST")) System.out.println("3. 查看所有用户");

        System.out.println("0. 返回主菜单");

        int choice = readIntInput();
        scanner.nextLine();

        switch (choice) {
            case 1:
                // 【升级】: 执行前再次检查，满足实验要求 ("权限不足，无法...")
                if (checkPerm("PERM_USER_ASSIGN_ROLE")) handleAssignRoleToUser();
                else System.out.println("权限不足，无法分配角色。");
                break;
            case 2:
                if (checkPerm("PERM_USER_REMOVE_ROLE")) handleRemoveRoleFromUser();
                else System.out.println("权限不足，无法移除角色。");
                break;
            case 3:
                if (checkPerm("PERM_USER_LIST")) handleListAllUsers();
                else System.out.println("权限不足，无法查看用户列表。");
                break;
            case 0:
                return;
            default:
                System.out.println("无效选项。");
        }
    }

    // --- (用户管理... 无变化) ---
    private void handleListAllUsers() {
        List<User> users = userService.getAllUsers();
        System.out.println("\n--- 系统中的所有用户 ---");
        System.out.println("-------------------------");
        for (User user : users) {
            System.out.printf("ID: %-3d | 用户名: %s\n",
                    user.getId(), user.getUsername());
        }
        System.out.println("-------------------------");
    }

    private void handleAssignRoleToUser() {
        try {
            System.out.println("\n--- 第一步: 选择一个用户 ---");
            handleListAllUsers();
            System.out.print("请输入要操作的用户 ID: ");
            int userId = readIntInput();
            scanner.nextLine();

            System.out.println("\n--- 第二步: 选择一个角色 ---");
            handleListAllRoles();
            System.out.print("请输入要分配的角色 ID: ");
            int roleId = readIntInput();
            scanner.nextLine();

            authService.assignRoleToUser(userId, roleId);
            System.out.println("[成功]: 角色 " + roleId + " 已分配给用户 " + userId);
            LoggerUtil.audit(currentUser.getUsername() + " 分配了角色 " + roleId + " 给用户 " + userId);

        } catch (Exception e) {
            System.out.println("\n[失败]: " + e.getMessage());
        }
    }

    private void handleRemoveRoleFromUser() {
        try {
            System.out.println("\n--- 第一步: 选择一个用户 ---");
            handleListAllUsers();
            System.out.print("请输入要操作的用户 ID: ");
            int userId = readIntInput();
            scanner.nextLine();

            System.out.println("\n--- 第二步: 选择要移除的角色 ---");
            Set<Role> currentRoles = new UserRoleDaoImpl().findRolesByUserId(userId);
            if (currentRoles.isEmpty()) {
                System.out.println("该用户目前没有任何角色。");
                return;
            }
            System.out.println("用户 " + userId + " 当前拥有以下角色:");
            for (Role role : currentRoles) {
                System.out.printf("ID: %-3d | 名称: %s\n", role.getId(), role.getName());
            }
            System.out.print("请输入要移除的角色 ID: ");
            int roleId = readIntInput();
            scanner.nextLine();

            authService.removeRoleFromUser(userId, roleId);
            System.out.println("[成功]: 角色 " + roleId + " 已从用户 " + userId + " 移除");
            LoggerUtil.audit(currentUser.getUsername() + " 移除了用户 " + userId + " 的角色 " + roleId);

        } catch (Exception e) {
            System.out.println("\n[失败]: " + e.getMessage());
        }
    }


    // ========================================
    // 【已升级】子菜单: 2. 角色管理 (增加 "操作权限" 检查)
    // ========================================

    private void handleRoleManagement() {
        System.out.println("\n--- 2. 角色管理 ---");
        // (为了简洁，我们假设 "创建" 和 "查看" 只需要菜单权限)
        System.out.println("1. 创建新角色");
        System.out.println("2. 查看所有角色");
        if (checkPerm("PERM_ROLE_ASSIGN_PERM")) System.out.println("3. 分配权限给角色");
        if (checkPerm("PERM_ROLE_SET_INHERIT")) System.out.println("4. 设置角色继承");

        System.out.println("0. 返回主菜单");

        int choice = readIntInput();
        scanner.nextLine();

        switch (choice) {
            case 1: handleCreateRole(); break;
            case 2: handleListAllRoles(); break;
            case 3:
                if (checkPerm("PERM_ROLE_ASSIGN_PERM")) handleAssignPermissionToRole();
                else System.out.println("权限不足，无法分配权限。");
                break;
            case 4:
                if (checkPerm("PERM_ROLE_SET_INHERIT")) handleSetRoleInheritance();
                else System.out.println("权限不足，无法设置继承。");
                break;
            case 0: return;
            default:
                System.out.println("无效选项。");
        }
    }

    // --- (角色管理... 无变化) ---
    private void handleCreateRole() {
        try {
            System.out.print("请输入新角色名: ");
            String name = scanner.nextLine();
            System.out.print("请输入描述: ");
            String desc = scanner.nextLine();

            Role newRole = roleService.createRole(name, desc);
            System.out.println("[成功]: 角色 '" + newRole.getName() + "' (ID: " + newRole.getId() + ") 已创建。");
            LoggerUtil.audit(currentUser.getUsername() + " 创建了角色: " + name);
        } catch (Exception e) {
            System.out.println("[失败]: " + e.getMessage());
        }
    }

    private void handleListAllRoles() {
        List<Role> roles = roleService.getAllRoles();
        System.out.println("\n--- 系统中的所有角色 ---");
        System.out.println("-----------------------------------------------------");
        for (Role role : roles) {
            System.out.printf("ID: %-3d | 名称: %-20s | 描述: %s\n",
                    role.getId(), role.getName(), role.getDescription());
        }
        System.out.println("-----------------------------------------------------");
    }

    private void handleAssignPermissionToRole() {
        try {
            System.out.println("\n--- (快捷模式) 分配权限给角色 ---");
            handleListAllRoles();
            System.out.print("请输入要操作的角色 ID: ");
            int roleId = readIntInput();
            scanner.nextLine();

            handleListAllPermissions();
            System.out.print("请输入要分配的权限 ID: ");
            int permId = readIntInput();
            scanner.nextLine();

            roleService.assignPermissionToRole(roleId, permId);
            System.out.println("[成功]: 权限 " + permId + " 已分配给角色 " + roleId);
            LoggerUtil.audit(currentUser.getUsername() + " 分配了权限 " + permId + " 给角色 " + roleId);
        } catch (Exception e) {
            System.out.println("[失败]: " + e.getMessage());
        }
    }

    private void handleSetRoleInheritance() {
        try {
            System.out.println("\n--- (快捷模式) 设置角色继承 ---");
            handleListAllRoles();
            System.out.print("请输入【高级】角色 ID (e.g., 经理): ");
            int seniorId = readIntInput();
            System.out.print("请输入【低级】角色 ID (e.g., 员工): ");
            int juniorId = readIntInput();
            scanner.nextLine();

            roleService.setRoleInheritance(seniorId, juniorId);
            System.out.println("[成功]: 角色 " + seniorId + " 现在继承角色 " + juniorId + " 的权限。");
            LoggerUtil.audit(currentUser.getUsername() + " 设置了角色继承: " + seniorId + " -> " + juniorId);
        } catch (Exception e) {
            System.out.println("[失败]: " + e.getMessage());
        }
    }

    // ========================================
    // 【已升级】子菜单: 3. 权限管理
    // ========================================
    private void handlePermissionManagement() {
        System.out.println("\n--- 3. 权限管理 ---");
        // (假设 "创建" 和 "查看" 只需要菜单权限)
        System.out.println("1. 创建新权限");
        System.out.println("2. 查看所有权限");
        System.out.println("0. 返回主菜单");

        int choice = readIntInput();
        scanner.nextLine();

        switch (choice) {
            case 1: handleCreatePermission(); break;
            case 2: handleListAllPermissions(); break;
            case 0: return;
            default:
                System.out.println("无效选项。");
        }
    }

    // --- (权限管理... 无变化) ---
    private void handleCreatePermission() {
        try {
            System.out.print("请输入新权限名 (e.g., PERM_USER_DELETE): ");
            String name = scanner.nextLine().toUpperCase();
            System.out.print("请输入描述: ");
            String desc = scanner.nextLine();

            Permission newPerm = permissionService.createPermission(name, desc);
            System.out.println("[成功]: 权限 '" + newPerm.getName() + "' (ID: " + newPerm.getId() + ") 已创建。");
            LoggerUtil.audit(currentUser.getUsername() + " 创建了权限: " + name);
        } catch (Exception e) {
            System.out.println("[失败]: " + e.getMessage());
        }
    }

    private void handleListAllPermissions() {
        List<Permission> perms = permissionService.getAllPermissions();
        System.out.println("\n--- 系统中的所有权限 ---");
        System.out.println("-----------------------------------------------------------------");
        for (Permission p : perms) {
            System.out.printf("ID: %-3d | 名称: %-25s | 描述: %s\n",
                    p.getId(), p.getName(), p.getDescription());
        }
        System.out.println("-----------------------------------------------------------------");
    }

    // --- (查看我的权限... 无变化) ---
    private void handleShowMyPermissions() {
        System.out.println("\n--- 用户 " + currentUser.getUsername() + " 的所有有效权限 ---");
        try {
            Set<Role> myRoles = new UserRoleDaoImpl().findRolesByUserId(currentUser.getId());
            if (myRoles.isEmpty()) {
                System.out.println("您没有被分配任何角色。");
                return;
            }

            Set<Permission> myPerms = new HashSet<>();
            for (Role r : myRoles) {
                myPerms.addAll(roleService.getAllPermissionsForRole(r.getId()));
            }

            if (myPerms.isEmpty()) {
                System.out.println("您所在的角色没有任何权限。");
                return;
            }

            System.out.println("------------------------------------------");
            for (Permission p : myPerms) {
                System.out.printf("- %-25s (%s)\n", p.getName(), p.getDescription());
            }
            System.out.println("------------------------------------------");

        } catch (Exception e) {
            System.out.println("获取权限时出错: " + e.getMessage());
        }
    }


    // --- (读输入... 无变化) ---
    private int readIntInput() {
        while (true) {
            try {
                return scanner.nextInt();
            } catch (java.util.InputMismatchException e) {
                System.out.print("输入无效，请输入一个数字: ");
                scanner.nextLine();
            }
        }
    }
}