package com.student.common.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 学生实体类
 * 对应数据库表：student
 * 
 * @author 段雯菀
 * @date 2025-01
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("student")
public class Student {
    @TableId
    private String studentId;
    private String name;
    private String gender;
    private String major;
    private Integer gradeYear;
}




