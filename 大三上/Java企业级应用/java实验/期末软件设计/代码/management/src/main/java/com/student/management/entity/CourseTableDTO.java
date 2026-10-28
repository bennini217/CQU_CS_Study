package com.student.management.entity;
import lombok.Data;

@Data
public class CourseTableDTO {
    private String classId;
    private String courseName;
    private String teacherName;
    private String day;    // 周一
    private String period; // 1-2节
}