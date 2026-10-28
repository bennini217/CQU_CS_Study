package com.student.common.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 课程实体类
 * 对应数据库表：course
 * 
 * @author 段雯菀
 * @date 2025-01
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("course")
public class Course {
    @TableId
    private String courseId;
    private String courseName;
    private Integer credit;
}




