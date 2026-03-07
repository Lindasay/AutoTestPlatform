package com.auto.test.platform.engine;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.auto.test.platform.entity.CaseContent;
import com.auto.test.platform.entity.TestCase;
import com.auto.test.platform.listener.ExtentReportListener;
import com.auto.test.platform.service.allure.AllureReportService;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 接口自动化执行引擎（集成Allure + ExtentReports）
 */
@Slf4j
@Component
public class ApiExecuteEngine {

    @Autowired
    private AllureReportService allureReportService;

    @Autowired
    private ExtentReportListener extentReportListener;

    // 使用ThreadLocal存储批次ID
    private static final ThreadLocal<String> batchId = new ThreadLocal<>();

    // 使用ThreadLocal存储是否已初始化ExtentReports
    private static final ThreadLocal<Boolean> extentReportInited = ThreadLocal.withInitial(() -> false);

    @Step("执行单个接口测试用例：{testCase.getCaseName}")
    public ExecuteResult execute(TestCase testCase) {
        ExecuteResult result = new ExecuteResult();
        result.setCaseId(testCase.getId());
        result.setProjectId(testCase.getProjectId());
        result.setCaseName(testCase.getCaseName());
        result.setCaseType(testCase.getCaseType());
        result.setExecuteTime(LocalDateTime.now());

        // 仅处理接口用例（case_type=1）
        if (!testCase.getCaseType().equals(1)) {
            result.setSuccess(false);
            result.setErrorMsg("非接口用例（case_type=1）, 无需执行");
            log.warn("用例ID：{} 非接口用例，执行终止", testCase.getId());
            // 记录到ExtentReports
            recordExtentReport(testCase, result, null, null);
            return result;
        }

        try {
            // 1. 解析并校验caseContent
            CaseContent caseContent = null;
            try {
                caseContent = JSON.parseObject(testCase.getCaseContent(), CaseContent.class);
                if (caseContent.getRequestUrl() == null || caseContent.getRequestUrl().isEmpty()) {
                    result.setSuccess(false);
                    result.setErrorMsg("caseContent缺少必填字段：requestUrl");
                    recordExtentReport(testCase, result, null, null);
                    return result;
                }
                String[] validMethods = {"GET", "POST", "PUT", "DELETE", "PATCH"};
                if (caseContent.getRequestMethod() == null && !Arrays.asList(validMethods).contains(caseContent.getRequestMethod().toUpperCase())) {
                    result.setSuccess(false);
                    result.setErrorMsg("requestMethod仅支持GET/POST/PUT/DELETE/PATCH");
                    recordExtentReport(testCase, result, null, null);
                    return result;
                }
                if (caseContent.getRequestMethod() == null || caseContent.getRequestMethod().isEmpty()) {
                    caseContent.setRequestMethod("GET");
                }
                if (caseContent.getTimeout() == null || caseContent.getTimeout() == 0) {
                    caseContent.setTimeout(3000);
                }
            } catch (JSONException e) {
                result.setSuccess(false);
                result.setErrorMsg("caseContent不是合法的JSON格式：" + e.getMessage());
                recordExtentReport(testCase, result, null, null);
                return result;
            } catch (Exception e) {
                result.setSuccess(false);
                result.setErrorMsg("caseContent解析失败：" + e.getMessage());
                recordExtentReport(testCase, result, null, null);
                return result;
            }
            // 提取接口请求
            String requestParams = caseContent.getRequestParams();
            String expectedResult = caseContent.getExpectedResult();
            String requestHeaders = caseContent.getRequestHeaders();

            // 3. 添加Allure附件
            Allure.step("准备执行接口测试：" + testCase.getCaseName());
            if (requestParams != null && !requestParams.isEmpty()) {
                Allure.addAttachment("请求参数", "application/json", requestParams);
            }
            if (requestHeaders != null && !requestHeaders.isEmpty()) {
                Allure.addAttachment("请求头", "application/json", requestHeaders);
            }

            // 4. 记录执行开始时间
            long start = System.currentTimeMillis();

            // 5. 执行接口请求
            Response response = executeRequest(caseContent);

            // 6. 计算执行时长
            long duration = System.currentTimeMillis() - start;

            // 7. 解析响应并断言
            String responseData = response.asString();
            boolean isSuccess = validateResponse(responseData, expectedResult);

            // 8. 封装结果
            result.setSuccess(isSuccess);
            result.setResponseData(responseData);
            result.setExecuteDuration((int) duration);

            if (!isSuccess && expectedResult != null) {
                result.setErrorMsg("响应不包含期望结果：" + expectedResult);
            }

            // Allure记录响应
            if (responseData != null) {
                Allure.addAttachment("响应数据", responseData);
            }
            Allure.step("接口测试完成：" + (isSuccess ? "成功" : "失败"));

            // 9. 记录到ExtentReports
            recordExtentReport(testCase, result, responseData, caseContent);

            // 10. 记录到Allure
            allureReportService.generateSingleReport(result);

            log.info("接口用例执行成功，用例ID: {}， 执行结果：{}, 耗时：{}ms", testCase.getId(), isSuccess, duration);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMsg(e.getMessage());
            result.setExecuteDuration(0);
            // Allure记录异常
            Allure.addAttachment("执行异常", e.getMessage());
            Allure.step("执行异常：" + e.getMessage());

            // ExtentReports记录异常
            recordExtentReport(testCase, result, null, null);

            // Allure记录异常用例
            allureReportService.generateSingleReport(result);

            log.error("接口用例执行失败，用例ID：{}", testCase.getId(), e);
        }
        return result;
    }

