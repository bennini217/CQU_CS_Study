package com.student.grade.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.common.Result;
import com.student.common.annotation.RequireAuth;
import com.student.common.annotation.RequireRole;
import com.student.common.entity.*;
import com.student.common.util.SqlInjectionProtection;
// import com.student.grade.client.StudentServiceClient; // 暂时不使用Feign调用
import com.student.grade.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 成绩控制器
 * 负责成绩查询、课程表等功能
 * 
 * @author 段雯菀
 * @date 2025-01
 */
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
    private StudentMapper studentMapper; // 使用本地Mapper查询，更快且不依赖其他服务

    // 注意：grade-service目前使用本地StudentMapper，不通过Feign调用student-service
    // 这样可以减少服务间依赖，提高性能
    // @Autowired
    // private StudentServiceClient studentServiceClient;

    /**
     * 查询某个学生的所有成绩
     * 
     * @param studentId 学生ID
     * @return 成绩列表
     */
    @GetMapping("/list")
    @RequireAuth
    @RequireRole("student")
    public Result<List<GradeDTO>> getMyGrades(@RequestParam String studentId) {
        System.out.println("正在查询学生成绩: " + studentId);

        // SQL注入防护：验证并清理用户输入
        String safeStudentId;
        try {
            safeStudentId = SqlInjectionProtection.validateUserId(studentId);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }

        // 使用参数化查询，防止SQL注入
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

    /**
     * 教师端：获取某个教学班的所有成绩
     * 
     * @param classId 班级ID
     * @return 成绩列表
     */
    @GetMapping("/class")
    @RequireAuth
    @RequireRole("teacher")
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
                .orderByDesc(Grade::getTotalScore));

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

            // 查询学生姓名（使用本地Mapper，更快）
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

    /**
     * 教师端：获取该教师教授的班级列表
     * 
     * @param teacherId 教师ID
     * @return 班级列表
     */
    @GetMapping("/my-classes")
    @RequireAuth
    @RequireRole("teacher")
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
            Course course = courseMapper.selectById(tc.getCourseId());
            item.put("courseName", course != null ? course.getCourseName() : "未知课程");
            
            result.add(item);
        }
        return Result.success(result);
    }

    /**
     * 获取学生课程表
     * 
     * @param studentId 学生ID
     * @return 课程表列表
     */
    @GetMapping("/timetable")
    @RequireAuth
    @RequireRole("student")
    public Result<List<CourseTableDTO>> getTimetable(@RequestParam String studentId) {
        // SQL注入防护：验证并清理用户输入
        String safeStudentId;
        try {
            safeStudentId = SqlInjectionProtection.validateUserId(studentId);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
        
        // 使用参数化查询，防止SQL注入
        List<Grade> grades = gradeMapper.selectList(new LambdaQueryWrapper<Grade>()
                .eq(Grade::getStudentId, safeStudentId));

        List<CourseTableDTO> result = new ArrayList<>();

        for (Grade g : grades) {
            // 根据classId查教学班信息
            TeachingClass tc = classMapper.selectById(g.getClassId());
            if (tc != null) {
                // 查询教师名字
                Teacher t = teacherMapper.selectById(tc.getTeacherId());

                CourseTableDTO dto = new CourseTableDTO();
                dto.setClassId(tc.getClassId());
                dto.setCourseName(g.getCourseName());
                dto.setTeacherName(t != null ? t.getName() : "未知教师");
                dto.setDay(tc.getDayOfWeek());
                dto.setPeriod(tc.getTimePeriod());
                result.add(dto);
            }
        }
        return Result.success(result);
    }
}


