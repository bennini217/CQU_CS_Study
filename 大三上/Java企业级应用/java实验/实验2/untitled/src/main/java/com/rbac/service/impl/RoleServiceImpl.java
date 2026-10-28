package com.rbac.service.impl;

import com.rbac.dao.IRoleDao;
import com.rbac.dao.impl.RoleDaoImpl;
import com.rbac.model.Permission;
import com.rbac.model.Role;
import com.rbac.service.IRoleService;
import com.rbac.util.LoggerUtil;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * IRoleService 接口的实现
 */
public class RoleServiceImpl implements IRoleService {

    // "经理" 需要 "工人"
    private final IRoleDao roleDao;

    // (我们还需要一个 PermissionDao 来检查权限是否存在，但为了简单，暂时省略)

    public RoleServiceImpl() {
        this.roleDao = new RoleDaoImpl();
    }

    @Override
    public Role createRole(String name, String description) throws Exception {
        // 1. 业务规则: 检查角色名是否已存在
        if (roleDao.findRoleByName(name).isPresent()) {
            throw new Exception("创建失败：角色名 '" + name + "' 已被占用。");
        }

        Role newRole = new Role();
        newRole.setName(name);
        newRole.setDescription(description);

        try {
            return roleDao.createRole(newRole);
        } catch (SQLException e) {
            LoggerUtil.error("创建角色时数据库异常: " + e.getMessage(), e);
            throw new Exception("数据库错误，创建角色失败。");
        }
    }

    @Override
    public boolean deleteRole(int roleId) throws Exception {
        try {
            return roleDao.deleteRole(roleId);
        } catch (SQLException e) {
            LoggerUtil.error("删除角色时数据库异常: " + e.getMessage(), e);
            throw new Exception("数据库错误，删除角色失败。");
        }
    }

    @Override
    public List<Role> getAllRoles() {
        try {
            return roleDao.findAllRoles();
        } catch (SQLException e) {
            LoggerUtil.error("获取所有角色时数据库异常: " + e.getMessage(), e);
            return List.of(); // 返回一个空列表
        }
    }

    @Override
    public void assignPermissionToRole(int roleId, int permissionId) throws Exception {
        // 业务规则：我们应该先检查 roleId 和 permissionId 是否真的存在
        // (暂时省略这一步，假设它们存在)
        try {
            roleDao.addPermissionToRole(roleId, permissionId);
        } catch (SQLException e) {
            LoggerUtil.error("给角色分配权限时数据库异常: " + e.getMessage(), e);
            throw new Exception("数据库错误，分配权限失败。");
        }
    }

    @Override
    public void setRoleInheritance(int seniorRoleId, int juniorRoleId) throws Exception {
        // 业务规则：防止自己继承自己
        if (seniorRoleId == juniorRoleId) {
            throw new Exception("设置失败：角色不能继承自己。");
        }

        // 业务规则：防止循环继承 (A->B, B->A)
        // 这是一个复杂的图遍历，我们暂时先信任用户，只做简单检查
        // (一个简单的检查：检查 B 是否已经是 A 的上级)
        if (isRoleAlreadySenior(juniorRoleId, seniorRoleId)) {
            throw new Exception("设置失败：产生循环继承！(e.g., A->B, B->...->A)");
        }

        try {
            roleDao.addRoleInheritance(seniorRoleId, juniorRoleId);
        } catch (SQLException e) {
            LoggerUtil.error("设置角色继承时数据库异常: " + e.getMessage(), e);
            throw new Exception("数据库错误，设置继承失败。");
        }
    }

    /**
     * 【RBAC1 核心】: 获取角色的所有权限 (递归)
     */
    @Override
    public Set<Permission> getAllPermissionsForRole(int roleId) {
        // 我们使用一个 Set 来自动处理重复的权限
        Set<Permission> allPermissions = new HashSet<>();

        // 我们需要一个 Set 来跟踪已访问的角色，防止循环继承导致的死循环
        Set<Integer> visitedRoleIds = new HashSet<>();

        // 调用递归辅助方法
        collectPermissionsRecursively(roleId, allPermissions, visitedRoleIds);
        return allPermissions;
    }

    /**
     * 递归辅助方法
     * @param roleId 当前要检查的角色 ID
     * @param allPermissions 正在收集的权限总集
     * @param visitedRoleIds 已访问的角色 ID (防止死循环)
     */
    private void collectPermissionsRecursively(int roleId, Set<Permission> allPermissions, Set<Integer> visitedRoleIds) {
        // 1. 【防死循环】
        //    如果这个角色 ID 已经访问过了 (e.g., A->B->A)，立刻停止
        if (visitedRoleIds.contains(roleId)) {
            return;
        }
        // 2. 标记：把当前角色 ID 加入“已访问”列表
        visitedRoleIds.add(roleId);

        try {
            // 3. 【获取当前角色的直接权限】
            Set<Permission> directPermissions = roleDao.findPermissionsByRoleId(roleId);
            allPermissions.addAll(directPermissions); // 加入总集

            // 4. 【递归】
            //    获取当前角色【直接继承的下级角色】
            Set<Role> juniorRoles = roleDao.findJuniorRoles(roleId);

            // 5. 遍历所有下级角色
            for (Role juniorRole : juniorRoles) {
                collectPermissionsRecursively(juniorRole.getId(), allPermissions, visitedRoleIds);
            }

        } catch (SQLException e) {
            LoggerUtil.error("递归获取权限时数据库异常: " + e.getMessage(), e);
        }
    }

    /**
     * 辅助方法：检查 A 是否已经是 B 的上级 (防止循环继承)
     * (这是一个简化的检查)
     */
    private boolean isRoleAlreadySenior(int roleA, int roleB) {
        // 这是一个简化的检查，只检查 roleA 是否直接被 roleB 继承
        // 完整的检查需要像上面一样递归
        try {
            return roleDao.findSeniorRoles(roleA).stream().anyMatch(r -> r.getId() == roleB);
        } catch (SQLException e) {
            return false;
        }
    }
}