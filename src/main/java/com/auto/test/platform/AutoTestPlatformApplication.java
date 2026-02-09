package com.auto.test.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * 自动化平台启动类（JDK17+Spring Boot3.5.10）
 * 启动类（适配Spring Boot 3.3.8 + MyBatis-Plus 3.5.7）
 * 指定factoryBean解决factoryBeanObjectType类型冲突问题
 * 项目核心入口
 */
@SpringBootApplication
public class AutoTestPlatformApplication {

	public static void main(String[] args) {

		//启动Spring Boot项目
		SpringApplication.run(AutoTestPlatformApplication.class, args);

		//启动成功提示（控制台打印）
		System.out.println("=====================================");
		System.out.println("✅ 自动化测试平台启动成功（JDK17+Spring Boot3.3.8）");
		System.out.println("🔗 访问地址：http://localhost:8080/auto-test");
		System.out.println("🗄️  数据库：MySQL test_platform");
		System.out.println("=====================================");
	}

}
