package com.auto.test.platform.config;

import com.auto.test.platform.common.interceptor.TokenInterceptor;
import jakarta.annotation.Resource;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Spring Security配置类（适配SpringBoot 3.3.8版本）
 * 修复requestMatcher私有、requestMatchers传参错误问题，语法100%兼容
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer {

    @Resource
    private TokenInterceptor tokenInterceptor;

    @Value("${allure.report.directory:./target/allure-report}")
    private String allureReportDir;

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
                        // 1. 必须放行错误路径
                        .requestMatchers("/error", "/auto-test/error").permitAll()

                        // 2. Swagger相关
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

                        // 3. 静态资源
                        .requestMatchers("/static/**").permitAll()
                        .requestMatchers("/report/**").permitAll()
                        .requestMatchers("/allure/**").permitAll()
                        .requestMatchers("/auto-test/static/**").permitAll()
                        .requestMatchers("/auto-test/report/**").permitAll()
                        .requestMatchers("/auto-test/allure/**").permitAll()
                        .requestMatchers("/index.html").permitAll()
                        .requestMatchers("/auto-test/index.html").permitAll()

                        // 4. 业务接口
                        .requestMatchers("/user/login", "/user/register", "/user/getByUsername").permitAll()
                        .requestMatchers("/project/**").permitAll()
                        .requestMatchers("/testCase/**").permitAll()
                        .requestMatchers("/reportData/**").permitAll()
                        .requestMatchers("/schedule/**").permitAll()

                        // 5. 其他请求需要认证
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
                        "/error", "/auto-test/error",  // 添加错误路径排除
                        "/user/login", "/user/register", "/user/getByUsername",
                        "/project/**", "/testCase/**", "/reportData/**", "/schedule/**",
                        "/doc.html", "/webjars/**", "/swagger-resources/**",
                        "/v3/api-docs/**", "/swagger-ui/**",
                        "/auto-test/doc.html", "/auto-test/webjars/**",
                        "/auto-test/swagger-resources/**", "/auto-test/v3/api-docs/**",
                        "/auto-test/swagger-ui/**",
                        "/static/**", "/report/**","/allure/**",
                        "/auto-test/static/**", "/auto-test/report/**","/auto-test/allure/**",
                        "/index.html", "/auto-test/index.html"
                );
    }

    //静态资源映射
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. 报告目录映射（本地report文件夹）
        String reportPath = "file:" + System.getProperty("user.dir") + "/report/";
        registry.addResourceHandler("/report/**")
                .addResourceLocations(reportPath);
        registry.addResourceHandler("/auto-test/report/**")
                .addResourceLocations(reportPath);

        // 2. Allure报告目录映射
        File allureDir = new File(allureReportDir);
        String allurePath = "file:" + allureDir.getAbsolutePath() + "/";
        registry.addResourceHandler("/allure/**")
                .addResourceLocations(allurePath)
                .setCachePeriod(3600)
                .resourceChain(true);
        registry.addResourceHandler("/auto-test/allure/**")
                .addResourceLocations(allurePath)
                .setCachePeriod(3600)
                .resourceChain(true);

        // 3. 静态资源映射（classpath:/static/）
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/auto-test/static/**")
                .addResourceLocations("classpath:/static/");

        // 4. 直接映射index.html（方便访问）
        registry.addResourceHandler("/index.html")
                .addResourceLocations("classpath:/static/index.html");
        registry.addResourceHandler("/auto-test/index.html")
                .addResourceLocations("classpath:/static/index.html");

        // 打印日志，便于调试
        System.out.println("=== 静态资源映射配置 ===");
        System.out.println("报告目录本地路径: " + reportPath);
        System.out.println("静态资源本地路径: classpath:/static/");
    }
}