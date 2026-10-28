package com.student.common.entity;

import lombok.Data;

/**
 * 课程表DTO
 * 用于返回学生课程表信息
 * 
 * @author 段雯菀
 * @date 2025-01
 */
@Data
public class CourseTableDTO {
    private String classId;
    private String courseName;
    private String teacherName;
    private String day;    // 周一
    private String period; // 1-2节
}




