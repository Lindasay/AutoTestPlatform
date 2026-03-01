package com.auto.test.platform.common.interceptor;

import cn.hutool.jwt.JWTUtil;
import com.alibaba.fastjson2.JSON;
import com.auto.test.platform.common.result.Result;
import com.auto.test.platform.common.util.TokenUtil;
import com.mchange.v2.log.LogUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * Token拦截器，验证所有需要认证的接口（适配SecurityConfig，用于接口Token验证）
 * 拦截需要认证的接口，校验Token有效性，解决“Token无效、未携带Token”问题
 * 与TokenUtil工具类联动，异常统一返回Result格式，对齐现有接口返回规范
 */
@Slf4j
@Component
public class TokenInterceptor implements HandlerInterceptor {

    @Resource
    private TokenUtil tokenUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        log.debug("Token拦截器：{} {}", requestURI, method);

        if (isExcludedPath(requestURI)){
            return true;
        }


        //获取Token
        String token = request.getHeader("Authorization");
        if (token == null || token.trim().isEmpty()) {
            log.warn("接口 {} 未提供Token", requestURI);
            sendErrorResponse(response,401,"未提供Token");
            return false;
        }

        // ✅ 重要：移除可能的Bearer前缀
        token = removeBearerPrefix(token);
        log.debug("处理后Token: {}...", token.substring(0, Math.min(20, token.length())));

        //使用TokenUtil验证Token
        if (!tokenUtil.validateToken(token)) {
            log.warn("Token验证失败");
            sendErrorResponse(response,403,"Token无效或已过期");
            return false;
        }

        //从Token中获取用户信息
        String username = tokenUtil.getUsernameByToken(token);
        String role = tokenUtil.getRoleByToken(token);

        //将用户信息存入请求上下文
        request.setAttribute("currentUser", username);
        request.setAttribute("userRole", role);

        log.debug("Token验证通过 - 用户：{},角色: {}, 接口: {}",username, role,requestURI);
        return true;
    }

    /**
     * 移除Bearer前缀
     */
    private String removeBearerPrefix(String token) {
        if (token == null) {
            return null;
        }

        // 移除Bearer前缀（不区分大小写）
        String trimmedToken = token.trim();
        if (trimmedToken.toLowerCase().startsWith("bearer ")) {
            return trimmedToken.substring(7).trim();
        }
        return trimmedToken;
    }


    private boolean isExcludedPath(String uri) {
        // 公开接口（不需要Token）
        if (uri.startsWith("/user/login") ||
                uri.startsWith("/user/register") ||
                uri.startsWith("/user/getByUsername") || uri.startsWith("/project") || uri.startsWith("/testCase")) {
            return true;
        }

        // Swagger文档接口
        if (uri.startsWith("/swagger") ||
                uri.startsWith("/webjars") ||
                uri.startsWith("/v3/api-docs") ||
                uri.startsWith("/doc.html") ||
                uri.equals("/favicon.ico")) {
            return true;
        }

        // 带上下文的路径
        if (uri.startsWith("/auto-test")) {
            String pathWithoutContext = uri.substring("/auto-test".length());
            return isExcludedPath(pathWithoutContext);
        }

        return false;
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = String.format("{\"code\":%d,\"msg\":\"%s\",\"data\":null}",status,message);

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();

    }
}