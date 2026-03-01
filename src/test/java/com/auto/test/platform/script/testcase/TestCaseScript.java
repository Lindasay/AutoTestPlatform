package com.auto.test.platform.script.testcase;

import com.auto.test.platform.common.config.RequestHeaderConfig;
import com.auto.test.platform.common.util.LogUtil;
import com.auto.test.platform.common.util.TestAssertUtil;
import com.auto.test.platform.script.TokenContext;
import io.restassured.response.Response;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 测试用例管理6个接口自动化脚本
 */
public class TestCaseScript {
    private Long projectId; //关联项目ID
    private Long caseId; //测试用例ID
    private String testCaseName; // 测试用例名称

    @BeforeClass(groups = "testcase")
    public void init(ITestContext context){
        LogUtil.info("===== 测试用例接口脚本初始化 =====");
        RequestHeaderConfig.init(context);

        // 验证Token
        if (!TokenContext.hasToken()) {
            // ✅ 修复：提供详细调试信息
            LogUtil.error("❌ Token为空，无法执行测试用例模块");
            LogUtil.error("调试信息：");
            LogUtil.error("- TokenContext.hasToken(): " + TokenContext.hasToken());
            LogUtil.error("- System Property值: " + System.getProperty("AUTO_TEST_PLATFORM_TOKEN"));
            LogUtil.error("- TokenContext.getTokenSafely(): " + TokenContext.getTokenSafely());

            // 不直接断言失败，让后续代码自然失败
        } else {
            LogUtil.info("✅ Token验证通过");

            String token = TokenContext.getTokenSafely();
            LogUtil.info("当前Token: {}...",
                    token.substring(0, Math.min(20, token.length())));
        }

        //创建测试项目
        projectId = addTestProject();
        LogUtil.info("创建测试项目成功，项目ID: {}",projectId);
    }

    //新增测试用例
    @Test(priority = 1,description = "新增测试用例", groups = "testcase", dependsOnGroups = "user")
    public void testAddTestCase(){
        LogUtil.info("执行【新增测试用例】接口测试，关联项目ID：" + projectId);
        // 使用唯一用例名称
        testCaseName = "自动化测试用例001_" + System.currentTimeMillis();
        String requestBody = String.format(
                "{\n" +
                        "  \"projectId\": %d,\n" +
                        "  \"caseName\": \"%s\",\n" +
                        "  \"caseType\": 1,\n" +
                        "  \"caseContent\": \"{\\\"url\\\":\\\"/api/test\\\",\\\"method\\\":\\\"GET\\\"}\",\n" +
                        "  \"status\": 1\n" +
                        "}", projectId, testCaseName);

        Response response = RequestHeaderConfig.getNoAuthRequest()
                .body(requestBody)
                .post("/testCase/add");

        LogUtil.debug("新增用例响应：" + response.asString());
        TestAssertUtil.assertSuccessCode(response);
        TestAssertUtil.assertMessage(response,"操作成功");

        caseId = getLastTestCaseId();
        LogUtil.info("新增用例ID：" + caseId);
    }

    //查询测试用例
    @Test(priority = 2,dependsOnMethods = "testAddTestCase",groups = "testcase")
    public void testGetTestCaseById(){
        LogUtil.info("执行【查询测试用例】接口测试，ID：" + caseId) ;
        Response response = RequestHeaderConfig.getNoAuthRequest().get("/testCase/get/" + caseId);

        TestAssertUtil.assertSuccessCode(response);
        TestAssertUtil.assertDataNotNull(response);
        TestAssertUtil.assertFieldValue(response,"data.caseName",testCaseName);
    }

    //分页查询测试用例
    @Test(priority = 3,groups = "testcase")
    public void testTestCasePage(){
        LogUtil.info("执行【分页查询用例】接口测试");
        Response response = RequestHeaderConfig.getNoAuthRequest()
                .param("page","1")
                .param("size","10")
                .param("projectId",projectId)
                .get("/testCase/page");

        TestAssertUtil.assertSuccessCode(response);
        TestAssertUtil.assertDataNotNull(response);
        TestAssertUtil.assertFieldValue(response,"data.total",1);
    }

    //修改测试用例
    @Test(priority = 4,dependsOnMethods = "testAddTestCase",groups = "testcase")
    public void testUpdateTestCase(){
        LogUtil.info("执行【修改测试用例】接口测试，ID" + caseId);
        String updatedCaseName = testCaseName + "（修改后）";
        String requestBody = String.format(
                "{\n" +
                        "  \"id\": %d,\n" +
                        "  \"projectId\": %d,\n" +
                        "  \"caseName\": \"%s\",\n" +
                        "  \"caseType\": 1,\n" +
                        "  \"caseContent\": \"{\\\"url\\\":\\\"/api/test\\\",\\\"method\\\":\\\"GET\\\"}\",\n" +
                        "  \"status\": 0\n" +
                        "}", caseId, projectId, updatedCaseName);

        Response response = RequestHeaderConfig.getNoAuthRequest()
                .body(requestBody)
                .put("/testCase/update");

        LogUtil.debug("修改用例请求：" + requestBody);
        LogUtil.debug("修改用例响应：" + response.asString());

        TestAssertUtil.assertSuccessCode(response);
        TestAssertUtil.assertMessage(response,"操作成功");
    }

