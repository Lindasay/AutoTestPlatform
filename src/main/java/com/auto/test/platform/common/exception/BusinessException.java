package com.auto.test.platform.common.exception;

import lombok.Getter;

/**
 * 自定义业务异常（全局统一，用于业务逻辑校验，架构必备）
 */
@Getter
public class BusinessException extends RuntimeException{
    private  final Integer code; //异常响应码（与ResponseCodeConstant对应）

    /**
     * 带响应码和消息的构造方法
     * @param code
     * @param message
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    //简化构造函数，默认系统异常码500
    public BusinessException(String message) {
        this(500,message);
    }
}
