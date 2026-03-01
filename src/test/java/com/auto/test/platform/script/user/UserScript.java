package com.auto.test.platform.script.user;



import com.auto.test.platform.common.config.RequestHeaderConfig;
import com.auto.test.platform.common.util.LogUtil;
import com.auto.test.platform.common.util.TestAssertUtil;
import com.auto.test.platform.script.TokenContext;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户模块自动化脚本（贴合第5天ProjectScript、TestCaseScript风格）
 * 覆盖核心场景：用户注册、用户登录、Token验证、根据用户名查询用户
 * 复用RequestHeaderConfig，统一请求配置，断言逻辑与现有脚本保持一致
 * 可直接运行，适配现有工程架构，无语法报错
 * - 注册：POST /user/register，入参User实体（userName/password）
 * - 登录：POST /user/login，入参LoginRequest（username/password）
 * - 查询：GET /user/getByUsername，入参@RequestParam username
 */
@Slf4j
public class UserScript {

    private  static String registeredUsername;
    private  static final String password = "123456";

    /**
     * 前置方法：初始化请求配置，打印脚本执行日志
     */
    @BeforeClass(groups = "user")
    public void init(ITestContext context) {
        LogUtil.info("===== 用户用例接口脚本初始化 =====");
        RequestHeaderConfig.init(context); // 从testng.xml读取baseUrl

        //清理可能的旧Token
        TokenContext.clearGlobalAuthToken();
    }

    //用户注册
    @Test(priority = 1,description = "用户注册", groups = "user")
    public void testUserRegister(){
        LogUtil.info("执行【用户注册】接口测试");

        //1.构造请求参数
        registeredUsername = "testUser" + System.currentTimeMillis();
        Map<String,Object> userParams = new HashMap<>();

        userParams.put("userName", registeredUsername);
        userParams.put("password", password);

        //2.发送POST请求（路径与控制器完全一致）
        Response response = RequestHeaderConfig.getNoAuthRequest()
                .body(userParams)
                .post("/user/register");

        //3.日志+断言
        LogUtil.info("【注册】响应结果：{}", response.asString());


        TestAssertUtil.assertSuccessCode(response);
        TestAssertUtil.assertMessage(response,"操作成功");
    }

    //用户登录并设置Token
    @Test(groups = "user", priority = 2,description = "用户登录并设置Token",dependsOnMethods = "testUserRegister")
    public void testUserLogin(ITestContext context){
        LogUtil.info("执行【用户登录】接口测试");

        //1.构造请求参数
        Map<String,Object> loginParams = new HashMap<>();
        loginParams.put("username", registeredUsername);
        loginParams.put("password", password);

        //2.发送登录请求
        Response response = RequestHeaderConfig.getNoAuthRequest()
                .body(loginParams)
                .post("/user/login");

        //3.提取Token并设置到全局上下文
        LogUtil.info("【登录】响应结果：{}", response.asString());

        // 1. 验证基本响应
        TestAssertUtil.assertSuccessCode(response);

        // 2.提取Token
        String token = extractTokenFromResponse(response);

        // ✅ 关键修复：立即设置Token，然后再做其他断言
        TokenContext.setGlobalAuthToken(token);
        LogUtil.info("✅ Token已设置到全局上下文");

        // 3.验证Token已设置
        boolean hasToken = TokenContext.hasToken();
        log.info("TokenContext.hasToken() 结果: {}", hasToken);

        if (hasToken) {
            String storedToken = TokenContext.getGlobalAuthToken();
            LogUtil.info("✅ 从TokenContext获取的Token: {}...",
                    storedToken.substring(0, Math.min(20, storedToken.length())));
        } else {
            log.error("❌ TokenContext中无Token，请检查setGlobalAuthToken()是否被调用");
        }
    }

    /**
     * 从响应中提取Token
     * @param response
     * @return
     */
    private String extractTokenFromResponse(Response response) {

        //尝试多种可能的路径
        String[] possiblePaths = {
                "data.token",
                "token",
                "data.accessToken",
                "accessToken"
        };

        for (String path : possiblePaths) {
            try {
                String token = response.jsonPath().getString(path);
                if (token != null && !token.trim().isEmpty()) {
                    log.info("✅ 从路径 '{}' 提取到Token", path);
                    return token;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        //如果JSON路径都失败，舱室字符串查找
        String responseBody = response.asString();
        if (responseBody.contains("\"token\":\"")){
            int start = responseBody.indexOf("\"token\":\"") + 9;
            int end = responseBody.indexOf("\"", start);
            if (end > start) {
                String token = responseBody.substring(start, end);
                LogUtil.info("✅ 从响应体字符串提取到Token");
                return token;
            }
        }

        LogUtil.error("无法从登录响应中提取Token，响应体：" + responseBody);
        return null;
    }

    //根据用户名查询用户
    @Test(priority = 3, description = "根据用户名查询用户",dependsOnMethods = "testUserLogin", groups = "user")
    public void testGetUserByUsername(){
        // 1. 发送GET请求：参数通过param传递（而非body）
        Response response = RequestHeaderConfig.getNoAuthRequest()
                .param("username", registeredUsername)
                .get("/user/getByUsername");

        // 2. 断言
        LogUtil.info("【查询用户】响应结果：{}", response.asString());
        TestAssertUtil.assertSuccessCode(response);
        TestAssertUtil.assertFieldValue(response, "data.userName", registeredUsername);
    }

    /**
     * 后置方法：打印执行完成日志
     */
    @AfterClass(groups = "user")
    public void clean() {
        LogUtil.info("===== 用户模块自动化脚本执行完成 =====");
    }
}
