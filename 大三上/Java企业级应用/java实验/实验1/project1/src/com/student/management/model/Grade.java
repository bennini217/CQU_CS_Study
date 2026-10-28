package com.student.management.model;

import java.time.LocalDateTime;

/**
 * 成绩类
 * 记录一个学生在一个教学班的各项成绩
 */
public class Grade {
    private Student student;         // 关联的学生
    private TeachingClass teachingClass; // 关联的教学班

    // 4部分成绩
    private int normalScore;     // 平时成绩
    private int midtermScore;    // 期中考试
    private int labScore;        // 实验成绩
    private int finalExamScore;  // 期末考试

    private double comprehensiveScore; // 综合成绩
    private LocalDateTime recordTime;    // 成绩取得时间

    public Grade(Student student, TeachingClass teachingClass) {
        this.student = student;
        this.teachingClass = teachingClass;
        // 初始化成绩为0
        this.normalScore = 0;
        this.midtermScore = 0;
        this.labScore = 0;
        this.finalExamScore = 0;
        this.comprehensiveScore = 0.0;
    }

    /**
     * 计算综合成绩
     * 策略：平时20%, 期中20%, 实验20%, 期末40%
     */
    public void calculateComprehensiveScore() {
        // 计算综合成绩
        this.comprehensiveScore =
                this.normalScore * 0.2 +
                        this.midtermScore * 0.2 +
                        this.labScore * 0.2 +
                        this.finalExamScore * 0.4;

        // 记录计算时间
        this.recordTime = LocalDateTime.now();
    }

    // 4项成绩用Setter 综合成绩只有Getter

    public Student getStudent() {
        return student;
    }

    public TeachingClass getTeachingClass() {
        return teachingClass;
    }

    public int getNormalScore() {
        return normalScore;
    }

    public void setNormalScore(int normalScore) {
        this.normalScore = normalScore;
    }

    public int getMidtermScore() {
        return midtermScore;
    }

    public void setMidtermScore(int midtermScore) {
        this.midtermScore = midtermScore;
    }

    public int getLabScore() {
        return labScore;
    }

    public void setLabScore(int labScore) {
        this.labScore = labScore;
    }

    public int getFinalExamScore() {
        return finalExamScore;
    }

    public void setFinalExamScore(int finalExamScore) {
        this.finalExamScore = finalExamScore;
    }

    public double getComprehensiveScore() {
        return comprehensiveScore;
    }

    public LocalDateTime getRecordTime() {
        return recordTime;
    }

    @Override
    public String toString() {
        return "Grade{" +
                "student=" + student.getName() +
                ", course=" + teachingClass.getCourse().getCourseName() +
                ", comprehensiveScore=" + comprehensiveScore +
                '}';
    }
}
