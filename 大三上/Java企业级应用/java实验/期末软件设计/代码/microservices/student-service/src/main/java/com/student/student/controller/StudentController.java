package com.student.student.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.common.Result;
import com.student.common.annotation.RequireAuth;
import com.student.common.annotation.RequireRole;
import com.student.common.entity.Student;
import com.student.common.util.SqlInjectionProtection;
import com.student.student.mapper.StudentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 学生控制器
 * 提供学生信息查询接口
 * 
 * @author 段雯菀
 * @date 2025-01
 */
@RestController
@RequestMapping("/api/student")
@RequireAuth
@RequireRole({"student", "teacher"})  // 学生和教师都可以查询学生信息
public class StudentController {

    @Autowired
    private StudentMapper studentMapper;

    /**
     * 根据学生ID查询学生信息
     * 
     * @param studentId 学生ID
     * @return 学生信息
     */
    @GetMapping("/{studentId}")
    public Result<Student> getStudent(@PathVariable String studentId) {
        // SQL注入防护：验证并清理用户输入
        String safeStudentId;
        try {
            safeStudentId = SqlInjectionProtection.validateUserId(studentId);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
        
        // 使用参数化查询，防止SQL注入
        Student student = studentMapper.selectById(safeStudentId);
        if (student == null) {
            return Result.error("学生不存在");
        }
        
        return Result.success(student);
    }
}




