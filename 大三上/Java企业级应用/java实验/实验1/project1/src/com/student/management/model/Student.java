package com.student.management.model;

import java.util.Objects; // 稍后用于比较对象

/**
 * 学生类
 * 包含学号、姓名、性别
 */
public class Student {
    private String studentId; // 学号
    private String name;      // 姓名
    private String gender;    // 性别

    // 构造函数
    public Student(String studentId, String name, String gender) {
        this.studentId = studentId;
        this.name = name;
        this.gender = gender;
    }

    // 获取私有属性值
    public String getStudentId() {
        return studentId;
    }

    // 设置私有属性值
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    // 打印学生信息
    @Override
    public String toString() {
        return "Student{" +
                "studentId='" + studentId + '\'' +
                ", name='" + name + '\'' +
                ", gender='" + gender + '\'' +
                '}';
    }

    // 根据学号判断是不是同一个学生
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Student student = (Student) o;
        return Objects.equals(studentId, student.studentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId);
    }
}
