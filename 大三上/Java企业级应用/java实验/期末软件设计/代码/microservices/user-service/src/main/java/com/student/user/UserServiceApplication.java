package com.student.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 用户认证服务启动类
 * 
 * @author 段雯菀
 * @date 2025-01
 */
@SpringBootApplication(scanBasePackages = {"com.student.user", "com.student.common"})
@EnableDiscoveryClient  // 启用服务发现，注册到Nacos
@MapperScan("com.student.user.mapper")
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
        System.out.println("--- 用户认证服务启动成功，端口: 8081 ---");
    }
}


