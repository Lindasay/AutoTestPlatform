package com.auto.test.platform.common.util;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * 脚本失败重试核心类（最多重试一次）
 */
public class RetryAnalyzer implements IRetryAnalyzer {
    private static final int MAX_RETRY = 1;
    private int retryCount = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < MAX_RETRY) {
            LogUtil.info("用例[" + result.getMethod().getMethodName() + "]失败，开始第" + (retryCount + 1) + "次重试");
            retryCount++;
            try {
                Thread.sleep(1000); //重试间隔1秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return true;
        }
        LogUtil.error("用例[" + result.getMethod().getMethodName() + "]重试" + MAX_RETRY + "次仍失败",null);
        return false;
    }
}
