package com.auto.test.platform.script;

import com.auto.test.platform.common.exception.BusinessException;
import com.auto.test.platform.common.util.LogUtil;
import lombok.extern.slf4j.Slf4j;
import org.testng.ITestContext;

/**
 * 测试专用的全局上下文
 * 使用System Property实现跨域测试类Token共享
 */
@Slf4j
public class TokenContext {
    // System Property中的Key（跨JVM共享）
    private static final String TOKEN_PROPERTY_KEY = "AUTO_TEST_PLATFORM_TOKEN" ;

    // 内存备份（防止System Property被意外清除）
    private static String memoryBackupToken = null;

    /**
     * 设置全局Token（用户登录成功后调用）
     */
    public static void setGlobalAuthToken(String token) {
       if (token == null || token.trim().isEmpty()) {
           throw new BusinessException("Token不能为空");
       }

       System.setProperty(TOKEN_PROPERTY_KEY, token);
       memoryBackupToken = token.trim();
       log.info("✅ Token已设置到全局上下文: {}...", token.trim());
       log.debug("System Property设置状态: {}",
               System.getProperty(TOKEN_PROPERTY_KEY) != null ? "成功" : "失败");
    }

    /**
     * 获取全局Token（所有鉴权接口调用）
     * @return 登录后的有效Token
     */
    public static String getGlobalAuthToken() {
        String token = System.getProperty(TOKEN_PROPERTY_KEY);

        if (token == null || token.trim().isEmpty()) {
            token = memoryBackupToken;

            if (token != null || !token.trim().isEmpty()) {
                LogUtil.debug("从内存备份恢复Token");
            }

            if (token == null || token.trim().isEmpty()) {
                // 详细的错误信息
                String errorMsg = "❌ 全局鉴权Token为空！\n" +
                        "可能的原因：\n" +
                        "1. UserScript.testUserLogin()未执行或执行失败\n" +
                        "2. TokenContext.setGlobalAuthToken()未被调用\n" +
                        "3. 测试类之间的Token传递失败\n" +
                        "4. System Property被意外清除\n\n" +
                        "调试信息：\n" +
                        "- System Property值: " + System.getProperty(TOKEN_PROPERTY_KEY) + "\n" +
                        "- 内存备份值: " + (memoryBackupToken != null ? "有值" : "空");

                LogUtil.error(errorMsg);
                throw new BusinessException("全局鉴权Token为空！请先执行用户登录测试");
            }
        }
        return token.trim();
    }

    /**
     * 检查Token是否存在（不抛异常）
     */
    public static boolean hasToken() {
        String token = System.getProperty(TOKEN_PROPERTY_KEY);
        if (token != null && !token.trim().isEmpty()){
            return true;
        }
        return memoryBackupToken != null && !memoryBackupToken.trim().isEmpty();
    }

    /**
     * 安全获取Token
     */
    public static String getTokenSafely() {
        String token = System.getProperty(TOKEN_PROPERTY_KEY);
        if (token == null || token.trim().isEmpty()) {
            token = memoryBackupToken;
        }
        return token != null ? token.trim() : null;
    }

    /**
     * 清空Token(脚本执行完后释放)
     */
    public static void clearGlobalAuthToken() {
        System.clearProperty(TOKEN_PROPERTY_KEY);
        memoryBackupToken = null;
        log.info("✅ 全局Token已清理");
    }

}
