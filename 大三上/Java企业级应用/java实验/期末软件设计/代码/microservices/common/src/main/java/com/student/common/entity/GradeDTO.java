package com.student.common.entity;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 成绩DTO，用于返回给前端，包含scores对象
 * 
 * @author 段雯菀
 * @date 2025-01
 */
@Data
public class GradeDTO {
    private String studentId;
    private String studentName; // 学生姓名，用于教师端显示
    private String classId;
    private String courseId;
    private String courseName;
    private Scores scores;
    private String time;

    @Data
    public static class Scores {
        private Integer normal;
        private Integer midterm;
        private Integer lab;
        private Integer finalExam;
        private BigDecimal comprehensive;
    }
}




