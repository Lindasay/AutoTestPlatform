package com.auto.test.platform.common.util;

import com.microsoft.playwright.Playwright;

/**
 * Playwright工具类（UI自动化核心，封装浏览器操作，架构automation层依赖）
 */
public class PlaywrightUtil {

    /**
     * 初始化Playwright（启动浏览器）
     * @param browserType 浏览器类型chromium、firefox、webkit）
     * @return Playwright实例
     */
    public static Playwright init(String browserType){
        Playwright playwright = Playwright.create();
        return playwright;
    }

    /**
     * 关闭Playwright（释放资源）
     * @param playwright
     */
    public static void close(Playwright playwright){
        if(playwright!=null){
            playwright.close();
        }
    }

}
