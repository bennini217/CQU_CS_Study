package com.rbac.dao.impl;

import com.rbac.dao.IUserRoleDao;
import com.rbac.model.Role;
import com.rbac.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * IUserRoleDao 接口的 JDBC 实现
 */
public class UserRoleDaoImpl implements IUserRoleDao {

    @Override
    public boolean assignRoleToUser(int userId, int roleId) throws SQLException {
        String sql = "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, roleId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            // 忽略 "UNIQUE constraint failed" 错误，表示已经存在
            if (e.getMessage().contains("UNIQUE constraint")) return true;
            throw e;
        }
    }

    @Override
    public boolean removeRoleFromUser(int userId, int roleId) throws SQLException {
        String sql = "DELETE FROM user_roles WHERE user_id = ? AND role_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, roleId);
            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public Set<Role> findRolesByUserId(int userId) throws SQLException {
        // SQL JOIN 查询：连接 user_roles 和 role 两个表
        String sql = "SELECT r.* FROM role r " +
                "JOIN user_roles ur ON r.id = ur.role_id " +
                "WHERE ur.user_id = ?";
        Set<Role> roles = new HashSet<>();

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    roles.add(mapResultSetToRole(rs));
                }
            }
        }
        return roles;
    }

    // 辅助工具：把 ResultSet (数据库行) 映射到 Role (Java 对象)
    private Role mapResultSetToRole(ResultSet rs) throws SQLException {
        return new Role(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description")
        );
    }
}