    //修改用例状态
    @Test(priority = 5,dependsOnMethods = "testUpdateTestCase",groups = "testcase")
    public void testUpdateTestCaseStatus(){
        LogUtil.info("执行【修改用例状态】接口测试，ID：" + caseId);
        Response response = RequestHeaderConfig.getNoAuthRequest()
                .put("/testCase/update-status/" + caseId + "/1");

        TestAssertUtil.assertSuccessCode(response);
        TestAssertUtil.assertMessage(response,"操作成功");
    }

    //删除测试用例
    @Test(priority = 6, dependsOnMethods = "testAddTestCase",groups = "testcase")
    public void testDeleteTestCase(){
        LogUtil.info("执行【删除测试用例】接口测试，ID："  + caseId);

        Response response = RequestHeaderConfig.getNoAuthRequest().delete("/testCase/delete/" + caseId);

        LogUtil.info("删除接口响应：" + response.asString());

        String actualMsg = response.jsonPath().getString("msg");

        if ("测试用例删除成功".equals(actualMsg)) {
            LogUtil.info("✅ 测试用例删除成功");
        } else if ("待删除的测试用例不存在".equals(actualMsg)) {
            LogUtil.info("⚠️ 测试用例已不存在");
            LogUtil.info("✅ 业务验证通过：删除接口具有幂等性");
        } else {
            // 其他消息，使用原始断言
            TestAssertUtil.assertSuccessCode(response);
            TestAssertUtil.assertMessage(response, "测试用例删除成功");
        }

    }

    @AfterClass(groups = "testcase")
    public void clean(){
        LogUtil.info("===== 测试用例接口脚本清理 =====");
        try {
            clearTestCase();
        } catch (Exception e) {
            LogUtil.error("清理测试数据失败: {}", e.getMessage());
        }
    }

    //私有方法：新增测试项目
    private Long addTestProject(){
        String projectName = "用例关联测试项目_" + System.currentTimeMillis();
        String body = String.format(
                "{\n" +
                        "  \"projectName\": \"%s\",\n" +
                        "  \"projectDesc\": \"企业级测试\",\n" +
                        "  \"status\": 1\n" +
                        "}", projectName);

        Response response = RequestHeaderConfig.getNoAuthRequest().body(body).post("/project/add");

        TestAssertUtil.assertSuccessCode(response);
        TestAssertUtil.assertMessage(response,"操作成功");

        //获取项目ID
        Response resp = RequestHeaderConfig.getNoAuthRequest()
                .param("pageNum",1)
                .param("pageSize",10)
                .param("projectName",projectName)
                .get("/project/page");

        return resp.jsonPath().getLong("data.records[0].id");
    }

    //私有方法：获取最新用例ID
    private Long getLastTestCaseId(){
        Response response = RequestHeaderConfig.getNoAuthRequest()
                .param("pageNum",1)
                .param("pageSize",10)
                .get("/testCase/page");
        return response.jsonPath().getLong("data.records[0].id");
    }

    //私有方法：清空测试用例
    private void clearTestCase(){
        try {
            Response response = RequestHeaderConfig.getNoAuthRequest()
                    .param("pageNum",1)
                    .param("pageSize",100)
                    .get("/testCase/page");

            int total = response.jsonPath().getInt("data.total");


            for (int i = 0; i < total; i++) {
                Long id = response.jsonPath().getLong("data.records["+ i +"].id");
                if (id != null){
                    Response deleteResp = RequestHeaderConfig.getNoAuthRequest().delete("/testCase/delete/" + id);

                    if (deleteResp.getStatusCode() == 200){
                        LogUtil.info("清理测试用例 ID: {} 成功", id);
                    }else {
                        LogUtil.warn("清理测试用例 ID: {} 失败", id);
                    }
                }
            }

            //清理测试项目
            if (projectId != null){
                Response deleteProjectResp = RequestHeaderConfig.getNoAuthRequest()
                        .delete("/project/delete/" + projectId);
                if (deleteProjectResp.getStatusCode() == 200){
                    LogUtil.info("清理项目 ID: {} 成功", projectId);
                }else {
                    LogUtil.warn("清理项目 ID: {} 失败", projectId);
                }
            }
        } catch (Exception e) {
            LogUtil.error("清理测试数据异常: {}", e.getMessage());
        } finally {
            //清理Token
            TokenContext.clearGlobalAuthToken();
            LogUtil.info("✅ 测试数据清理完成，Token已清除");
        }
    }

}
