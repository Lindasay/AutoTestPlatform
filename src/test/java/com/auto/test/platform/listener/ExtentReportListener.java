package com.auto.test.platform.listener;

import com.auto.test.platform.common.util.LogUtil;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 可视化报告监听器
 */
public class ExtentReportListener implements ITestListener {
    private ExtentReports extent; //全局报告对象
    private ExtentTest test; //单个用例测试对象

    //初始化报告（所有用例执行前执行）
    @Override
    public void onStart(ITestContext context) {
        //定义报告存储路径和文件名，报告路径：工程目录/report
        String reportPath = System.getProperty("user.dir") + "/report/";
        String reportName = "自动化测试聚合报告_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".html";

        //配置HTML报告样式
        ExtentSparkReporter spark = new ExtentSparkReporter(reportPath + reportName);
        spark.config().setDocumentTitle("企业级自动化测试报告");
        spark.config().setReportName("接口自动化测试总览");
        spark.config().setTheme(Theme.STANDARD); //报告主题
        spark.config().setEncoding("UTF-8"); //解决中文乱码

        //初始化全局对象
        extent = new ExtentReports();
        extent.attachReporter(spark);
        extent.setSystemInfo("测试环境", "QA环境");
        extent.setSystemInfo("测试人员", "自动化测试工程师");
        extent.setSystemInfo("测试框架","TestNG + RestAssured");

        LogUtil.info("报告初始化完成，路径：" + reportPath + reportName);
    }

    //单个用例开始执行
    @Override
    public void onTestStart(ITestResult testResult) {
        //创建用例节点：按【类名.方法名】命名，分类为测试模块（如项目管理/测试用例管理）
        String className = testResult.getTestClass().getRealClass().getSimpleName();
        String methodName = testResult.getName();
        test = extent.createTest(className + "." + methodName);
        test.assignCategory(getModuleName(className)); //按类名归类模块
    }

    //单个用例执行成功
    @Override
    public void onTestSuccess(ITestResult testResult) {
        test.log(Status.PASS,"用例执行通过✅" + testResult.getName());
    }

    //单个用例执行失败时
    @Override
    public void onTestFailure(ITestResult testResult) {
        test.log(Status.FAIL,"用例执行失败❌：" + testResult.getName());
        test.log(Status.FAIL,"失败原因：" + testResult.getThrowable());
    }

    //单个用例跳过执行时
    @Override
    public void onTestSkipped(ITestResult testResult) {
        test.log(Status.SKIP,"用例执行跳过⚠️：" + testResult.getName());
    }

    //所有用例执行完成后（生成最终报告）
    @Override
    public void onFinish(ITestContext testContext) {
        extent.flush(); //必须调用，生成最终测试报告
        LogUtil.info("自动化报告生成完成，可在report目录查看");
    }

    //辅助方法：根据测试类名匹配模块名称
    private String getModuleName(String className) {
        if (className.contains("Project")){
            return "项目管理模块";
        }else  if (className.contains("TestCase")){
            return "测试用例管理模块";
        }else {
            return "其他模块";
        }
    }

    // 忽略重写（无需修改）
    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {}
}
