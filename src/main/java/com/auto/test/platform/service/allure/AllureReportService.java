package com.auto.test.platform.service.allure;

import com.auto.test.platform.engine.ApiExecuteEngine;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Allure报告实现类
 */
@Slf4j
@Service
public class AllureReportService {

    @Value("${allure.results.directory:./target/allure-results}")
    private String resultsDirectory;

    @Value("${allure.report.directory:./target/allure-report}")
    private String reportDirectory;

    @Value("${server.servlet.context-path:/auto-test}")
    private String contextPath;

    @Autowired
    private AllureLifecycle allureLifecycle;

    /**
     * 初始化Allure目录
     */
    public void initAllureDirectory() {
        try {
            Path resultPath = Paths.get(resultsDirectory);
            if (!Files.exists(resultPath)) {
                Files.createDirectories(resultPath);
                log.info("✅ 创建Allure结果目录: {}", resultPath.toAbsolutePath());
            } else {
                log.info("✅ Allure结果目录已存在: {}", resultPath.toAbsolutePath());
            }

            Path reportPath = Paths.get(reportDirectory);
            if (!Files.exists(reportPath)) {
                Files.createDirectories(reportPath);
                log.info("✅ 创建Allure报告目录: {}", reportPath.toAbsolutePath());
            } else {
                log.info("✅ Allure报告目录已存在: {}", reportPath.toAbsolutePath());
            }

        } catch (IOException e) {
            log.error("❌ 初始化Allure目录失败", e);
        }
    }

    /**
     * 生成单个用例的测试报告
     */
    public void generateSingleReport(ApiExecuteEngine.ExecuteResult executeResult) {
        try {
            String uuid = UUID.randomUUID().toString();

            TestResult testResult = new TestResult()
                    .setUuid(uuid)
                    .setName(executeResult.getCaseName())
                    .setFullName("用例ID：" + executeResult.getCaseId())
                    .setHistoryId(executeResult.getCaseId() != null ?
                            executeResult.getCaseId().toString() : uuid)
                    .setTestCaseId(executeResult.getCaseId() != null ?
                            executeResult.getCaseId().toString() : uuid)
                    .setStart(getTimestamp(executeResult.getExecuteTime()))
                    .setStop(getTimestamp(executeResult.getExecuteTime()) +
                            executeResult.getExecuteDuration());

            if (executeResult.getSuccess() != null && executeResult.getSuccess()) {
                testResult.setStatus(Status.PASSED);
                log.debug("✅ 用例 {} 执行成功，记录到Allure", executeResult.getCaseName());
            } else {
                testResult.setStatus(Status.FAILED);
                StatusDetails statusDetails = new StatusDetails()
                        .setMessage("执行失败")
                        .setTrace(executeResult.getErrorMsg());
                testResult.setStatusDetails(statusDetails);
                log.debug("❌ 用例 {} 执行失败，记录到Allure", executeResult.getCaseName());
            }

            testResult.setLabels(Arrays.asList(
                    new Label().setName("suite").setValue("接口自动化测试"),
                    new Label().setName("feature").setValue("接口测试"),
                    new Label().setName("story").setValue(executeResult.getCaseName())));

            // 写入Allure结果
            allureLifecycle.scheduleTestCase(testResult);
            allureLifecycle.startTestCase(uuid);
            allureLifecycle.stopTestCase(uuid);
            allureLifecycle.writeTestCase(uuid);

        } catch (Exception e) {
            log.error("❌ 生成Allure单个用例报告失败", e);
        }
    }

    /**
     * 生成批量执行用例的测试报告
     */
    public String generateBatchReport(ApiExecuteEngine.BatchExecuteResult batchResult) {
        log.info("🚀 开始生成Allure聚合报告，批次ID: {}", batchResult.getBatchId());
        log.info("📊 总用例数: {}，成功: {}，失败: {}",
                batchResult.getTotalCount(), batchResult.getPassCount(), batchResult.getFailCount());

        try {
            // 1. 检查目录是否存在
            File resultsDir = new File(resultsDirectory);
            File reportDir = new File(reportDirectory);

            if (!resultsDir.exists() && !resultsDir.mkdirs()) {
                log.error("❌ 无法创建Allure结果目录: {}", resultsDirectory);
                return ""; // 返回空字符串而不是null
            }

            if (!reportDir.exists() && !reportDir.mkdirs()) {
                log.error("❌ 无法创建Allure报告目录: {}", reportDirectory);
                return "";
            }

            // 2. 清理历史结果
            cleanResultsDirectory();

            // 3. 为每个用例生成结果
            int successCount = 0;
            for (ApiExecuteEngine.ExecuteResult result : batchResult.getCaseResults()) {
                try {
                    generateSingleReport(result);
                    successCount++;
                } catch (Exception e) {
                    log.warn("⚠️ 生成单个用例报告失败: {}", result.getCaseName(), e);
                }
            }

            log.info("📝 成功生成了 {} 个用例的Allure结果", successCount);

            // 4. 检查是否有结果文件
            File[] resultFiles = resultsDir.listFiles((dir, name) ->
                    name.endsWith(".json") || name.endsWith(".xml"));

            if (resultFiles == null || resultFiles.length == 0) {
                log.warn("⚠️ 没有找到Allure结果文件，可能用例执行未记录结果");
                return ""; // 返回空字符串而不是null
            }

            log.info("📁 发现 {} 个Allure结果文件", resultFiles.length);

            // 5. 生成HTML报告
            String reportUrl = generateHtmlReport();

            if (reportUrl != null && !reportUrl.trim().isEmpty()) {
                log.info("✅ Allure报告生成成功: {}", reportUrl);
                return reportUrl;
            } else {
                log.warn("⚠️ Allure报告生成失败，返回空URL");
                return "";
            }

        } catch (Exception e) {
            log.error("❌ 生成Allure聚合报告失败", e);
            return ""; // 返回空字符串而不是null
        }
    }

