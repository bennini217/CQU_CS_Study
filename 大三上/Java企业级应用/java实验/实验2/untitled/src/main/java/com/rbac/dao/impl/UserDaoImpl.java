package com.rbac.dao.impl;

import com.rbac.dao.IUserDao;
import com.rbac.model.User;
import com.rbac.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList; // 【新增】
import java.util.List;     // 【新增】
import java.util.Optional;

/**
 * IUserDao 接口的 JDBC 实现
 * 【已升级】: 增加了 findAllUsers 和一个辅助方法，使其更专业
 */
public class UserDaoImpl implements IUserDao {

    @Override
    public User createUser(User user) throws SQLException {
        String sql = "INSERT INTO user (username, password_hash) VALUES (?, ?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) throw new SQLException("创建用户失败，没有行被影响。");

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("创建用户失败，无法获取 ID。");
                }
            }
            return user;
        }
    }

    @Override
    public Optional<User> findUserByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM user WHERE username = ?";
        User user = null;

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // 【重构】: 调用辅助方法
                    user = mapResultSetToUser(rs);
                }
            }
        }
        return Optional.ofNullable(user);
    }

    @Override
    public Optional<User> findUserById(int id) throws SQLException {
        String sql = "SELECT * FROM user WHERE id = ?";
        User user = null;

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // 【重构】: 调用辅助方法
                    user = mapResultSetToUser(rs);
                }
            }
        }
        return Optional.ofNullable(user);
    }

    @Override
    public boolean deleteUser(int id) throws SQLException {
        String sql = "DELETE FROM user WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 【新增功能】: 实现 findAllUsers
     */
    @Override
    public List<User> findAllUsers() throws SQLException {
        String sql = "SELECT * FROM user";
        List<User> users = new ArrayList<>();

        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // 【重构】: 调用辅助方法
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    /**
     * 【新增】: 专业的辅助方法 (Helper Method)
     * * 避免在三个不同的地方写重复的 "rs.getInt..." 代码
     * * 把 "数据库行" 映射到 "Java 对象"
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        // 注意: 我们的 User model 没有包含 created_at, 所以我们不映射它
        return user;
    }
}