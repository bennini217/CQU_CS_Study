package com.rbac.dao.impl;

import com.rbac.dao.IRoleDao;
import com.rbac.model.Permission;
import com.rbac.model.Role;
import com.rbac.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * IRoleDao 接口的 JDBC 实现
 */
public class RoleDaoImpl implements IRoleDao {

    // --- 1. role 表的基本操作 ---

    @Override
    public Role createRole(Role role) throws SQLException {
        String sql = "INSERT INTO role (name, description) VALUES (?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, role.getName());
            pstmt.setString(2, role.getDescription());
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    role.setId(generatedKeys.getInt(1));
                }
            }
            return role;
        }
    }

    @Override
    public Optional<Role> findRoleById(int id) throws SQLException {
        String sql = "SELECT * FROM role WHERE id = ?";
        return findRoleByQuery(sql, id);
    }

    @Override
    public Optional<Role> findRoleByName(String name) throws SQLException {
        String sql = "SELECT * FROM role WHERE name = ?";
        return findRoleByQuery(sql, name);
    }

    // 辅助方法，用于根据不同查询条件查找角色
    private Optional<Role> findRoleByQuery(String sql, Object param) throws SQLException {
        Role role = null;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (param instanceof Integer) {
                pstmt.setInt(1, (Integer) param);
            } else if (param instanceof String) {
                pstmt.setString(1, (String) param);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    role = mapResultSetToRole(rs);
                }
            }
        }
        return Optional.ofNullable(role);
    }

    @Override
    public boolean deleteRole(int id) throws SQLException {
        String sql = "DELETE FROM role WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public List<Role> findAllRoles() throws SQLException {
        String sql = "SELECT * FROM role";
        List<Role> roles = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                roles.add(mapResultSetToRole(rs));
            }
        }
        return roles;
    }


    // --- 2. role_permissions (角色-权限) 关系表操作 ---

    @Override
    public boolean addPermissionToRole(int roleId, int permissionId) throws SQLException {
        String sql = "INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roleId);
            pstmt.setInt(2, permissionId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            // 忽略 "UNIQUE constraint failed" 错误，表示已经存在
            if (e.getMessage().contains("UNIQUE constraint")) return true;
            throw e;
        }
    }

    @Override
    public boolean removePermissionFromRole(int roleId, int permissionId) throws SQLException {
        String sql = "DELETE FROM role_permissions WHERE role_id = ? AND permission_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roleId);
            pstmt.setInt(2, permissionId);
            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public Set<Permission> findPermissionsByRoleId(int roleId) throws SQLException {
        // SQL JOIN 查询：连接 role_permissions 和 permission 两个表
        String sql = "SELECT p.* FROM permission p " +
                "JOIN role_permissions rp ON p.id = rp.permission_id " +
                "WHERE rp.role_id = ?";
        Set<Permission> permissions = new HashSet<>();

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    permissions.add(mapResultSetToPermission(rs));
                }
            }
        }
        return permissions;
    }


    // --- 3. role_hierarchy (角色继承) 【RBAC1 核心】---

    @Override
    public boolean addRoleInheritance(int seniorRoleId, int juniorRoleId) throws SQLException {
        String sql = "INSERT INTO role_hierarchy (senior_role_id, junior_role_id) VALUES (?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, seniorRoleId);
            pstmt.setInt(2, juniorRoleId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint")) return true;
            throw e;
        }
    }

    @Override
    public boolean removeRoleInheritance(int seniorRoleId, int juniorRoleId) throws SQLException {
        String sql = "DELETE FROM role_hierarchy WHERE senior_role_id = ? AND junior_role_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, seniorRoleId);
            pstmt.setInt(2, juniorRoleId);
            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public Set<Role> findJuniorRoles(int seniorRoleId) throws SQLException {
        // SQL JOIN 查询：连接 role_hierarchy 和 role 两个表
        String sql = "SELECT r.* FROM role r " +
                "JOIN role_hierarchy rh ON r.id = rh.junior_role_id " +
                "WHERE rh.senior_role_id = ?";
        return findRolesByHierarchyQuery(sql, seniorRoleId);
    }

    @Override
    public Set<Role> findSeniorRoles(int juniorRoleId) throws SQLException {
        String sql = "SELECT r.* FROM role r " +
                "JOIN role_hierarchy rh ON r.id = rh.senior_role_id " +
                "WHERE rh.junior_role_id = ?";
        return findRolesByHierarchyQuery(sql, juniorRoleId);
    }

    // 辅助方法，用于查询继承关系
    private Set<Role> findRolesByHierarchyQuery(String sql, int roleId) throws SQLException {
        Set<Role> roles = new HashSet<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    roles.add(mapResultSetToRole(rs));
                }
            }
        }
        return roles;
    }


    // --- 辅助工具 (Helper Methods) ---

    // 把 ResultSet (数据库行) 映射到 Role (Java 对象)
    private Role mapResultSetToRole(ResultSet rs) throws SQLException {
        return new Role(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description")
        );
    }

    // 把 ResultSet (数据库行) 映射到 Permission (Java 对象)
    private Permission mapResultSetToPermission(ResultSet rs) throws SQLException {
        return new Permission(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description")
        );
    }
}