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
 * 拦截需要认证的接口，校验Token有效性，解决"Token无效、未携带Token"问题
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

    /**
     * 检查是否为排除路径（与SecurityConfig配置保持一致）
     */
    private boolean isExcludedPath(String uri) {
        log.debug("检查路径是否排除: {}", uri);

        // 1. 处理带上下文路径的请求
        if (uri.startsWith("/auto-test")) {
            String pathWithoutContext = uri.substring("/auto-test".length());
            if (pathWithoutContext.isEmpty()) {
                pathWithoutContext = "/";
            }
            log.debug("去掉上下文路径后: {}", pathWithoutContext);

            // 递归检查去掉上下文后的路径
            return isExcludedPath(pathWithoutContext);
        }

        // 2. 核心修复：必须明确排除 index.html 和根路径
        if (uri.equals("/index.html") ||
                uri.equals("/") ||
                uri.equals("/auto-test/index.html") ||
                uri.equals("/auto-test/")) {
            log.debug("路径为首页，直接排除: {}", uri);
            return true;
        }

        // 3. 业务接口放行（与SecurityConfig一致）
        if (uri.startsWith("/user/login") ||
                uri.startsWith("/user/register") ||
                uri.startsWith("/user/getByUsername") ||
                uri.startsWith("/project") ||
                uri.startsWith("/testCase") ||
                uri.startsWith("/reportData")) {
            log.debug("业务接口路径，排除: {}", uri);
            return true;
        }

        // 4. Swagger相关路径放行
        if (uri.startsWith("/swagger") ||
                uri.startsWith("/webjars") ||
                uri.startsWith("/v3/api-docs") ||
                uri.startsWith("/doc.html") ||
                uri.equals("/favicon.ico")) {
            log.debug("Swagger路径，排除: {}", uri);
            return true;
        }

        // 5. 静态资源路径放行（添加更多常见的静态资源后缀）
        if (uri.startsWith("/static/") ||
                uri.startsWith("/css/") ||
                uri.startsWith("/js/") ||
                uri.startsWith("/images/") ||
                uri.startsWith("/fonts/") ||
                uri.startsWith("/report/") ||
                uri.endsWith(".html") ||
                uri.endsWith(".css") ||
                uri.endsWith(".js") ||
                uri.endsWith(".png") ||
                uri.endsWith(".jpg") ||
                uri.endsWith(".jpeg") ||
                uri.endsWith(".gif") ||
                uri.endsWith(".ico") ||
                uri.endsWith(".svg") ||
                uri.endsWith(".woff") ||
                uri.endsWith(".woff2") ||
                uri.endsWith(".ttf")) {
            log.debug("静态资源路径，排除: {}", uri);
            return true;
        }

        // 6. 错误处理路径放行
        if (uri.equals("/error") || uri.startsWith("/error/")) {
            log.debug("错误处理路径，排除: {}", uri);
            return true;
        }

        log.debug("路径需要Token验证: {}", uri);
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