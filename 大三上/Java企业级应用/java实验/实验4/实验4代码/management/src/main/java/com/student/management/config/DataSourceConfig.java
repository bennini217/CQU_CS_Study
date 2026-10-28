package com.student.management.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();

        // 1. 驱动配置
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // 2. 数据库地址 (注意 student_db 是你的数据库名)
        dataSource.setJdbcUrl("jdbc:mysql://localhost:3306/student_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true");

        // 3. 账号
        dataSource.setUsername("root");

        // 4. 密码 【重要：请务必确认这里填的是你刚才测试连接成功的密码】
        // 我看你之前的截图里写的是 000000，如果不对请改成 123456 或 root
        dataSource.setPassword("000000");

        return dataSource;
    }
}