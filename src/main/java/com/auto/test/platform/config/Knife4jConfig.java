package com.auto.test.platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;


/**
 * Knife4j接口文档配置类（最终可用版）
 * 核心作用：配置接口扫描路径、文档基本信息，确保doc.html能正常生成和访问
 *
 */
@Configuration // 标识为配置类，Spring启动时加载
public class Knife4jConfig {

    /**
     * 核心配置：创建 OpenAPI 对象（替代原 Docket 对象）
     * 配置接口文档基础信息，Knife4j 自动扫描 controller 接口生成 doc.html
     */
    @Bean
    public OpenAPI autoTestPlatformOpenAPI() {
        // 1. 创建并配置 Server 对象
        Server server = new Server();
        server.setUrl("/auto-test");
        server.setDescription("默认服务器地址");

        // 2. 定义安全方案（Authorization 头）
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .description("鉴权 Token，格式为：用户名-角色-时间戳（不需要Bearer前缀）");

        // 3. 创建安全要求
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("Authorization");

        // 2. 将 Server 对象包装成 List 并传入
        return new OpenAPI()
                .info(new Info()
                        .title("自动化测试平台 - 接口文档")
                        .description("包含项目管理、测试用例管理等核心接口，支持接口调试、参数校验")
                        .version("1.0.0"))
                .servers(Collections.singletonList(server))
                .schemaRequirement("Authorization", securityScheme)
                .security(Collections.singletonList(securityRequirement));
    }

}