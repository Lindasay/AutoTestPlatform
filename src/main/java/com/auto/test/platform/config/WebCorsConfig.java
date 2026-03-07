package com.auto.test.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * Web跨域配置类（架构config层必备，解决前端调用后端接口跨域问题）
 * 与yml中跨域配置互补，支持更精细的跨域规则
 */
@Configuration
public class WebCorsConfig {

    /**
     * 配置跨域过滤器（核心Bean）
     */
    @Bean
    public CorsFilter corsFilter() {
        //1.配置跨域规则
        CorsConfiguration config = new CorsConfiguration();

        // 允许的来源模式
        config.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",            // 本地开发所有端口
                "http://127.0.0.1:*",           // 本地开发所有端口
                "https://*.test.com",           // 测试环境所有子域名
                "https://*.prod.com"            // 生产环境所有子域名（根据实际情况修改）
        ));

        // 允许所有请求头（如Token、Content-Type等）
        config.addAllowedHeader("*");
        // 允许所有请求方法（GET、POST、PUT、DELETE等）
        config.addAllowedMethod("*");
        // 允许携带Cookie（适配后续用户登录、会话保持场景）
        config.setAllowCredentials(true);
        // 跨域请求有效期（3600秒，避免频繁发起预检请求）
        config.setMaxAge(3600L);

        // 2. 配置跨域规则生效的URL（所有接口都生效）
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        // 3. 返回跨域过滤器
        return new CorsFilter(source);
    }
}
