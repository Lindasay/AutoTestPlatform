package com.auto.test.platform.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;




/**
 * MyBatis-Plus配置类（适配Spring Boot 3.3.8 + 3.5.7正式版）
 * 注意：@MapperScan仅在启动类配置，避免重复扫描
 * 核心：通过自定义SqlSessionFactory规避factoryBeanObjectType类型冲突
 */
@Configuration
// 仅在此处配置Mapper扫描，启动类不再配置，避免重复扫描
@MapperScan(basePackages = "com.auto.test.platform.mapper")
public class MyBatisPlusConfig {

   // 分页插件
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