    /**
     * 生成HTML格式的测试执行报告
     */
    // 修复后的 generateHtmlReport 方法
    public String generateHtmlReport() {
        log.info("🔄 开始生成Allure HTML报告...");
        log.info("📁 结果目录: {}", resultsDirectory);
        log.info("📁 报告目录: {}", reportDirectory);

        try {
            // 1. 检查allure命令是否存在
            ProcessBuilder checkCmd = new ProcessBuilder("allure", "--version");
            Process checkProcess = checkCmd.start();

            // 修复：waitFor 返回 boolean，我们需要用 exitValue() 获取退出码
            boolean processCompleted = checkProcess.waitFor(5, TimeUnit.SECONDS);

            if (!processCompleted) {
                // 进程未在5秒内完成
                checkProcess.destroy();
                log.error("❌ Allure命令检查超时，请确保已安装Allure CLI");
                return "";
            }

            int exitCode = checkProcess.exitValue();
            if (exitCode != 0) {
                log.error("❌ Allure命令检查失败，exitCode: {}", exitCode);
                log.error("📋 请确保已正确安装Allure CLI");
                return "";
            }

            log.info("✅ Allure CLI检查通过");

            // 2. 执行allure generate命令
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "allure", "generate",
                    resultsDirectory,
                    "-o", reportDirectory,
                    "--clean");

            // 记录执行的命令
            String command = String.join(" ", processBuilder.command());
            log.info("💻 执行命令: {}", command);

            Process process = processBuilder.start();

            // 修复：正确使用 waitFor
            boolean success = process.waitFor(60, TimeUnit.SECONDS);
            int exitCodeGenerate = process.exitValue();

            if (success && exitCodeGenerate == 0) {
                log.info("✅ Allure HTML报告生成成功");

                // 检查报告文件是否存在
                File indexHtml = new File(reportDirectory, "index.html");
                if (indexHtml.exists()) {
                    String reportUrl = getReportUrl();
                    log.info("📄 报告文件存在: {}", indexHtml.getAbsolutePath());
                    log.info("🔗 报告URL: {}", reportUrl);
                    return reportUrl;
                } else {
                    log.error("❌ 报告文件不存在: {}", indexHtml.getAbsolutePath());
                    return "";
                }
            } else {
                if (!success) {
                    log.error("❌ Allure报告生成超时（60秒）");
                } else {
                    log.error("❌ Allure报告生成失败，exitCode: {}", exitCodeGenerate);
                }

                // 读取错误输出
                String error = new String(process.getErrorStream().readAllBytes());
                if (!error.trim().isEmpty()) {
                    log.error("❌ 错误输出: {}", error);
                }
                return "";
            }

        } catch (Exception e) {
            log.error("❌ 执行Allure命令失败", e);
            return "";
        }
    }

    /**
     * 清理历史报告
     */
    public void cleanResultsDirectory() {
        try {
            File resultsDir = new File(resultsDirectory);
            if (resultsDir.exists() && resultsDir.isDirectory()) {
                FileUtils.cleanDirectory(resultsDir);
                log.info("🧹 清理Allure结果目录: {}", resultsDirectory);
            } else {
                log.info("📁 Allure结果目录不存在，无需清理: {}", resultsDirectory);
            }
        } catch (IOException e) {
            log.warn("⚠️ 清理Allure结果目录失败: {}", e.getMessage());
        }
    }

    /**
     * 获取报告URL（相对于上下文路径）
     */
    public String getReportUrl() {
        // 返回相对于上下文路径的URL
        // 例如: /auto-test/allure/index.html
        String url = contextPath;
        if (url == null || url.trim().isEmpty() || "/".equals(url)) {
            url = "";
        }

        // 确保url以/开头，但结尾没有/
        if (!url.startsWith("/")) {
            url = "/" + url;
        }
        if (url.endsWith("/") && url.length() > 1) {
            url = url.substring(0, url.length() - 1);
        }

        String reportUrl = url + "/allure/index.html";
        log.debug("🔗 生成的报告URL: {}", reportUrl);
        return reportUrl;
    }

    /**
     * 获取时间戳
     */
    private long getTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return System.currentTimeMillis();
        }
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public String getResultsDirectory() {
        return resultsDirectory;
    }

    public String getReportDirectory() {
        return reportDirectory;
    }

    /**
     * 检查Allure报告是否可用
     */
    public boolean isReportAvailable() {
        try {
            File indexHtml = new File(reportDirectory, "index.html");
            boolean exists = indexHtml.exists();
            log.debug("📄 检查Allure报告: {} -> {}", indexHtml.getAbsolutePath(), exists ? "存在" : "不存在");
            return exists;
        } catch (Exception e) {
            return false;
        }
    }
}