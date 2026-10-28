package com.student.teacher;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 教师服务启动类
 * 
 * @author 段雯菀
 * @date 2025-01
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.student.teacher.mapper")
public class TeacherServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TeacherServiceApplication.class, args);
        System.out.println("--- 教师服务启动成功，端口: 8084 ---");
    }
}




