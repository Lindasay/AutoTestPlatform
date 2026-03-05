package com.auto.test.platform.engine;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.auto.test.platform.entity.CaseContent;
import com.auto.test.platform.entity.TestCase;
import com.auto.test.platform.listener.ExtentReportListener;
import com.aventstack.extentreports.ExtentReports;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 接口自动化执行引擎（集成Allure + ExtentReports）
 */
@Slf4j
@Component
public class ApiExecuteEngine {

    private final ExtentReportListener extentReportListener =  new ExtentReportListener();
    private boolean isReportInited = false; //标记报告是否初始化

   @Step("执行单个接口测试用例：{testCase.getCaseName}")
    public ExecuteResult execute(TestCase testCase) {
       ExecuteResult result = new ExecuteResult();
       result.setCaseId(testCase.getId());
       result.setProjectId(testCase.getProjectId());
       result.setCaseName(testCase.getCaseName());
       result.setCaseType(testCase.getCaseType());
       result.setExecuteTime(LocalDateTime.now());

       //仅处理接口用例（case_type=1）
       if (!testCase.getCaseType().equals(1)){
           result.setSuccess(false);
           result.setErrorMsg("非接口用例（case_type=1）, 无需执行");
           log.warn("用例ID：{} 非接口用例，执行终止",testCase.getId());
           //记录到ExtendReports
           recordExtentReport(testCase,result,null,null);
           return result;
       }

       try {
           // 1.解析并校验caseContent
           CaseContent caseContent = null;
           try {
               caseContent = JSON.parseObject(testCase.getCaseContent(), CaseContent.class);
               if (caseContent.getRequestUrl() == null || caseContent.getRequestUrl().isEmpty()) {
                   result.setSuccess(false);
                   result.setErrorMsg("caseContent缺少必填字段：requestUrl");
                   recordExtentReport(testCase,result,null,null);
                   return result;
               }
               String[] validMethods = {"GET", "POST", "PUT", "DELETE", "PATCH"};
               if (caseContent.getRequestMethod() == null && !Arrays.asList(validMethods).contains(caseContent.getRequestMethod().toUpperCase())) {
                   result.setSuccess(false);
                   result.setErrorMsg("requestMethod仅支持GET/POST/PUT/DELETE/PATCH");
                   recordExtentReport(testCase,result,null,null);
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
               recordExtentReport(testCase, result, null,null);
               return result;
           } catch (Exception e) {
               result.setSuccess(false);
               result.setErrorMsg("caseContent解析失败：" + e.getMessage());
               recordExtentReport(testCase,result,null,null);
               return result;
           }
            // 提取接口请求
           String requestParams = caseContent.getRequestParams();
           String expectedResult = caseContent.getExpectedResult();
           String requestHeaders = caseContent.getRequestHeaders();

           // 3.添加Allure附件
           Allure.addAttachment("请求参数", requestParams == null ? "" : requestParams);
           if (requestHeaders != null && !requestHeaders.isEmpty()) {
               Allure.addAttachment("请求头",requestHeaders);
           }

           // 4.记录执行开始时间
           long start = System.currentTimeMillis();

           // 5.执行接口请求
           Response response = executeRequest(caseContent);

           // 6.计算执行时长
           long duration = System.currentTimeMillis() - start;

           // 7.解析响应并断言
           String responseData = response.asString();
           boolean isSuccess = expectedResult == null || responseData.contains(expectedResult);

           // 8.封装结果
           result.setSuccess(isSuccess);
           result.setResponseData(responseData);
           result.setExecuteDuration((int) duration);

           //Allure记录响应
           Allure.addAttachment("响应数据", responseData);
           log.info("接口用例执行成功，用例ID: {}， 执行结果：{}, 耗时：{}ms", testCase.getId(), isSuccess, duration);

           // 9.记录到ExtendReports
           recordExtentReport(testCase, result, responseData, caseContent);

       } catch (Exception e) {
           result.setSuccess(false);
           result.setErrorMsg(e.getMessage());
           result.setExecuteDuration(0);
           //Allure记录异常
           Allure.addAttachment("执行异常", e.getMessage());
           recordExtentReport(testCase, result, null,null);
           log.error("接口用例执行失败，用例ID：{}", testCase.getId(), e);

       }
       return result;
   }

    // ========== 新增批量执行方法（核心：生成聚合结果） ==========
    @Step("批量执行接口用例，共{testCases.size()}条")
    public BatchExecuteResult executeBatch(List<TestCase> testCases) {
        BatchExecuteResult batchResult = new BatchExecuteResult();
        batchResult.setExecuteTime(LocalDateTime.now());
        batchResult.setTotalCount(testCases.size());
        batchResult.setCaseResults(new ArrayList<>());


        // 初始化聚合报告
        if (!isReportInited) {
            extentReportListener.initReport();
            isReportInited = true;
        }

        // 遍历执行所有用例
        int passCount = 0;
        int failCount = 0;
        int totalDuration = 0; //总时长
        for (TestCase testCase : testCases) {
            ExecuteResult singleResult = execute(testCase); // 复用单条执行逻辑
            batchResult.getCaseResults().add(singleResult);

            // 统计成功/失败数
            if (singleResult.getSuccess()) {
                passCount++;
            } else {
                failCount++;
            }
            // 统计总时长
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

        // 计算通过率（保留2位小数）
        if (testCases.isEmpty()) {
            batchResult.setPassRate(BigDecimal.ZERO);
        } else {
            BigDecimal passRate = BigDecimal.valueOf((double) passCount / testCases.size() * 100)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
            batchResult.setPassRate(passRate);
        }
        // 聚合报告路径（所有用例共用一份ExtentReports报告）
        String reportName = extentReportListener.getReportName();
        if (reportName != null) {
            batchResult.setExtentReportPath("/report/" + reportName);
        }
        // 生成最终聚合报告
        extentReportListener.generateReport();
        log.info("批量执行完成：总用例{}条，成功{}条，失败{}条，通过率{}",
                testCases.size(), passCount, failCount, batchResult.getPassRate());

        return batchResult;
    }

    // ========== 通用方法：执行接口请求 ==========
    private Response executeRequest(CaseContent caseContent) {
        // 正确配置RestAssured超时
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

    /**
     * 复用ExtentReportListener,记录用例执行结果到ExtentReports
     * @param testCase
     * @param result
     * @param responseData
     * @param caseContent
     */
    private void recordExtentReport(TestCase testCase, ExecuteResult result, String responseData, CaseContent  caseContent) {

        try {
            // 1.初始化ExtentReports(首次执行时)
            if (!isReportInited) {
                extentReportListener.initReport();
                isReportInited = true;
            }

            // 2.创建用例报告点
            String moduleName = extentReportListener.getModuleName(testCase.getCaseName());
            extentReportListener.createTestNode(testCase.getId(),testCase.getCaseName(),moduleName);

            // 3.记录执行结果
            if (result.getSuccess()){
                extentReportListener.logTestSuccess();
            }else {
                extentReportListener.logTestFail(result.getErrorMsg());
            }

            // 4.记录请求/响应详情
            if (caseContent != null) {
                extentReportListener.logInfo("请求URL：" +  caseContent.getRequestUrl());
                extentReportListener.logInfo("请求方法：" + caseContent.getRequestMethod());
                extentReportListener.logInfo("请求参数：" + caseContent.getRequestParams());
            }else {
                extentReportListener.logInfo("请求信息：caseContent解析失败，无法展示");
            }
            if (responseData != null) {
                extentReportListener.logInfo("响应数据：" + responseData);
            }
            extentReportListener.logInfo("执行时长：" + result.getExecuteDuration() + "ms");

            // 5.生成最终报告
            extentReportListener.generateReport();

            // 6.记录报告路径到执行结果
            if (extentReportListener.getReportName() != null){
                result.setExtentReportPath("/report/" + extentReportListener.getReportName());
            }

        } catch (Exception e) {
            log.error("生成ExtendReports报告失败",e);
        }
    }


    //执行结果临时包装类（包含ExtendReports路径）
    @Data
    public static class ExecuteResult {

       private Long caseId; //用例ID
       private Long projectId; //项目ID
       private String caseName; //用例名称
       private Integer caseType; //用例类型
       private Boolean success; // 执行结果（true-成功 false-失败）
       private String responseData; // 响应数据
       private String errorMsg; // 失败原因
       private LocalDateTime executeTime; // 执行时间
       private Integer executeDuration; // 执行时长（毫秒）
       private String extentReportPath; // ExtentReports报告访问路径
    }

    // ========== 内部类：批量执行聚合结果 ==========
    @Data
    public static class BatchExecuteResult {
        private LocalDateTime executeTime;      // 批量执行时间
        private Integer totalCount;             // 总用例数
        private Integer passCount;              // 成功数
        private Integer failCount;              // 失败数
        private Integer totalDuration;          // 总执行时长
        private Integer averageDuration;        // 平均执行时长
        private BigDecimal passRate;                // 通过率（如90.00%）
        private String extentReportPath;        // 聚合报告路径
        private List<ExecuteResult> caseResults;// 所有用例的执行详情
    }
}
