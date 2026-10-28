package com.student.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生学期平均成绩表
 * 用于存储学生每个学期的平均成绩，用于成绩走势分析
 */
@Data
@TableName("student_semester_grade")
public class StudentSemesterGrade {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String studentId;      // 学生ID
    private String semester;        // 学期，如：2021-Autumn, 2022-Spring, 2022-Autumn, 2023-Spring, 2023-Autumn
    private String semesterName;    // 学期名称，如：大一上、大一下、大二上、大二下、大三上
    private BigDecimal avgScore;    // 该学期平均成绩
    private Integer courseCount;    // 该学期课程数量
    private LocalDateTime recordTime; // 记录时间
}



