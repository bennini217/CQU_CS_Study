package com.student.grade;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 成绩服务启动类
 * 
 * @author 段雯菀
 * @date 2025-01
 */
@SpringBootApplication(scanBasePackages = {"com.student.grade", "com.student.common"})
@EnableDiscoveryClient
// @EnableFeignClients // grade-service暂时不需要调用其他服务，使用本地Mapper即可
@MapperScan("com.student.grade.mapper")
public class GradeServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GradeServiceApplication.class, args);
        System.out.println("--- 成绩服务启动成功，端口: 8083 ---");
    }
}


