package com.auto.test.platform.service.impl;

import com.auto.test.platform.common.constant.ResponseCodeConstant;
import com.auto.test.platform.common.result.Result;
import com.auto.test.platform.common.util.AssertUtil;
import com.auto.test.platform.entity.User;
import com.auto.test.platform.mapper.UserMapper;
import com.auto.test.platform.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户Service实现类
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private BCryptPasswordEncoder passwordEncoder;

    @Resource
    private AuthenticationManager authenticationManager;

    /**
     * 生成Token（简化版）
     */
    private String generateToken(User user) {
        return user.getUserName() + "-" + user.getRole() + "-" + System.currentTimeMillis();
    }

    /**
     * 用户注册
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> register(User user) {
        try {
            log.info("用户注册开始，用户名：{}", user.getUserName());

            // 1.参数校验
            AssertUtil.notBlank(user.getUserName(), "用户名不能为空");
            AssertUtil.notBlank(user.getPassword(), "密码不能为空");
            AssertUtil.isTrue(user.getUserName().length() <= 50, "用户名不能超过50个字符");

            // 校验用户状态（只能是0或1）
            if (user.getStatus() != null) {
                AssertUtil.isTrue(user.getStatus() == 0 || user.getStatus() == 1,
                        "用户状态只能是启用（1）或禁用（0）");
            }

            // 2.校验用户名唯一
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getUserName, user.getUserName());
            User existUser = baseMapper.selectOne(queryWrapper);
            AssertUtil.isTrue(existUser == null, ResponseCodeConstant.DATA_ALREADY_EXIST, "用户名已存在，请更换");

            // 3.密码加密处理
            user.setPassword(passwordEncoder.encode(user.getPassword()));

            // 4.设置默认值
            if (user.getRole() == null || user.getRole().isEmpty()) {
                user.setRole("tester");
            }
            if (user.getStatus() == null) {
                user.setStatus(1); // 默认启用
            }

            // 5.新增用户
            int result = baseMapper.insert(user);
            log.info("用户注册成功，用户名：{}，插入结果：{}", user.getUserName(), result);

            return Result.success("用户注册成功");
        } catch (Exception e) {
            log.error("用户注册异常，用户名：{}，异常：{}", user.getUserName(), e.getMessage(), e);
            return Result.fail("用户注册失败");
        }
    }

    /**
     * 用户登录
     */
    @Override
    public Result<?> login(String username, String password) {
        try {
            log.info("用户登录开始，用户名：{}", username);

            // 1.参数校验
            if (username == null || username.trim().isEmpty()) {
                return Result.fail("用户名不能为空");
            }
            if (password == null || password.trim().isEmpty()) {
                return Result.fail("密码不能为空");
            }

            // 2.Spring Security身份验证
            Authentication authentication;
            try {
                authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(username, password)
                );
            } catch (BadCredentialsException e) {
                log.warn("用户名或密码错误，用户名：{}", username);
                return Result.fail(ResponseCodeConstant.USERNAME_PASSWORD_ERROR, "用户名或密码错误");
            } catch (DisabledException e) {
                log.warn("账户被禁用，用户名：{}", username);
                return Result.fail("账户已被禁用，请联系管理员");
            }catch (UsernameNotFoundException e) {
                log.warn("用户不存在，用户名：{}", username);
                return Result.fail("用户不存在");
            }

            // 3.验证认证结果
            if (authentication == null || !authentication.isAuthenticated()) {
                return Result.fail(ResponseCodeConstant.USERNAME_PASSWORD_ERROR, "用户名或密码错误");
            }

            // 4.存储认证信息
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 5.查询用户信息
            UserDetails userDetails = (UserDetails)authentication.getPrincipal();
            User user = userMapper.selectByUsername(userDetails.getUsername());
            if (user == null) {
                return Result.fail("用户不存在");
            }

            // 6.检查用户状态
            if (user.getStatus() != null && user.getStatus() == 0) {
                return Result.fail("账户已被禁用，请联系管理员");
            }

            // 7.生成Token并返回
            String token = generateToken(user);

            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("userInfo", user);
            result.put("username", user.getUserName());
            result.put("role", user.getRole());

            log.info("用户登录成功，用户名：{}", username);
            return Result.success(result);

        } catch (Exception e) {
            log.error("用户登录异常，用户名：{}，异常：{}", username, e.getMessage(), e);
            return Result.fail("登录失败，系统异常");
        }
    }

    /**
     * 根据用户名查询用户
     */
    @Override
    public Result<User> getUserByUsername(String username) {
        try {
            log.info("查询用户开始，用户名：{}", username);

            // 1.参数校验
            AssertUtil.notBlank(username, "用户名不能为空");

            // 2.查询用户
            User user = userMapper.selectByUsername(username);
            if (user == null) {
                log.warn("用户不存在，用户名：{}", username);
                return Result.fail("用户不存在");
            }

            // 3.脱敏处理（不返回密码）
            user.setPassword(null);

            log.info("查询用户成功，用户名：{}", username);
            return Result.success(user);

        } catch (Exception e) {
            log.error("查询用户异常，用户名：{}，异常：{}", username, e.getMessage(), e);
            return Result.fail("查询用户失败");
        }
    }
}