    // ========== 修复后的批量执行方法 ==========
    @Step("批量执行接口用例，共{testCases.size()}条")
    public BatchExecuteResult executeBatch(List<TestCase> testCases) {
        // 生成批次ID
        String currentBatchId = UUID.randomUUID().toString();
        batchId.set(currentBatchId);

        BatchExecuteResult batchResult = new BatchExecuteResult();
        batchResult.setBatchId(currentBatchId);
        batchResult.setExecuteTime(LocalDateTime.now());
        batchResult.setTotalCount(testCases.size());
        batchResult.setCaseResults(new ArrayList<>());

        // 初始化Allure报告目录
        allureReportService.initAllureDirectory();

        // 初始化ExtentReports聚合报告
        if (!extentReportInited.get()) {
            extentReportListener.initReport();
            extentReportInited.set(true);
        }

        // 遍历执行所有用例
        int passCount = 0;
        int failCount = 0;
        int totalDuration = 0;
        for (TestCase testCase : testCases) {
            ExecuteResult singleResult = execute(testCase);
            batchResult.getCaseResults().add(singleResult);

            if (singleResult.getSuccess()) {
                passCount++;
            } else {
                failCount++;
            }

            if (singleResult.getExecuteDuration() != null) {
                totalDuration += singleResult.getExecuteDuration();
            }
        }

        // 封装聚合统计数据
        batchResult.setPassCount(passCount);
        batchResult.setFailCount(failCount);
        batchResult.setTotalDuration(totalDuration);

        // 计算平均执行时长
        if (testCases.isEmpty()) {
            batchResult.setAverageDuration(0);
        } else {
            batchResult.setAverageDuration(totalDuration / testCases.size());
        }

        // 计算通过率
        if (testCases.isEmpty()) {
            batchResult.setPassRate(BigDecimal.ZERO);
        } else {
            BigDecimal passRate = BigDecimal.valueOf((double) passCount / testCases.size() * 100)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
            batchResult.setPassRate(passRate);
        }

        // 修复：正确设置ExtentReports报告路径
        String extentReportPath = null;
        if (extentReportListener.getReportName() != null) {
            extentReportPath = "/report/" + extentReportListener.getReportName();
            batchResult.setExtentReportPath(extentReportPath);
            log.info("ExtentReports报告生成成功: {}", extentReportPath);
        } else {
            log.warn("ExtentReports报告名称为空，无法设置报告路径");
        }

        // 修复：正确设置Allure报告URL
        String allureReportUrl = allureReportService.generateBatchReport(batchResult);
        if (allureReportUrl != null && !allureReportUrl.isEmpty()) {
            batchResult.setAllureReportUrl(allureReportUrl);
            log.info("Allure报告生成成功: {}", allureReportUrl);
        } else {
            log.warn("Allure报告URL为空，无法设置报告URL");
        }

        log.info("批量执行完成：总用例{}条，成功{}条，失败{}条，通过率{}%",
                testCases.size(), passCount, failCount, batchResult.getPassRate());

        return batchResult;
    }

