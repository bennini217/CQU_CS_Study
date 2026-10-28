package com.student.student;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 学生服务启动类
 * 
 * @author 段雯菀
 * @date 2025-01
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.student.student.mapper")
@EnableFeignClients  // 启用Feign客户端，用于调用其他服务
public class StudentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(StudentServiceApplication.class, args);
        System.out.println("--- 学生服务启动成功，端口: 8082 ---");
    }
}


