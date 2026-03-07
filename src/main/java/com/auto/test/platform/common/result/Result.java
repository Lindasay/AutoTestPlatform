package com.auto.test.platform.common.result;

import com.auto.test.platform.common.constant.ResponseCodeConstant;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 全局通用返回结果
 * 所有接口统一返回格式，前端无需适配多种返回类型
 * @param <T> 数据泛型，支持任意数据类型
 * 补充分页辅助字段，统一字段命名，避免前端解析异常
 */
@Data
public class Result<T> implements Serializable {
    /** 状态码：200-成功 500-系统异常 400-参数错误 404-资源
     * 不存在*/
    private  Integer code;

    /** 返回消息：成功/失败原因 */
    private String msg;

    /** 返回数据：成功时返回，失败时为null */
    private T data;

    // --------------- 简化构造方法（企业级规范，便于开发）---------------

    /**
     * 成功响应（无数据）
     * @return
     * @param <T>
     */
    public static <T> Result<T> success() {
        Result <T> result = new Result<T>();
        result.setCode(ResponseCodeConstant.SUCCESS);
        result.setMsg("操作成功");
        return result;
    }

    //成功响应（有数据）
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResponseCodeConstant.SUCCESS);
        result.setMsg("操作成功");
        result.setData(data);
        return  result;
    }

    //失败响应（自定义消息）
    public static <T> Result<T> fail(String msg) {
        Result<T> result = new Result<T>();
        result.setCode(ResponseCodeConstant.SYSTEM_ERROR);
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

    public static <T> Result<T> success(T data, String message) {
        Result<T> result = new Result<>();
        result.setCode(ResponseCodeConstant.SUCCESS);
        result.setMsg(message);
        result.setData(data);
        return  result;
    }

    public static Result<?> error(String message) {
        Result<?> result = new Result<>();
        result.setCode(ResponseCodeConstant.SYSTEM_ERROR);
        result.setMsg(message);
        result.setData(null);
        return  result;
    }
}
