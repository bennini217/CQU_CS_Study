package com.student.management.dto;

import com.student.management.model.Student;

/**
 * 辅助类: 用于存储学生排名所需的数据
 */
public class StudentRank {
    private Student student;
    private double totalScore = 0;
    private double averageScore = 0;
    private int courseCount = 0;

    public StudentRank(Student student) {
        this.student = student;
    }

    public void addScore(double score) {
        this.totalScore += score;
        this.courseCount++;
        // 每次添加时都重新计算平均分
        this.averageScore = (this.courseCount > 0) ? (this.totalScore / this.courseCount) : 0;
    }

    public Student getStudent() {
        return student;
    }

    public double getTotalScore() {
        return totalScore;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public int getCourseCount() {
        return courseCount;
    }
}