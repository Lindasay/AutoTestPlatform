package com.auto.test.platform.common.exception;


import com.auto.test.platform.common.constant.ResponseCodeConstant;
import com.auto.test.platform.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理器（Spring Boot3.x）
 * 捕获所有Controller层异常，统一返回Result格式，避免前端报500页面
 * @RestControllerAdvice = = @ControllerAdvice + @ResponseBody
 */
@Slf4j
@RestControllerAdvice //全局异常拦截，仅处理Controller层
public class GlobalExceptionHandler {

    //拦截自定义业务异常（核心，业务逻辑异常用这个）
    @ExceptionHandler(value = BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.error("业务异常：{}",e.getMessage(),e);
        return Result.fail(e.getCode(),e.getMessage());
    }

    //拦截参数校验异常如@NotBlank、@Min等注解的校验失败）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleParamValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        //获取第一个校验失败的字段和消息
        FieldError fieldError = bindingResult.getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : null;
        log.error("参数校验异常：{}", message);
        return Result.fail(ResponseCodeConstant.PARAM_ERROR, message);
    }


    /**
     * 处理缺少路径参数异常（如接口路径中未传递id）
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public Result<?> handleMissingPathVariableException(MissingPathVariableException e) {
        log.error("缺少路径参数异常:{}",e.getMessage(),e);
        String message = "缺少必要的路径参数：" + e.getVariableName();
        return Result.fail(ResponseCodeConstant.PARAM_ERROR, message);
    }

    /**
     * 处理缺少请求参数异常（如GET请求未传递pageNum）
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.error("缺少请求参数异常：{}",e.getMessage(),e);
        String message = "缺少必要的请求参数"+e.getParameterName();
        return Result.fail(ResponseCodeConstant.PARAM_ERROR, message);
    }

    /**
     * 处理参数类型不匹配异常（如路径参数id应为long，却传递字符串）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<?> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error("参数类型不匹配异常：{}",e.getMessage(),e);
        String message = "参数类型不匹配，参数" +  e.getName() + "应属于" + e.getRequiredType().getSimpleName() + "类型";
        return Result.fail(ResponseCodeConstant.PARAM_ERROR, message);
    }

    /**
     * 处理数据库重复建异常（如唯一索引异常）
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<?> handleDuplicateKeyException(DuplicateKeyException e) {
        log.error("数据库重复键异常：{}",e.getMessage(),e);
        return Result.fail(ResponseCodeConstant.DATA_ALREADY_EXIST,"数据已存在，请勿重复提交");
    }

    //拦截所有未捕获异常（兜底，避免系统崩溃）
    @ExceptionHandler(value = Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常：{}",e.getMessage(),e);
        return Result.fail(ResponseCodeConstant.SYSTEM_ERROR,"系统异常，请联系管理员");
    }

}
