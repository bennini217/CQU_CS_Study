package com.rbac.service.impl;

import com.rbac.dao.IUserDao;
import com.rbac.dao.impl.UserDaoImpl; // 我们暂时直接 new 一个实现
import com.rbac.model.User;
import com.rbac.service.IUserService;
import com.rbac.util.LoggerUtil;
import com.rbac.util.PasswordUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * IUserService 接口的实现
 */
public class UserServiceImpl implements IUserService {

    // --- 依赖注入 (Dependency Injection) ---
    // Service 层需要 DAO 层来工作
    // 这就像 "经理" (Service) 需要一个 "工人" (DAO)
    private final IUserDao userDao;

    /**
     * 构造方法
     * 在这里, 我们 "雇佣" 了这个工人 (UserDao)
     * (更高级的框架会在这里自动注入, 我们先手动 new)
     */
    public UserServiceImpl() {
        this.userDao = new UserDaoImpl(); // **重要**: 雇佣了我们刚才写的 DAO 工人
    }

    // (更优的 "依赖注入" 方式是使用构造函数传参, 但我们先用简单版)
    // public UserServiceImpl(IUserDao userDao) {
    //    this.userDao = userDao;
    // }
    // ---

    /**
     * 实现 "创建用户" 的业务逻辑
     */
    @Override
    public User createUser(String username, String password) throws Exception {

        // --- 这是 Service 层的核心价值 ---
        // 1. 业务规则: 检查用户名是否已存在
        if (userDao.findUserByUsername(username).isPresent()) {
            // 如果 isPresent() == true, 说明用户已存在
            throw new Exception("创建失败：用户名 '" + username + "' 已被占用。");
        }

        // 2. 业务规则: 密码不能为空
        if (password == null || password.isEmpty()) {
            throw new Exception("创建失败：密码不能为空。");
        }

        // 3. 业务操作: 加密密码 (调用 Util)
        String salt = PasswordUtil.hashPassword(password); // jBCrypt 会自动处理盐

        // 3. 业务操作: 加密密码 (调用 Util)
        String hashedPassword = PasswordUtil.hashPassword(password);

        // 4. 准备要存入数据库的 User 对象
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPasswordHash(hashedPassword);

        // 5. 业务操作: 调用 DAO 进行存储
        try {
            return userDao.createUser(newUser); // 调用【修改后】的 DAO 方法
        } catch (SQLException e) {
            LoggerUtil.error("创建用户时数据库异常: " + e.getMessage(), e);
            throw new Exception("数据库错误，创建用户失败。");
        }
    }

    @Override
    public Optional<User> getUserByUsername(String username) {
        try {
            return userDao.findUserByUsername(username);
        } catch (SQLException e) {
            LoggerUtil.error("查找用户时数据库异常: " + e.getMessage(), e);
            return Optional.empty(); // 向 UI 层屏蔽 SQL 异常
        }
    }

    @Override
    public boolean deleteUser(int id) throws Exception {
        // 可以在这里加业务规则，比如 "不允许删除 ID 为 1 (admin) 的用户"
        if (id == 1) {
            throw new Exception("不允许删除超级管理员 (ID=1)。");
        }

        try {
            return userDao.deleteUser(id);
        } catch (SQLException e) {
            LoggerUtil.error("删除用户时数据库异常: " + e.getMessage(), e);
            throw new Exception("数据库错误，删除用户失败。");
        }
    }

    /**
     * 【新增功能】: 实现 "获取所有用户"
     */
    @Override
    public List<User> getAllUsers() {
        try {
            return userDao.findAllUsers();
        } catch (SQLException e) {
            LoggerUtil.error("获取所有用户时数据库异常: " + e.getMessage(), e);
            return List.of(); // 出错时返回一个安全的空列表
        }
    }
}