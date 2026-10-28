package com.rbac.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseUtil {

    // 数据库文件名
    private static final String DATABASE_URL = "jdbc:sqlite:rbac.db";

    // 获取数据库连接
    public static Connection getConnection() throws SQLException {
        // Maven 帮我们管理了驱动，这里 DriverManager 会自动找到它
        return DriverManager.getConnection(DATABASE_URL);
    }

    // 初始化数据库，创建所有表
    public static void initializeDatabase() {
        String[] createTableSqls = {
                "CREATE TABLE IF NOT EXISTS user (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE, password_hash TEXT NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);",
                "CREATE TABLE IF NOT EXISTS role (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE, description TEXT);",
                "CREATE TABLE IF NOT EXISTS permission (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE, description TEXT);",
                "CREATE TABLE IF NOT EXISTS user_roles (user_id INTEGER NOT NULL, role_id INTEGER NOT NULL, PRIMARY KEY (user_id, role_id), FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE, FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE);",
                "CREATE TABLE IF NOT EXISTS role_permissions (role_id INTEGER NOT NULL, permission_id INTEGER NOT NULL, PRIMARY KEY (role_id, permission_id), FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE, FOREIGN KEY (permission_id) REFERENCES permission (id) ON DELETE CASCADE);",
                "CREATE TABLE IF NOT EXISTS role_hierarchy (senior_role_id INTEGER NOT NULL, junior_role_id INTEGER NOT NULL, PRIMARY KEY (senior_role_id, junior_role_id), FOREIGN KEY (senior_role_id) REFERENCES role (id) ON DELETE CASCADE, FOREIGN KEY (junior_role_id) REFERENCES role (id) ON DELETE CASCADE);"
        };

        // try-with-resources 语法：自动关闭 conn 和 stmt
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            System.out.println("数据库连接成功，正在初始化表结构...");

            for (String sql : createTableSqls) {
                stmt.execute(sql);
            }

            System.out.println("数据库表结构初始化成功！");

        } catch (SQLException e) {
            System.err.println("数据库初始化失败！");
            e.printStackTrace();
        }
    }
}