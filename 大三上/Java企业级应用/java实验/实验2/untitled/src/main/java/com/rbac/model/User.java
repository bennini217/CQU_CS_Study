package com.rbac.model;

/**
 * User (用户) 实体类 (POJO / JavaBean)
 * 这个类映射数据库中的 "user" 表
 */
public class User {

    // --- 1. 属性 (Fields) ---
    // 属性设为 private，这是 Java 的 "封装" 特性
    private int id;
    private String username;
    private String passwordHash; // 对应数据库的 password_hash

    // --- 2. 构造方法 (Constructors) ---
    // C++ 和 Python 都有构造方法 (e.g., __init__)

    /**
     * 一个空的构造方法
     * 在很多 Java 框架中是必需的
     */
    public User() {
    }

    /**
     * 一个方便的构造方法，用于创建新用户或从数据库读取用户
     */
    public User(int id, String username, String passwordHash) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    // --- 3. Getter 和 Setter 方法 ---
    // 因为属性是 private，我们需要 public 方法来读取 (get) 和设置 (set) 它们
    // 这是 Java (JavaBean 规范) 的标准写法，IDE 通常可以自动生成

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    // --- 4. (可选) toString() 方法 ---
    // 类似于 Python 的 __repr__ 或 __str__
    // 方便我们打印这个对象进行调试 (Debug)
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                '}';
    }
}