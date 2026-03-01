package com.auto.test.platform.common.config;


import com.auto.test.platform.common.util.LogUtil;
import com.auto.test.platform.script.TokenContext;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.testng.ITestContext;


/**
 * 企业级请求头配置类（统一管理，适配权限拦截/无权限场景）
 * 规范：1. 基础请求头统一配置；2. Token按需加载；3. 支持自定义扩展
 * 请求头统一配置：自动携带Token，初始化基础URL
 */
@Slf4j
public class RequestHeaderConfig {

    // 基础请求路径（与工程server.servlet.context-path一致）
    public static final String BASE_URL = "http://localhost:8080/auto-test";

    /**
     * 初始化请求配置（读取testng.xml的全局参数baseUrl）
     */
    public static void init(ITestContext context) {
        //读取testng.xml的全局参数baseUrl
        String baseUrl = context.getCurrentXmlTest().getParameter("baseUrl");
        if (baseUrl != null && !baseUrl.isEmpty()) {
            RestAssured.baseURI = baseUrl;
        }else {
            RestAssured.baseURI = BASE_URL; //默认地址
        }

        //开启请求/响应日志（验证失败时打印）
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        LogUtil.info("请求基础URL初始化完成：{}", RestAssured.baseURI);
    }

    /**
     * 获取带鉴权的基础请求对象：自动添加Token和通用头
     */
    //获取基础请求头（所有接口通用）
    public static RequestSpecification getAuthRequest() {

        //获取Token
        String token = TokenContext.getGlobalAuthToken();

        log.info("🔑 使用Token发起请求: {}...", token.trim());

        return RestAssured.given()
                .header("Content-Type", "application/json;charset=utf-8")
                .header("Accept", "application/json")
                .header("Authorization",token.trim());
    }

    /**
     * 获取无鉴权的基础请求对象（公开接口使用）
     */
    public static RequestSpecification getNoAuthRequest() {
        log.info("使用无Token请求（公开接口）");
        return RestAssured.given()
                .header("Content-Type", "application/json;charset=UTF-8")
                .header("Accept", "application/json");
    }
}
