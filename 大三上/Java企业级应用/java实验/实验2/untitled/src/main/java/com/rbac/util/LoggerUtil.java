package com.rbac.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoggerUtil {

    // 为"审计" (Audit) 创建一个专门的日志记录器
    // 我们的"权限审计"日志会全部用这个记录器
    private static final Logger auditLogger = LogManager.getLogger("AuditLogger");

    // 为"应用" (Application) 创建一个通用的日志记录器
    // 用于记录程序错误、调试信息等
    private static final Logger appLogger = LogManager.getLogger(LoggerUtil.class);

    // 静态方法，用于记录审计日志
    public static void audit(String message) {
        auditLogger.info(message);
    }

    // 静态方法，用于记录普通信息日志
    public static void info(String message) {
        appLogger.info(message);
    }

    // 静态方法，用于记录错误日志
    public static void error(String message, Throwable t) {
        appLogger.error(message, t);
    }
}