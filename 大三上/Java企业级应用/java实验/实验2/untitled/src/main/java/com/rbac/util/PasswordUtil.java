package com.rbac.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    // 1. 生成带盐的哈希密码 (用于创建用户)
    // "盐" (salt) 会由 BCrypt 自动生成并包含在最终的哈希字符串中
    public static String hashPassword(String plainTextPassword) {
        // BCrypt.gensalt() 会生成一个随机的盐
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
    }

    // 2. 校验密码 (用于登录)
    public static boolean checkPassword(String plainTextPassword, String hashedPassword) {
        // BCrypt 会从 hashedPassword 中自动解析出 "盐"
        // 然后用这个盐去加密 plainTextPassword，再进行比对
        try {
            return BCrypt.checkpw(plainTextPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // 如果 hashedPassword 格式不正确
            System.err.println("密码校验格式错误: " + e.getMessage());
            return false;
        }
    }
}