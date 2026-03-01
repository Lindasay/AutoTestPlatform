package com.auto.test.platform.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 统一日志输出工具
 */
public class LogUtil {
    private static final Logger log =  LoggerFactory.getLogger(LogUtil.class);

    public static void info(String msg) {
        log.info("【自动化脚本】" + msg);
    }

    public static void error(String msg, Object actual) {
        log.error("【自动化脚本失败】" + msg, actual);
    }

    public static void debug(String msg) {
        log.debug("【自动化脚本调试】" + msg);
    }

    public static void info(String msg, Object actual) {
        log.info("【自动化脚本】" + msg + ", response:" + actual);
    }

    public static void debug(String msg, String response) {
        log.debug("【自动化脚本】" + msg + ", response:" + response);
    }

    public static void warn(String msg, Object actual) {
        log.warn("【自动化脚本调试】" + msg, actual);
    }
    public static void error(String message) {
        log.error(message);
    }
}
