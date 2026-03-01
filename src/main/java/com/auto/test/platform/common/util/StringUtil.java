package com.auto.test.platform.common.util;

import cn.hutool.core.util.StrUtil;

/**
 * 字符串工具类（封装常用字符串操作，基于hutool，架构必备）
 */
public class StringUtil extends StrUtil {

    /**
     * 空字符串常量
     */
    public static final String EMPTY = "";

    /**
     * 判断字符串是否为空（null、空字符串、全空格都视为空）
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 判断字符串是否非空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 字符串脱敏（用于敏感信息展示，如密码、手机号）
     */
    public static String desensitize(String str, int prefixLen, int suffixLen) {
        if (isEmpty(str)) {
            return EMPTY;
        }
        int len = str.length();
        if (prefixLen +  suffixLen >= len) {
            return str; // 长度不足，不脱敏
        }
        //拼接前缀、星号、后缀
        String prefix = str.substring(0, prefixLen);
        String suffix = str.substring(len - suffixLen);
        return prefix + StrUtil.repeat("*", len-prefixLen-suffixLen);
    }
}
