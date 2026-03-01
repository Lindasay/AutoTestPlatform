package com.auto.test.platform.service;

import com.auto.test.platform.common.result.Result;
import com.auto.test.platform.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户Service接口
 */
public interface UserService extends IService<User> {

    //用户注册
    Result<?> register(User user);

    //用户登录（返回Token）
    Result<?> login(String username, String password);

    //根据用户名查询用户
    Result<User> getUserByUsername(String username);
}
