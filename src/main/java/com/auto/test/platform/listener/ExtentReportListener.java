package com.auto.test.platform.listener;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.testng.ITestListener;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 可视化报告监听器
 */
@Slf4j
@Getter //提供getter方法，方便外部使用
public class ExtentReportListener implements ITestListener {
    private ExtentReports extent; //全局报告对象
    private ExtentTest currentTest; //当前用例的报告节点
    private String reportPath; //报告存储路径
    private String reportName; //报告文件名

    /**
     * 初始化报告
     */
    public void initReport(){
        //定义报告存储路径和文件名
        String basePath = System.getProperty("user.dir") + "/report/";
        this.reportName = "自动化测试聚合报告_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        this.reportPath = basePath + reportName;

        //配置HTML报告样式（保留原有逻辑）
        ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
        spark.config().setDocumentTitle("企业级自动化测试报告");
        spark.config().setReportName("接口自动化测试预览");
        spark.config().setTheme(Theme.DARK);
        spark.config().setEncoding("utf-8");

        // 初始化全局报告对象
        extent = new ExtentReports();
        extent.attachReporter(spark);
        extent.setSystemInfo("测试环境", "QA环境");
        extent.setSystemInfo("测试人员", "自动化测试工程师");
        extent.setSystemInfo("测试框架", "Spring Boot + RestAssured");

        log.info("报告初始化完成，路径：" + reportPath);
    }

    /**
     * 创建单个用例的报告节点
     * @param caseId 用例ID
     * @param caseName 用例名称
     * @param moduleName 模块名称
     */
    public void createTestNode(Long caseId, String caseName, String moduleName){
        String testKey = caseId + "_" + caseName + "_" + moduleName;
        currentTest = extent.createTest(testKey);
        currentTest.assignCategory(moduleName); //按模块分类
    }

    /**
     * 记录用例执行成功
     */
    public void logTestSuccess(){
        if (currentTest != null) {
            currentTest.log(Status.PASS,"用例执行通过✅");
        }
    }

    /**
     * 记录用例执行失败
     * @param errorMsg 失败原因
     */
    public void logTestFail(String errorMsg){
        if (currentTest != null) {
            currentTest.log(Status.FAIL,"用例执行失败❌：" + errorMsg);
        }
    }

    /**
     * 记录用例跳过（替代原onTestSkipped方法）
     */
    public void logTestSkipped(String reason) {
        if (currentTest != null) {
            currentTest.log(Status.SKIP, "用例执行跳过⚠️：" + reason);
        }
    }

    /**
     * 记录自定义信息（如请求参数、响应数据）
     * @param message 信息内容
     */
    public void logInfo(String message) {
        if (currentTest != null) {
            currentTest.log(Status.INFO, message);
        }
    }

    /**
     * 生成最终报告
     */
    public void generateReport() {
        if (extent != null) {
            extent.flush(); // 必须调用，生成最终测试报告
            log.info("自动化报告生成完成，可在report目录查看");
        }
    }

    /**
     * 辅助方法：根据用例名称匹配模块名称
     */
    public String getModuleName(String caseName) {
        if (caseName.contains("Project")) {
            return "项目管理模块";
        } else if (caseName.contains("TestCase")) {
            return "测试用例管理模块";
        } else {
            return "接口自动化模块";
        }
    }
}
