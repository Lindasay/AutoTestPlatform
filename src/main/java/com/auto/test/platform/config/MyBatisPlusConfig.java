package com.auto.test.platform.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus配置类（适配Spring Boot 3.3.8 + 3.5.7正式版）
 * 注意：@MapperScan仅在启动类配置，避免重复扫描
 * 核心、分页插件、Mapper扫描等，架构必备
 */
@Configuration
@MapperScan(basePackages = "com.auto.test.platform.mapper") // 仅在此处配置Mapper扫描，启动类不再配置，避免重复扫描
public class MyBatisPlusConfig {

    /**
     * 分页插件（核心，支持MyBatis-Plus分页查询）
     * 关键修复：添加@Bean注解，让Spring注入插件；修复重复添加分页插件的问题
     */
    @Bean // 核心：添加该注解，否则插件无法生效
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 修复：统一配置分页插件（原代码重复添加了2次PaginationInnerInterceptor，导致冲突）
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setOverflow(true); // 页码溢出自动处理（分页合理化）
        paginationInterceptor.setMaxLimit(100L); // 限制最大单页条数（与接口文档的pageSize最大100匹配）
        interceptor.addInnerInterceptor(paginationInterceptor);

        return interceptor;
    }
}
