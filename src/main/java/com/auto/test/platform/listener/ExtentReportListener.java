package com.auto.test.platform.listener;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.testng.ITestListener;

import java.io.File;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 可视化报告监听器
 */
@Slf4j
@Getter //提供getter方法，方便外部使用
@Component
public class ExtentReportListener implements ITestListener {
    private ExtentReports extent; //全局报告对象
    private String reportPath; //报告存储路径
    private String reportName; //报告文件名

    //修复线程安全问题
    private static final ThreadLocal<ExtentTest> currentTest = new ThreadLocal<>();
    private static final Map<String, ExtentTest> testNodeMap = new ConcurrentHashMap<>();
    private volatile boolean isInitialized = false;

    @Value("${extent.report.directory:./report}")
    private String reportDirectory;

    @Value("${extent.report.theme:dark}")
    private String theme;

    /**
     * 初始化报告
     */
    public synchronized void initReport(){

        if (isInitialized) {
            log.info("ExtentReports已初始化，跳过重复初始化");
            return;
        }

        try {
            //确保报告目录存在
            File reportDir = new File(reportDirectory);
            if (!reportDir.exists()) {
                boolean created = reportDir.mkdirs();
                if (created){
                    log.info("创建报告目录：{}", reportDirectory);
                }
            }

            String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            this.reportName = "自动化测试聚合报告_" +  timestamp + ".html";
            this.reportPath = Paths.get(reportDirectory,reportName).toString();

            //配置HTML报告
            ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
            spark.config().setDocumentTitle("企业级自动化测试报告");
            spark.config().setReportName("接口自动化测试预览");
            spark.config().setTheme(Theme.DARK.name().equalsIgnoreCase(theme) ? Theme.DARK : Theme.STANDARD);
            spark.config().setEncoding("utf-8");
            spark.config().setTimeStampFormat("yyyy-MM-dd HH:mm:ss");

            // 初始化全局报告对象
            extent = new ExtentReports();
            extent.attachReporter(spark);

            //设置系统信息
            extent.setSystemInfo("测试环境", "自动化测试环境");
            extent.setSystemInfo("测试人员", "自动化测试工程师");
            extent.setSystemInfo("测试框架", "Spring Boot + RestAssured");
            extent.setSystemInfo("操作系统", System.getProperty("os.name"));
            extent.setSystemInfo("Java版本", System.getProperty("java.version"));
            extent.setSystemInfo("执行时间", timestamp);

            isInitialized = true;
            log.info("ExtentReports报告初始化完成，路径：{}", reportPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 创建单个用例的报告节点
     * @param caseId 用例ID
     * @param caseName 用例名称
     * @param moduleName 模块名称
     */
    public void createTestNode(Long caseId, String caseName, String moduleName){
        try {
            if (extent == null) {
                initReport();
            }

            // 生成唯一键
            String testKey = String.format("%d_%s_%s_%d",
                    caseId, caseName,moduleName,System.currentTimeMillis());

            // 创建测试节点
            ExtentTest testNode = extent.createTest(caseName).assignCategory(moduleName).assignAuthor("AutoTestPlatform");

            //设置描述信息
            if (caseId != null) {
                testNode.info("用例ID：" + caseId);
            }
            testNode.info("模块：" + moduleName);
            testNode.info("开始时间：" + new  SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

            //保存节点
            testNodeMap.put(testKey, testNode);
            currentTest.set(testNode);

        } catch (Exception e) {
            log.error("创建测试节点失败 - 用例ID：{}, 名称：{}", caseId, caseName,e);
        }
    }

    /**
     * 记录用例执行成功
     */
    public void logTestSuccess(){
        ExtentTest test = currentTest.get();
        if (test != null) {
            test.pass("用例执行通过 ✅");
            test.info("结束时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
        }
    }

    /**
     * 记录用例执行失败
     * @param errorMsg 失败原因
     */
    public void logTestFail(String errorMsg){
        ExtentTest test = currentTest.get();
        if (test != null) {
            test.fail("用例执行失败 ❌: " + (errorMsg != null ? errorMsg : "未知错误"));
            test.info("结束时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
        }
    }

    /**
     * 记录用例跳过（替代原onTestSkipped方法）
     */
    public void logTestSkipped(String reason) {
        ExtentTest test = currentTest.get();
        if (test != null) {
            test.skip("用例执行跳过 ⚠️: " + (reason != null ? reason : "未知原因"));
            test.info("结束时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
        }
    }

    /**
     * 记录自定义信息（如请求参数、响应数据）
     * @param message 信息内容
     */
    public void logInfo(String message) {
        ExtentTest test = currentTest.get();
        if (test != null && message != null) {
            test.info(message);
        }
    }

    /**
     * 生成最终报告
     */
    public synchronized void generateReport() {
        try {
            if (extent != null) {
                extent.flush();
                log.info("ExtentReports报告生成完成，路径：{}", reportPath);

                //清理ThreadLocal
                currentTest.remove();
            }else {
                log.warn("ExtentReports未初始化，无法生成报告");
            }
        } catch (Exception e) {
            log.error("生成ExtentReports报告失败", e);
        }
    }

    /**
     * 清理资源
     */
    public synchronized void cleanup() {
        try {
            currentTest.remove();
            testNodeMap.clear();
            isInitialized = false;
            log.info("ExtentReportListener资源清理完成");
        } catch (Exception e) {
            log.error("清理ExtentReportListener资源失败", e);
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
        } else if (caseName.toLowerCase().contains("user")) {
            return "用户管理模块";
        }
        else {
            return "接口自动化模块";
        }
    }
}