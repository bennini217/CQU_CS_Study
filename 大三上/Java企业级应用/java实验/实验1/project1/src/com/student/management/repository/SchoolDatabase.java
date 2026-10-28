package com.student.management.repository;

import com.student.management.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 模拟数据库
 * 使用静态列表来存储所有数据，方便在程序各处访问
 */
public class SchoolDatabase {

    // 这些列表是全局的，程序的任何地方都能访问

    public static List<Student> students = new ArrayList<>();
    public static List<Teacher> teachers = new ArrayList<>();
    public static List<Course> courses = new ArrayList<>();
    public static List<TeachingClass> teachingClasses = new ArrayList<>();
    public static List<Grade> grades = new ArrayList<>();

}