package com.student.management.util;

import cn.hutool.core.util.StrUtil;

/**
 * SQL注入防护工具类
 * 用于过滤和验证用户输入，防止SQL注入攻击
 */
public class SqlInjectionProtection {

    /**
     * SQL注入危险字符列表
     */
    private static final String[] DANGEROUS_CHARS = {
        "'", "\"", ";", "--", "/*", "*/", "xp_", "sp_", 
        "exec", "execute", "select", "insert", "update", 
        "delete", "drop", "alter", "create", "union", 
        "script", "<", ">", "&", "|", "&&", "||"
    };

    /**
     * 检查字符串是否包含SQL注入危险字符
     * 
     * @param input 待检查的字符串
     * @return true表示安全，false表示存在危险字符
     */
    public static boolean isSafe(String input) {
        if (StrUtil.isBlank(input)) {
            return true; // 空字符串视为安全
        }

        String lowerInput = input.toLowerCase().trim();
        
        // 检查是否包含危险字符
        for (String dangerous : DANGEROUS_CHARS) {
            if (lowerInput.contains(dangerous.toLowerCase())) {
                return false;
            }
        }

        return true;
    }

    /**
     * 过滤字符串中的SQL注入危险字符
     * 将危险字符替换为空字符串
     * 
     * @param input 待过滤的字符串
     * @return 过滤后的安全字符串
     */
    public static String filter(String input) {
        if (StrUtil.isBlank(input)) {
            return input;
        }

        String result = input;
        for (String dangerous : DANGEROUS_CHARS) {
            result = result.replace(dangerous, "");
            result = result.replace(dangerous.toUpperCase(), "");
        }

        return result.trim();
    }

    /**
     * 验证并清理用户输入
     * 如果包含危险字符，返回null或抛出异常
     * 
     * @param input 用户输入
     * @param fieldName 字段名称（用于错误提示）
     * @return 清理后的安全字符串
     * @throws IllegalArgumentException 如果输入包含危险字符
     */
    public static String validateAndClean(String input, String fieldName) {
        if (StrUtil.isBlank(input)) {
            return input;
        }

        if (!isSafe(input)) {
            throw new IllegalArgumentException(
                String.format("参数[%s]包含非法字符，可能存在SQL注入风险", fieldName)
            );
        }

        return input.trim();
    }

    /**
     * 验证用户ID格式（S或T开头+3位数字）
     * 同时检查SQL注入风险
     * 
     * @param userId 用户ID
     * @return 验证后的安全用户ID
     * @throws IllegalArgumentException 如果格式不正确或包含危险字符
     */
    public static String validateUserId(String userId) {
        if (StrUtil.isBlank(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        // 检查SQL注入风险
        if (!isSafe(userId)) {
            throw new IllegalArgumentException("用户ID包含非法字符，可能存在SQL注入风险");
        }

        // 验证格式：S或T开头，后跟3位数字
        if (!userId.matches("^[ST]\\d{3}$")) {
            throw new IllegalArgumentException("用户ID格式错误，应为S或T开头后跟3位数字（如：S001、T001）");
        }

        return userId.toUpperCase();
    }

    /**
     * 验证密码安全性
     * 检查长度和SQL注入风险
     * 
     * @param password 密码
     * @return 验证后的安全密码
     * @throws IllegalArgumentException 如果密码不安全
     */
    public static String validatePassword(String password) {
        if (StrUtil.isBlank(password)) {
            throw new IllegalArgumentException("密码不能为空");
        }

        // 检查SQL注入风险（密码可以包含特殊字符，但需要过滤SQL关键字）
        String lowerPassword = password.toLowerCase();
        String[] sqlKeywords = {"select", "insert", "update", "delete", "drop", "alter", "create", "exec", "execute"};
        for (String keyword : sqlKeywords) {
            if (lowerPassword.contains(keyword)) {
                throw new IllegalArgumentException("密码不能包含SQL关键字");
            }
        }

        // 检查长度
        if (password.length() < 6) {
            throw new IllegalArgumentException("密码长度不能小于6位");
        }

        return password;
    }
}
