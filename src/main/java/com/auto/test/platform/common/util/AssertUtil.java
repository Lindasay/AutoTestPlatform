package com.auto.test.platform.common.util;

import com.auto.test.platform.common.constant.ResponseCodeConstant;
import com.auto.test.platform.common.exception.BusinessException;

/**
 * 自定义断言工具（简化业务逻辑校验，避免重读if-else，架构必备）
 */
public class AssertUtil {

    /**
     * 断言对象不为null，否则抛出数据不存在异常
     */
    public  static void notNull(Object object, String message) {
        if(object == null) {
            throw new BusinessException(ResponseCodeConstant.INTERFACE_NOT_EXIST,message);
        }
    }

    /**
     * 断言条件为true，否则抛出业务异常
     */
    public static void isTrue(boolean condition, String message) {
        if(!condition) {
            throw new BusinessException(message);
        }
    }

    /**
     * 断言条件为true，否则抛出指定响应码的业务异常
     */
    public static void isTrue(boolean condition, Integer code, String message) {
        if(!condition) {
            throw new BusinessException(code, message);
        }
    }

    /**
     * 断言字符串不为空（非null，非空字符串），否则抛出参数错误异常
     */
    public static void notBlank(String str, String message) {
        if(str == null || str.trim().isEmpty()) {
            throw new BusinessException(ResponseCodeConstant.PARAM_ERROR,message);
        }
    }


}
