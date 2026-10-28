package com.student.management.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 教学班类
 * 包含教学班号、学期、教师、课程、学生列表
 */
public class TeachingClass {
    private String classId;   // 教学班号
    private String semester;  // 开课学期
    private Teacher teacher;  // 教师 (直接引用Teacher对象)
    private Course course;    // 课程 (直接引用Course对象)

    // 这个列表用来存放所有选了这个教学班的学生
    private List<Student> students;

    public TeachingClass(String classId, String semester, Teacher teacher, Course course) {
        this.classId = classId;
        this.semester = semester;
        this.teacher = teacher;
        this.course = course;
        this.students = new ArrayList<>(); // 初始化一个空的学生列表
    }

    // 添加一个学生到这个班级
    public void addStudent(Student student) {
        if (!this.students.contains(student)) {
            this.students.add(student);
        }
    }

    // 获取班级总人数
    public int getTotalStudents() {
        return this.students.size();
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public List<Student> getStudents() {
        return students;
    }

    @Override
    public String toString() {
        return "TeachingClass{" +
                "classId='" + classId + '\'' +
                ", semester='" + semester + '\'' +
                ", teacher=" + teacher.getName() + // 只显示教师名字
                ", course=" + course.getCourseName() + // 只显示课程名字
                ", totalStudents=" + getTotalStudents() +
                '}';
    }
}
