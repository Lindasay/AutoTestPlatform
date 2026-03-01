package com.auto.test.platform.common.util;

import io.restassured.RestAssured;
import io.restassured.response.Response;

/**
 * RestAssured工具类（接口自动化核心，封装GET/POST请求，架构Automation层依赖）
 *
 */
public class RestAssuredUtil {

    /**
     * 初始化配置(全局只需要初始化一次)
     */
    public static void init(){
        //配置请求超时时间（5秒）
        RestAssured.config().getHttpClientConfig().setParam("http.connection.timeout", 5000);
        RestAssured.config().getHttpClientConfig().setParam("http.socket.timeout",5000);

        //关闭SSL验证（避免HTTPS接口报错）
        RestAssured.useRelaxedHTTPSValidation();
    }

    /**
     * GET请求（简化封装）
     * @param url 请求地址
     * @return 响应结果
     */
    public static Response get(String url){
        return RestAssured.given()
                .header("Content-Type", "application/json")
                .when()
                .get(url)
                .then()
                .extract()
                .response();
    }

    /**
     * POST请求（简化封装）
     * @param url 请求地址
     * @param body 请求体（JSON字符串）
     * @return 响应结果
     */
    public static Response post(String url, String body){
        return RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .post(url)
                .then()
                .extract()
                .response();
    }


}
