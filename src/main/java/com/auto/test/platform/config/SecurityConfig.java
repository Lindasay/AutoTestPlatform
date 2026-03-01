package com.auto.test.platform.config;

import com.auto.test.platform.common.interceptor.TokenInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Security配置类（适配SpringBoot 3.3.8版本）
 * 修复requestMatcher私有、requestMatchers传参错误问题，语法100%兼容
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer {

    @Resource
    private TokenInterceptor tokenInterceptor;

    // 密码加密器（SpringBoot 3.3.8标准写法）
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 认证管理器（适配SpringBoot 3.3.8）
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Security过滤器链
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 关闭不必要的安全配置:CSRF、表单登录、HTTP Basic
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                // 接口权限配置
                .authorizeHttpRequests(auth -> auth
                        // 1. 放行Swagger所有路径（拆分多个requestMatchers，避免传参错误）
                        .requestMatchers("/doc.html").permitAll()
                        .requestMatchers("/webjars/**").permitAll()
                        .requestMatchers("/swagger-resources/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/favicon.ico").permitAll()
                        // 带上下文的Swagger路径
                        .requestMatchers("/auto-test/doc.html").permitAll()
                        .requestMatchers("/auto-test/webjars/**").permitAll()
                        .requestMatchers("/auto-test/swagger-resources/**").permitAll()
                        .requestMatchers("/auto-test/v3/api-docs/**").permitAll()
                        .requestMatchers("/auto-test/swagger-ui/**").permitAll()
                        .requestMatchers("/auto-test/favicon.ico").permitAll()
                        // 2. 放行用户登录注册接口
                        .requestMatchers("/user/login", "/user/register","/user/getByUsername").permitAll()
                        .requestMatchers("/project/**","/testCase/**").permitAll()
                        .requestMatchers("/report/**","/static/**","/css/**", "/js/**", "/images/**").permitAll()
                        // 3. 其他接口需认证
                        .anyRequest().authenticated()
                );
        return http.build();
    }

    // Token拦截器配置
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 不需要Token的接口
                        "/user/login", "/user/register", "/user/getByUsername","/project/**","/testCase/**","/report/**","/static/**","/css/**", "/js/**", "/images/**",
                        // Swagger路径（不带上下文）
                        "/doc.html", "/webjars/**", "/swagger-resources/**", "/v3/api-docs/**", "/swagger-ui/**", "/favicon.ico",
                        // Swagger路径（带上下文）
                        "/auto-test/doc.html", "/auto-test/webjars/**", "/auto-test/swagger-resources/**", "/auto-test/v3/api-docs/**", "/auto-test/swagger-ui/**", "/auto-test/favicon.ico"
                );
    }
}