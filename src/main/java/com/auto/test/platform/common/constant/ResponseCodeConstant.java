package com.auto.test.platform.common.constant;

/**
 * 响应码常量（全局统一，避免魔法值，架构必备）
 */
public class ResponseCodeConstant {

    // 成功
    public static final int SUCCESS = 200;
    // 参数错误
    public static final int PARAM_ERROR = 400;
    // 未登录/权限不足
    public static final int NO_AUTH = 401;
    // 接口不存在
    public static final int INTERFACE_NOT_EXIST = 404;
    // 数据已存在
    public static final int DATA_ALREADY_EXIST = 409;
    // 业务异常
    public static final int BUSINESS_ERROR = 500;
    // 系统异常
    public static final int SYSTEM_ERROR = 501;
    //用户名密码错误
    public static final int USERNAME_PASSWORD_ERROR = 1004;

    // 补充异常提示常量（可选，便于统一维护）
    // 用户名或密码错误

    public static final String PARAM_ERROR_MSG = "参数错误，请检查请求参数";
    public static final String NO_AUTH_MSG = "权限不足，请先登录";
    public static final String DATA_ALREADY_EXIST_MSG = "数据已存在，请勿重复操作";
    public static final String SYSTEM_ERROR_MSG = "系统异常，请联系管理员";
}
