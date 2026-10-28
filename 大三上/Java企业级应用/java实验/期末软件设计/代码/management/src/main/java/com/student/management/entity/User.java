package com.student.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_user") // 对应数据库表名
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String userType; // 'STUDENT' 或 'TEACHER'
    // 对应实验3前端需要的 name 字段，这里假设数据库里也有，或者根据业务逻辑获取
    private String name;
}