package com.student.common.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 密码加密工具类
 * 支持演示模式（明文）和生产模式（BCrypt加密）
 * 
 * @author 段雯菀
 * @date 2025-01
 */
@Component
public class PasswordUtil {

    /**
     * 密码加密模式
     * demo: 演示模式，明文存储
     * production: 生产模式，BCrypt加密
     */
    @Value("${app.password.encrypt-mode:demo}")
    private String encryptMode;

    /**
     * 加密密码
     * 根据配置的模式选择加密方式
     * 
     * @param plainPassword 明文密码
     * @return 加密后的密码（演示模式返回原密码，生产模式返回BCrypt加密后的密码）
     */
    public String encrypt(String plainPassword) {
        if (StrUtil.isBlank(plainPassword)) {
            throw new IllegalArgumentException("密码不能为空");
        }

        // 演示模式：直接返回明文密码
        if ("demo".equalsIgnoreCase(encryptMode)) {
            return plainPassword;
        }

        // 生产模式：使用BCrypt加密
        if ("production".equalsIgnoreCase(encryptMode)) {
            // 使用hutool的BCrypt类生成加密密码
            return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        }

        // 默认使用演示模式
        return plainPassword;
    }

    /**
     * 验证密码
     * 根据配置的模式选择验证方式
     * 
     * @param plainPassword 用户输入的明文密码
     * @param storedPassword 数据库中存储的密码（可能是明文或加密后的密码）
     * @return true表示密码匹配，false表示不匹配
     */
    public boolean verify(String plainPassword, String storedPassword) {
        if (StrUtil.isBlank(plainPassword) || StrUtil.isBlank(storedPassword)) {
            return false;
        }

        // 演示模式：直接比较明文
        if ("demo".equalsIgnoreCase(encryptMode)) {
            return plainPassword.equals(storedPassword);
        }

        // 生产模式：使用BCrypt验证
        if ("production".equalsIgnoreCase(encryptMode)) {
            // 使用BCrypt.checkpw()方法验证密码
            // 注意：如果存储的密码不是BCrypt格式，可能是旧数据，需要兼容处理
            try {
                return BCrypt.checkpw(plainPassword, storedPassword);
            } catch (Exception e) {
                // 如果验证失败（可能是旧数据），尝试明文比较（兼容旧数据）
                return plainPassword.equals(storedPassword);
            }
        }

        // 默认使用演示模式
        return plainPassword.equals(storedPassword);
    }

    /**
     * 判断密码是否为BCrypt加密格式
     * BCrypt加密后的密码通常以$2a$、$2b$、$2y$开头
     * 
     * @param password 密码字符串
     * @return true表示是BCrypt格式，false表示不是
     */
    public boolean isBcryptFormat(String password) {
        if (StrUtil.isBlank(password)) {
            return false;
        }
        return password.startsWith("$2a$") || 
               password.startsWith("$2b$") || 
               password.startsWith("$2y$");
    }

    /**
     * 获取当前加密模式
     * 
     * @return 加密模式：demo 或 production
     */
    public String getEncryptMode() {
        return encryptMode;
    }

    /**
     * 检查是否需要加密
     * 
     * @return true表示需要加密（生产模式），false表示不需要（演示模式）
     */
    public boolean needEncrypt() {
        return "production".equalsIgnoreCase(encryptMode);
    }
}




