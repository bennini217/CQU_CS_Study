package com.student.management.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("学生成绩管理系统后端 API")
                        .version("1.0")
                        .description("实验4成果展示：包含用户认证、RBAC权限、成绩查询、AI智能助教等功能接口。")
                        .contact(new Contact().name("段雯菀").email("20231326@school.edu.cn")));
    }
}