    // ========== 通用方法：执行接口请求 ==========
    private Response executeRequest(CaseContent caseContent) {
        Integer timeout = caseContent.getTimeout();

        RestAssuredConfig config = RestAssuredConfig.newConfig()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", timeout)
                        .setParam("http.socket.timeout", timeout));

        if ("GET".equalsIgnoreCase(caseContent.getRequestMethod())) {
            return RestAssured.given()
                    .config(config)
                    .contentType(ContentType.JSON)
                    .headers(caseContent.getRequestHeaders() == null ? new JSONObject() : JSON.parseObject(caseContent.getRequestHeaders()))
                    .params(caseContent.getRequestParams() == null ? new JSONObject() : JSON.parseObject(caseContent.getRequestParams()))
                    .when()
                    .get(caseContent.getRequestUrl())
                    .then()
                    .extract()
                    .response();
        } else {
            return RestAssured.given()
                    .config(config)
                    .contentType(ContentType.JSON)
                    .headers(caseContent.getRequestHeaders() == null ? new JSONObject() : JSON.parseObject(caseContent.getRequestHeaders()))
                    .body(caseContent.getRequestParams() == null ? "{}" : caseContent.getRequestParams())
                    .when()
                    .request(caseContent.getRequestMethod(), caseContent.getRequestUrl())
                    .then()
                    .extract()
                    .response();
        }
    }

    private boolean validateResponse(String responseData, String expectedResult) {
        if (expectedResult == null || expectedResult.isEmpty()) {
            return true;
        }

        try {
            if (responseData == null) {
                return false;
            }
            if (responseData.trim().startsWith("{") || responseData.trim().startsWith("[")) {
                return responseData.contains(expectedResult);
            } else {
                return responseData.equals(expectedResult);
            }
        } catch (Exception e) {
            return responseData != null && responseData.contains(expectedResult);
        }
    }

    /**
     * 记录用例执行结果到ExtentReports
     */
    private void recordExtentReport(TestCase testCase, ExecuteResult result, String responseData, CaseContent caseContent) {
        try {
            // 创建用例报告节点
            String moduleName = extentReportListener.getModuleName(testCase.getCaseName());
            extentReportListener.createTestNode(testCase.getId(), testCase.getCaseName(), moduleName);

            // 记录执行结果
            if (result.getSuccess()) {
                extentReportListener.logTestSuccess();
            } else {
                extentReportListener.logTestFail(result.getErrorMsg());
            }

            // 记录请求/响应详情
            if (caseContent != null) {
                extentReportListener.logInfo("请求URL：" + caseContent.getRequestUrl());
                extentReportListener.logInfo("请求方法：" + caseContent.getRequestMethod());
                extentReportListener.logInfo("请求参数：" + caseContent.getRequestParams());
                if (caseContent.getRequestHeaders() != null && !caseContent.getRequestHeaders().isEmpty()) {
                    extentReportListener.logInfo("请求头：" + caseContent.getRequestHeaders());
                }
            } else {
                extentReportListener.logInfo("请求信息：caseContent解析失败，无法展示");
            }

            if (responseData != null) {
                extentReportListener.logInfo("响应数据：" + responseData);
            }

            extentReportListener.logInfo("执行时长：" + result.getExecuteDuration() + "ms");
            extentReportListener.logInfo("执行时间：" + result.getExecuteTime());

            // 生成最终报告
            extentReportListener.generateReport();

            // 记录报告路径到执行结果
            if (extentReportListener.getReportName() != null) {
                result.setExtentReportPath("/report/" + extentReportListener.getReportName());
            }

        } catch (Exception e) {
            log.error("生成ExtentReports报告失败", e);
        }
    }

    // 执行结果临时包装类
    @Data
    public static class ExecuteResult {
        private Long caseId;
        private Long projectId;
        private String caseName;
        private Integer caseType;
        private Boolean success;
        private String responseData;
        private String errorMsg;
        private LocalDateTime executeTime;
        private Integer executeDuration;
        private String extentReportPath;
        private String allureReportUrl;
    }

    // 批量执行聚合结果
    @Data
    public static class BatchExecuteResult {
        private String batchId;
        private LocalDateTime executeTime;
        private Integer totalCount;
        private Integer passCount;
        private Integer failCount;
        private Integer totalDuration;
        private Integer averageDuration;
        private BigDecimal passRate;
        private String extentReportPath;  // ExtentReports报告路径
        private String allureReportUrl;   // Allure报告URL
        private List<ExecuteResult> caseResults;
    }
}