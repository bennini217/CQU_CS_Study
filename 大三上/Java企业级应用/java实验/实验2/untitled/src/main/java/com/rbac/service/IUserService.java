package com.rbac.service;

import com.rbac.model.User;

import java.util.List;
import java.util.Optional;

/**
 * User (用户) 的业务逻辑服务 (Service) 接口
 * * 这个 "合同" 定义了所有与用户相关的 "业务操作"
 * * 它负责处理业务规则、数据校验和调用 DAO
 */
public interface IUserService {

    /**
     * 注册/创建新用户 (这是一个业务操作)
     * @param username 明文用户名
     * @param password 明文密码
     * @return 创建成功的 User 对象
     * @throws Exception 如果用户名已存在或创建失败
     */
    User createUser(String username, String password) throws Exception;

    /**
     * 根据用户名查找用户 (这个功能和 DAO 类似，但由 Service 暴露给上层)
     * @param username 用户名
     * @return 包含 User 的 Optional
     */
    Optional<User> getUserByUsername(String username);

    /**
     * 根据 ID 删除用户
     * @param id 用户 ID
     * @return true 如果删除成功
     * @throws Exception 如果删除失败
     */
    boolean deleteUser(int id) throws Exception;
    // ... (在 deleteUser 下面添加)

    /**
     * 【新增功能】: 获取所有用户
     */
    List<User> getAllUsers();
    // (我们稍后会在这里添加 IAuthService，负责登录)
}