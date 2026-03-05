package com.auto.test.platform.entity;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 用例内容结构化（约束caseContent的JSON字段）
 * 专门用于解析和校验接口用例的caseContent字段
 */
@Data
public class CaseContent {

    /**
     * 接口请求URL（必传）
     */
    @NotBlank(message = "requestUrl不能为空")
    private String requestUrl;

    /**
     * 请求方法（可选，默认GET）
     * 仅允许：GET/POST/PUT/DELETE/PATCH
     */
    @Pattern(regexp = "^(GET|POST|PUT|DELETE|PATCH)?$", message = "requestMethod仅支持GET/POST/PUT/DELETE/PATCH")
    private String requestMethod;

    /**
     * 请求参数（可选，JSON格式字符串）
     */
    private String requestParams;

    /**
     * 预期结果（可选，用于断言响应包含该字符串）
     */
    private String expectedResult;

    /**
     * 请求头（可选，JSON格式字符串）
     */
    private String requestHeaders;

    /**
     * 超时时间（可选，默认3000ms）
     */
    private Integer timeout = 3000;

}
