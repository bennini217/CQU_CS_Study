package com.student.common.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 教师实体类
 * 对应数据库表：teacher
 * 
 * @author 段雯菀
 * @date 2025-01
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("teacher")
public class Teacher {
    @TableId
    private String teacherId;
    private String name;
}




