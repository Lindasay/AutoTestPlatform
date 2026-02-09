package com.auto.test.platform.common.exception;

import com.auto.test.platform.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器（Spring Boot3.x）
 * 捕获所有Controller层异常，统一返回Result格式，避免前端报500页面
 * @RestControllerAdvice = = @ControllerAdvice + @ResponseBody
 */
@Slf4j
@RestControllerAdvice //全局异常拦截，仅处理Controller层
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常（@Valid/@Validated）
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        //拼接所有参数错误信息
        String msg = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败：{}", msg);
        return Result.fail(400,msg);
    }

    /**
     * 处理自定义运行时异常（业务层手动抛出）
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        log.error("业务运行异常",e);
        return Result.fail(e.getMessage());
    }

    /**
     * 处理所有未捕获的异常（兜底）
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统未知异常：" , e);
        return Result.fail("系统内部错误，请联系管理员");
    }
}
