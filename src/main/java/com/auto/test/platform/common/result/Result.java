package com.auto.test.platform.common.result;

import lombok.Data;

/**
 * 全局通用返回结果
 * 所有接口统一返回格式，前端无需适配多种返回类型
 * @param <T> 数据泛型，支持任意数据类型
 */
@Data
public class Result<T> {
    /** 状态码：200-成功 500-系统异常 400-参数错误 404-资源不存在*/
    private  Integer code;

    /** 返回消息：成功/失败原因 */
    private String msg;

    /** 返回数据：成功时返回，失败时为null */
    private T data;

    /** 时间戳：接口响应时间（毫秒），便于问题排查 */
    private Long timestamp;

    // 构造方法：自动填充时间戳
    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    //成功响应（无数据）
    public static <T> Result<T> success() {
        return success(null);
    }

    //成功响应（有数据）
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<T>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        return  result;
    }

    //失败响应（自定义消息）
    public static <T> Result<T> fail(String msg) {
        Result<T> result = new Result<T>();
        result.setCode(500);
        result.setMsg(msg);
        result.setData(null);
        return  result;
    }

    //失败响应（自定义状态码+消息。如参数错误400）
    public static <T> Result<T> fail(Integer code, String msg) {
        Result<T> result = new Result<T>();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(null);
        return  result;
    }
}
