package com.student.management.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.management.common.Result;
import com.student.management.entity.Grade;
import com.student.management.entity.Student;
import com.student.management.entity.StudentSemesterGrade;
import com.student.management.entity.TeachingClass;
import com.student.management.mapper.GradeMapper;
import com.student.management.mapper.StudentMapper;
import com.student.management.mapper.StudentSemesterGradeMapper;
import com.student.management.mapper.TeachingClassMapper;
import com.student.management.util.SqlInjectionProtection;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "仪表盘模块", description = "获取仪表盘统计数据")
public class DashboardController {

    @Autowired
    private GradeMapper gradeMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TeachingClassMapper teachingClassMapper;

    @Autowired
    private StudentSemesterGradeMapper semesterGradeMapper;

    // 学生端仪表盘数据
    @GetMapping("/student")
    @Operation(summary = "学生仪表盘", description = "获取学生的统计数据")
    public Result<Map<String, Object>> getStudentDashboard(@RequestParam String studentId) {
        // SQL注入防护：验证并清理用户输入
        String safeStudentId;
        try {
            safeStudentId = SqlInjectionProtection.validateUserId(studentId);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
        
        // 使用参数化查询，防止SQL注入
        List<Grade> myGrades = gradeMapper.selectList(new LambdaQueryWrapper<Grade>()
                .eq(Grade::getStudentId, safeStudentId));

        int totalCourses = myGrades.size();
        double totalScore = myGrades.stream()
                .mapToDouble(g -> g.getTotalScore().doubleValue())
                .sum();
        double avgScore = totalCourses > 0 ? totalScore / totalCourses : 0;
        int failCount = (int) myGrades.stream()
                .filter(g -> g.getTotalScore().doubleValue() < 60)
                .count();

        // 计算排名
        List<Grade> allGrades = gradeMapper.selectList(null);
        Map<String, BigDecimal> studentTotalScores = new HashMap<>();
        for (Grade grade : allGrades) {
            studentTotalScores.merge(grade.getStudentId(), grade.getTotalScore(), BigDecimal::add);
        }

        List<Student> allStudents = studentMapper.selectList(null);
        int myRank = 1;
        BigDecimal myTotal = studentTotalScores.getOrDefault(safeStudentId, BigDecimal.ZERO);
        for (Student s : allStudents) {
            BigDecimal sTotal = studentTotalScores.getOrDefault(s.getStudentId(), BigDecimal.ZERO);
            if (sTotal.compareTo(myTotal) > 0) {
                myRank++;
            }
        }

        int totalStudents = allStudents.size();
        int percentage = totalStudents > 0 ? (int) ((double) myRank / totalStudents * 100) : 0;

        Map<String, Object> data = new HashMap<>();
        data.put("totalCourses", totalCourses);
        data.put("avgScore", Math.round(avgScore * 10) / 10.0);
        data.put("failCount", failCount);
        data.put("rank", myRank);
        data.put("percentage", percentage);
        data.put("totalStudents", totalStudents);

        return Result.success(data);
    }

    // 教师端仪表盘数据
    @GetMapping("/teacher")
    @Operation(summary = "教师仪表盘", description = "获取教师的统计数据")
    public Result<Map<String, Object>> getTeacherDashboard(@RequestParam String teacherId) {
        // SQL注入防护：验证并清理用户输入
        String safeTeacherId;
        try {
            safeTeacherId = SqlInjectionProtection.validateUserId(teacherId);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
        
        // 使用参数化查询，防止SQL注入
        List<TeachingClass> myClasses = teachingClassMapper.selectList(
                new LambdaQueryWrapper<TeachingClass>().eq(TeachingClass::getTeacherId, safeTeacherId));

        List<String> classIds = myClasses.stream()
                .map(TeachingClass::getClassId)
                .toList();

        List<Grade> myStudentsGrades = gradeMapper.selectList(
                new LambdaQueryWrapper<Grade>().in(Grade::getClassId, classIds));

        int studentCount = (int) myStudentsGrades.stream()
                .map(Grade::getStudentId)
                .distinct()
                .count();

        int failCount = (int) myStudentsGrades.stream()
                .filter(g -> g.getTotalScore().doubleValue() < 60)
                .count();

        Map<String, Object> data = new HashMap<>();
        data.put("classCount", myClasses.size());
        data.put("studentCount", studentCount);
        data.put("pendingCount", 0); // 待批改数量，暂时为0
        data.put("failCount", failCount);

        return Result.success(data);
    }

    // 学生端：获取成绩走势数据（按学期显示）
    @GetMapping("/student/trend")
    @Operation(summary = "学生成绩走势", description = "获取学生各学期的平均成绩走势")
    public Result<Map<String, Object>> getStudentTrend(@RequestParam String studentId) {
        // SQL注入防护：验证并清理用户输入
        String safeStudentId;
        try {
            safeStudentId = SqlInjectionProtection.validateUserId(studentId);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
        
        // 查询该学生的所有学期成绩，按学期排序
        // 使用参数化查询，防止SQL注入
        List<StudentSemesterGrade> semesterGrades = semesterGradeMapper.selectList(
                new LambdaQueryWrapper<StudentSemesterGrade>()
                        .eq(StudentSemesterGrade::getStudentId, safeStudentId)
                        .orderByAsc(StudentSemesterGrade::getSemester));

        // 提取学期名称和平均成绩
        List<String> semesterNames = new java.util.ArrayList<>();
        List<Double> avgScores = new java.util.ArrayList<>();
        
        for (StudentSemesterGrade sg : semesterGrades) {
            semesterNames.add(sg.getSemesterName());
            avgScores.add(sg.getAvgScore().doubleValue());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("semesterNames", semesterNames);
        data.put("avgScores", avgScores);
        
        return Result.success(data);
    }

    // 教师端：获取教学班级及格率数据
    @GetMapping("/teacher/pass-rate")
    @Operation(summary = "教学班级及格率", description = "获取教师所教班级的及格率统计")
    public Result<Map<String, Object>> getTeacherPassRate(@RequestParam String teacherId) {
        // SQL注入防护：验证并清理用户输入
        String safeTeacherId;
        try {
            safeTeacherId = SqlInjectionProtection.validateUserId(teacherId);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
        
        // 使用参数化查询，防止SQL注入
        List<TeachingClass> myClasses = teachingClassMapper.selectList(
                new LambdaQueryWrapper<TeachingClass>().eq(TeachingClass::getTeacherId, safeTeacherId));

        List<String> classIds = myClasses.stream()
                .map(TeachingClass::getClassId)
                .toList();

        List<Grade> myStudentsGrades = gradeMapper.selectList(
                new LambdaQueryWrapper<Grade>().in(Grade::getClassId, classIds));

        int excellent = 0; // 优秀 >= 90
        int good = 0;      // 良好 80-89
        int pass = 0;      // 及格 60-79
        int fail = 0;      // 不及格 < 60

        for (Grade g : myStudentsGrades) {
            double score = g.getTotalScore().doubleValue();
            if (score >= 90) excellent++;
            else if (score >= 80) good++;
            else if (score >= 60) pass++;
            else fail++;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("excellent", excellent);
        data.put("good", good);
        data.put("pass", pass);
        data.put("fail", fail);

        return Result.success(data);
    }
}

