package com.student.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限管理注解：标记需要特定角色的接口（装饰性注解，不实际执行验证）
 * 使用此注解的接口表示需要用户具有指定的角色（student或teacher）才能访问
 * 
 * @author 段雯菀
 * @date 2025-01
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    /**
     * 允许的角色列表
     * 例如：{"student"} 或 {"teacher"} 或 {"student", "teacher"}
     */
    String[] value();
}





