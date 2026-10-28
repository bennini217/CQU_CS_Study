package com.student.management.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.management.common.Result;
import com.student.management.entity.CourseTableDTO;
import com.student.management.entity.Grade;
import com.student.management.entity.GradeDTO;
import com.student.management.entity.Student;
import com.student.management.entity.Teacher;
import com.student.management.entity.TeachingClass;
import com.student.management.mapper.CourseMapper;
import com.student.management.mapper.GradeMapper;
import com.student.management.mapper.StudentMapper;
import com.student.management.mapper.TeacherMapper;
import com.student.management.mapper.TeachingClassMapper;
import com.student.management.util.SqlInjectionProtection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/grade")
public class GradeController {

    @Autowired
    private GradeMapper gradeMapper;

    @Autowired
    private TeachingClassMapper classMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private StudentMapper studentMapper;

    // 接口功能：查询某个学生的所有成绩
    // 调用方式：GET /api/grade/list?studentId=S001
    @GetMapping("/list")
    public Result<List<GradeDTO>> getMyGrades(@RequestParam String studentId) {
        System.out.println("正在查询学生成绩: " + studentId);

        // SQL注入防护：验证并清理用户输入
        String safeStudentId;
        try {
            safeStudentId = SqlInjectionProtection.validateUserId(studentId);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }

        // 使用 MyBatis-Plus 的 LambdaQueryWrapper 参数化查询
        // 所有参数通过预编译方式传入，避免SQL注入
        // 生成的SQL: SELECT * FROM grade WHERE student_id = ? (参数化)
        List<Grade> list = gradeMapper.selectList(new LambdaQueryWrapper<Grade>()
                .eq(Grade::getStudentId, safeStudentId));

        // 转换为前端期望的格式
        List<GradeDTO> result = list.stream().map(grade -> {
            GradeDTO dto = new GradeDTO();
            dto.setStudentId(grade.getStudentId());
            dto.setClassId(grade.getClassId());
            dto.setCourseId(grade.getCourseId());
            dto.setCourseName(grade.getCourseName());
            dto.setTime(grade.getRecordTime() != null ? 
                grade.getRecordTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");

            GradeDTO.Scores scores = new GradeDTO.Scores();
            scores.setNormal(grade.getNormalScore());
            scores.setMidterm(grade.getMidtermScore());
            scores.setLab(grade.getLabScore());
            scores.setFinalExam(grade.getFinalScore());
            scores.setComprehensive(grade.getTotalScore());
            dto.setScores(scores);

            return dto;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    // 教师端：获取某个教学班的所有成绩
    @GetMapping("/class")
    public Result<List<GradeDTO>> getClassGrades(@RequestParam String classId) {
        System.out.println("查询班级成绩: " + classId);
        
        // SQL注入防护：验证并清理用户输入
        String safeClassId;
        try {
            safeClassId = SqlInjectionProtection.validateAndClean(classId, "classId");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
        
        // 使用参数化查询，防止SQL注入
        List<Grade> list = gradeMapper.selectList(new LambdaQueryWrapper<Grade>()
                .eq(Grade::getClassId, safeClassId)
                .orderByDesc(Grade::getTotalScore)); // 按总分降序

        // 转换为前端期望的格式，并添加学生姓名
        List<GradeDTO> result = new ArrayList<>();
        for (Grade grade : list) {
            GradeDTO dto = new GradeDTO();
            dto.setStudentId(grade.getStudentId());
            dto.setClassId(grade.getClassId());
            dto.setCourseId(grade.getCourseId());
            dto.setCourseName(grade.getCourseName());
            dto.setTime(grade.getRecordTime() != null ? 
                grade.getRecordTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");

            // 查询学生姓名
            Student student = studentMapper.selectById(grade.getStudentId());
            dto.setStudentName(student != null ? student.getName() : "未知学生");

            GradeDTO.Scores scores = new GradeDTO.Scores();
            scores.setNormal(grade.getNormalScore());
            scores.setMidterm(grade.getMidtermScore());
            scores.setLab(grade.getLabScore());
            scores.setFinalExam(grade.getFinalScore());
            scores.setComprehensive(grade.getTotalScore());
            dto.setScores(scores);

            result.add(dto);
        }

        return Result.success(result);
    }

    // 教师端：获取该教师教授的班级列表 (给下拉菜单用)
    @GetMapping("/my-classes")
    public Result<List<Map<String, Object>>> getTeacherClasses(@RequestParam String teacherId) {
        // SQL注入防护：验证并清理用户输入
        String safeTeacherId;
        try {
            safeTeacherId = SqlInjectionProtection.validateUserId(teacherId);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
        
        // 使用参数化查询，防止SQL注入
        List<TeachingClass> list = classMapper.selectList(new LambdaQueryWrapper<TeachingClass>()
                .eq(TeachingClass::getTeacherId, safeTeacherId));
        
        // 转换为前端期望的格式，包含课程名称
        List<Map<String, Object>> result = new ArrayList<>();
        for (TeachingClass tc : list) {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("id", tc.getClassId());
            item.put("classId", tc.getClassId());
            item.put("courseId", tc.getCourseId());
            item.put("teacherId", tc.getTeacherId());
            item.put("semester", tc.getSemester());
            item.put("day", tc.getDayOfWeek());
            item.put("period", tc.getTimePeriod());
            
            // 查询课程名称
            com.student.management.entity.Course course = courseMapper.selectById(tc.getCourseId());
            item.put("courseName", course != null ? course.getCourseName() : "未知课程");
            
            result.add(item);
        }
        return Result.success(result);
    }
 
    // 🔥 新接口：获取真实课表
    @GetMapping("/timetable")
    public Result<List<CourseTableDTO>> getTimetable(@RequestParam String studentId) {
        // SQL注入防护：验证并清理用户输入
        String safeStudentId;
        try {
            safeStudentId = SqlInjectionProtection.validateUserId(studentId);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
        
        // 1. 先查出学生选的所有课 (查 grade 表)
        // 使用参数化查询，防止SQL注入
        List<Grade> grades = gradeMapper.selectList(new LambdaQueryWrapper<Grade>()
                .eq(Grade::getStudentId, safeStudentId));

        List<CourseTableDTO> result = new ArrayList<>();

        for (Grade g : grades) {
            // 2. 根据 classId 查教学班信息 (获取真实时间)
            TeachingClass tc = classMapper.selectById(g.getClassId());
            if (tc != null) {
                // 3. 查老师名字
                Teacher t = teacherMapper.selectById(tc.getTeacherId());

                CourseTableDTO dto = new CourseTableDTO();
                dto.setClassId(tc.getClassId());
                dto.setCourseName(g.getCourseName());
                dto.setTeacherName(t != null ? t.getName() : "未知教师");
                dto.setDay(tc.getDayOfWeek());   // 数据库里的真实周几
                dto.setPeriod(tc.getTimePeriod()); // 数据库里的真实节次
                result.add(dto);
            }
        }
        return Result.success(result);
    }
}