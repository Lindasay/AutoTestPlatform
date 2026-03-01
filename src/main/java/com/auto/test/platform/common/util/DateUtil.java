package com.auto.test.platform.common.util;

import cn.hutool.core.date.DateTime;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期工具类（封装常用日期操作，基于hutool，架构必备）
 */
public class DateUtil extends cn.hutool.core.date.DateUtil {

    /**
     * 标准日期时间格式（yyyy-MM-dd HH:mm:ss）
     */
    public static final String STANDARD_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * LocalDateTime转字符串（标准格式）
     */
    public static String localDateTimeToString(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(STANDARD_FORMAT);
        return localDateTime.format(formatter);
    }

    /**
     * 字符串转LocalDateTime（标准格式）
     */
    public static String stringToLocalDateTime(String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(STANDARD_FORMAT);
        return LocalDateTime.parse(str, formatter).format(formatter);
    }

    /**
     * 获取当前时间字符串（标准格式）
     */
//    public static String getCurrentTimeStr() {
//        return DateUtil.now(STANDARD_FORMAT);
//    }

    /**
     * 计算两个日期的时间差（毫秒）
     */
    public static long getTimeDiffMs(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        DateTime startDateTime = cn.hutool.core.date.DateUtil.date(start);
        DateTime endDateTime = cn.hutool.core.date.DateUtil.date(end);

        return endDateTime.getTime() -startDateTime.getTime();
    }

}
