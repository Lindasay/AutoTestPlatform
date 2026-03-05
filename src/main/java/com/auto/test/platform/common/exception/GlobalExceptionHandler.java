package com.auto.test.platform.common.exception;

import com.auto.test.platform.common.constant.ResponseCodeConstant;
import com.auto.test.platform.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
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
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.error("业务异常：{}",e.getMessage(),e);
        return Result.fail(e.getCode(),e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleParamValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        FieldError fieldError = bindingResult.getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : null;
        log.error("参数校验异常：{}", message);
        return Result.fail(ResponseCodeConstant.PARAM_ERROR, message);
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public Result<?> handleMissingPathVariableException(MissingPathVariableException e) {
        log.error("缺少路径参数异常:{}",e.getMessage(),e);
        String message = "缺少必要的路径参数：" + e.getVariableName();
        return Result.fail(ResponseCodeConstant.PARAM_ERROR, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.error("缺少请求参数异常：{}",e.getMessage(),e);
        String message = "缺少必要的请求参数"+e.getParameterName();
        return Result.fail(ResponseCodeConstant.PARAM_ERROR, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<?> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error("参数类型不匹配异常：{}",e.getMessage(),e);
        String message = "参数类型不匹配，参数" +  e.getName() + "应属于" + e.getRequiredType().getSimpleName() + "类型";
        return Result.fail(ResponseCodeConstant.PARAM_ERROR, message);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<?> handleDuplicateKeyException(DuplicateKeyException e) {
        log.error("数据库重复键异常：{}",e.getMessage(),e);
        return Result.fail(ResponseCodeConstant.DATA_ALREADY_EXIST,"数据已存在，请勿重复提交");
    }

    // 简化异常处理：只处理业务异常，静态资源异常交给Spring
    @ExceptionHandler(value = Exception.class)
    public Result<?> handleException(Exception e, HttpServletRequest request) throws Exception {
        String uri = request.getRequestURI();

        // 如果是静态资源请求，直接抛出异常，让Spring返回默认404
        if (isStaticResourceRequest(uri)) {
            log.warn("静态资源请求异常 [{}]，交给Spring处理: {}", uri, e.getMessage());
            throw e;
        }

        // 只处理业务接口异常
        if (isBusinessApiRequest(uri)) {
            log.error("业务接口系统异常 [{}]: {}", uri, e.getMessage(), e);
            return Result.fail(501, "系统异常，请联系管理员");
        }

        // 其他情况也交给Spring处理
        throw e;
    }

    private boolean isStaticResourceRequest(String uri) {
        return uri.startsWith("/static/") ||
                uri.startsWith("/report/") ||
                uri.endsWith(".html") ||
                uri.endsWith(".css") ||
                uri.endsWith(".js") ||
                uri.endsWith(".png") ||
                uri.endsWith(".jpg") ||
                uri.endsWith(".ico") ||
                uri.equals("/index.html") ||
                uri.equals("/") ||
                uri.startsWith("/auto-test/static/") ||
                uri.startsWith("/auto-test/report/") ||
                uri.equals("/auto-test/index.html");
    }

    private boolean isBusinessApiRequest(String uri) {
        return uri.startsWith("/auto-test/project") ||
                uri.startsWith("/auto-test/testCase") ||
                uri.startsWith("/auto-test/reportData") ||
                uri.startsWith("/auto-test/user") ||
                uri.startsWith("/project") ||
                uri.startsWith("/testCase") ||
                uri.startsWith("/reportData") ||
                uri.startsWith("/user");
    }
}