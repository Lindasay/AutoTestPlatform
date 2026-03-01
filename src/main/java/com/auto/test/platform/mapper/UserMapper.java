package com.auto.test.platform.mapper;

import com.auto.test.platform.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户Mapper接口（继承BaseMapper，无需手动编写基础的CRUD方法）
 */
public interface UserMapper extends BaseMapper<User> {

    //根据用户名查询用户（用于登录验证）
    @Select("SELECT * FROM user WHERE username = #{username}")
    User selectByUsername(@Param("username") String username);
}
