package com.student.grade.client;

import com.student.common.Result;
import com.student.common.entity.Student;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 学生服务Feign客户端
 * 用于调用student-service服务
 * 
 * @author 段雯菀
 * @date 2025-01
 */
@FeignClient(name = "student-service", path = "/api/student")
public interface StudentServiceClient {
    
    /**
     * 根据学生ID查询学生信息
     * 
     * @param studentId 学生ID
     * @return 学生信息
     */
    @GetMapping("/{studentId}")
    Result<Student> getStudent(@PathVariable String studentId);
}




