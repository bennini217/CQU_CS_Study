package com.student.management;// ... package ... imports ...

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.student.management.mapper")
public class StudentApplication { // 【关键】这里必须叫 StudentApplication
    public static void main(String[] args) {
        // 【关键】这里也必须是 StudentApplication.class
        SpringApplication.run(StudentApplication.class, args);
        System.out.println("--- 启动成功 ---");
    }
}