package com.rbac.dao;

import com.rbac.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional; // 我们将使用 Optional 来优雅地处理 "null"

/**
 * User (用户) 的数据访问对象 (DAO) 接口
 * * 这是一个 "合同"，定义了所有与 User 表相关的数据库操作
 */
public interface IUserDao {

    /**
     * 创建一个新用户
     * @param user 要创建的用户对象 (不包含 id)
     * @param salt   为该用户生成的盐
     * @return 创建后的用户对象 (包含数据库生成的 id)
     * @throws SQLException 如果数据库操作失败
     */
    User createUser(User user) throws SQLException;

    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 一个包含 User 对象的 Optional (如果找到)，或一个空的 Optional (如果没找到)
     * @throws SQLException 如果数据库操作失败
     */
    Optional<User> findUserByUsername(String username) throws SQLException;

    /**
     * 根据 ID 查找用户
     * @param id 用户 ID
     * @return 一个包含 User 对象的 Optional (如果找到)
     * @throws SQLException 如果数据库操作失败
     */
    Optional<User> findUserById(int id) throws SQLException;

    /**
     * 根据 ID 删除用户
     * @param id 要删除的用户 ID
     * @return true 如果删除成功, false 如果未找到用户
     * @throws SQLException 如果数据库操作失败
     */
    boolean deleteUser(int id) throws SQLException;
    List<User> findAllUsers() throws SQLException;
    // 我们还可以添加 update, findAll 等，但先从这几个核心开始
}