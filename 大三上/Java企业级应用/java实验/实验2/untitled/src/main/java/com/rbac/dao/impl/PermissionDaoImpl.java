package com.rbac.dao.impl;

import com.rbac.dao.IPermissionDao;
import com.rbac.model.Permission;
import com.rbac.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * IPermissionDao 接口的 JDBC 实现
 */
public class PermissionDaoImpl implements IPermissionDao {

    @Override
    public Permission createPermission(Permission permission) throws SQLException {
        String sql = "INSERT INTO permission (name, description) VALUES (?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, permission.getName());
            pstmt.setString(2, permission.getDescription());
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    permission.setId(generatedKeys.getInt(1));
                }
            }
            return permission;
        }
    }

    @Override
    public Optional<Permission> findPermissionByName(String name) throws SQLException {
        String sql = "SELECT * FROM permission WHERE name = ?";
        return findPermissionByQuery(sql, name);
    }

    @Override
    public Optional<Permission> findPermissionById(int id) throws SQLException {
        String sql = "SELECT * FROM permission WHERE id = ?";
        return findPermissionByQuery(sql, id);
    }

    // 辅助方法，用于根据不同查询条件查找
    private Optional<Permission> findPermissionByQuery(String sql, Object param) throws SQLException {
        Permission permission = null;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (param instanceof Integer) {
                pstmt.setInt(1, (Integer) param);
            } else if (param instanceof String) {
                pstmt.setString(1, (String) param);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    permission = mapResultSetToPermission(rs);
                }
            }
        }
        return Optional.ofNullable(permission);
    }

    @Override
    public List<Permission> findAllPermissions() throws SQLException {
        String sql = "SELECT * FROM permission";
        List<Permission> permissions = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                permissions.add(mapResultSetToPermission(rs));
            }
        }
        return permissions;
    }

    @Override
    public boolean deletePermission(int id) throws SQLException {
        String sql = "DELETE FROM permission WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    // 辅助工具：把 ResultSet (数据库行) 映射到 Permission (Java 对象)
    private Permission mapResultSetToPermission(ResultSet rs) throws SQLException {
        return new Permission(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description")
        );
    }
}