package com.student.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * 跨域配置类
 * Gateway使用WebFlux，需要通过CorsWebFilter配置CORS
 * 
 * @author 段雯菀
 * @date 2025-01
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许所有来源（使用OriginPattern，支持通配符和credentials）
        config.addAllowedOriginPattern("*");
        config.addAllowedMethod("*");  // 允许所有HTTP方法
        config.addAllowedHeader("*");  // 允许所有请求头
        config.setAllowCredentials(true);  // 允许携带凭证
        config.setMaxAge(3600L);  // 预检请求缓存时间

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}




