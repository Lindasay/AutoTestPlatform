package com.auto.test.platform.controller;

import com.auto.test.platform.common.result.Result;
import com.auto.test.platform.entity.User;
import com.auto.test.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户接口控制器（注册、登录）
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/user")
@Tag(name = "用户管理接口", description = "注册、登录、查询等核心业务接口")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @Operation(summary = "注册用户", description = "传入用户名、密码，完成用户注册，校验用户名唯一")
    public Result<?> register(@Valid @RequestBody User user) {
        long startTime = System.currentTimeMillis();
        log.info("用户注册开始，用户名：{}", user.getUserName());

        try {
            Result<?> result = userService.register(user);
            log.info("用户注册完成，用户名：{}，耗时：{}ms",
                    user.getUserName(), System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            log.error("用户注册异常，用户名：{}，耗时：{}ms，异常：{}",
                    user.getUserName(), System.currentTimeMillis() - startTime, e.getMessage(), e);
            return Result.fail("用户注册失败");
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "传入用户名、密码，完成用户登录")
    public Result<?> login(@Valid @RequestBody LoginRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("用户登录开始，用户名：{}", request.getUsername());

        try {
            Result<?> result = userService.login(request.getUsername(), request.getPassword());
            log.info("用户登录完成，用户名：{}，耗时：{}ms",
                    request.getUsername(), System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            log.error("用户登录异常，用户名：{}，耗时：{}ms，异常：{}",
                    request.getUsername(), System.currentTimeMillis() - startTime, e.getMessage(), e);
            return Result.fail("用户登录失败");
        }
    }

    /**
     * 根据用户名查询用户
     */
    @GetMapping("/getByUsername")
    @Operation(summary = "根据用户名查询用户信息", description = "传入用户名，完成用户查询")
    public Result<User> getByUsername(@RequestParam String username) {

        long startTime = System.currentTimeMillis();
        log.info("查询用户开始，用户名：{}", username);

        try {
            Result<User> result = userService.getUserByUsername(username);
            log.info("查询用户完成，用户名：{}，耗时：{}ms",
                    username, System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            log.error("查询用户异常，用户名：{}，耗时：{}ms，异常：{}",
                    username, System.currentTimeMillis() - startTime, e.getMessage(), e);
            return Result.fail("查询用户失败");
        }
    }

    /**
     * 登录请求参数类
     */
    @Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;
    }
}