package com.student.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户实体类
 * 对应数据库表：sys_user
 * 
 * @author 段雯菀
 * @date 2025-01
 */
@Data
@TableName("sys_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String userType; // 'STUDENT' 或 'TEACHER'
    private String name; // 用户姓名
}




