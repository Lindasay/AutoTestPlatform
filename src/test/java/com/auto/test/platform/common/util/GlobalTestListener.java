package com.auto.test.platform.common.util;

import com.auto.test.platform.script.TokenContext;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;

/**
 * 全局测试套件监听器
 */
public class GlobalTestListener implements ISuiteListener {

    @Override
    public void onStart(ISuite suite) {
        LogUtil.info("🚀 =========================================");
        LogUtil.info("🚀 测试套件开始执行: {}", suite.getName());
        LogUtil.info("🚀 =========================================");

        // 确保开始时Token是空的
        TokenContext.clearGlobalAuthToken();
    }

    @Override
    public void onFinish(ISuite suite) {
        LogUtil.info("✅ =========================================");
        LogUtil.info("✅ 测试套件执行完成: {}", suite.getName());
        LogUtil.info("✅ =========================================");

        // 清理全局Token
        TokenContext.clearGlobalAuthToken();
        LogUtil.info("✅ 全局Token已清理");
    }
}
