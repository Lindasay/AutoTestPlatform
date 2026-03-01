package com.auto.test.platform.service.impl;

import com.auto.test.platform.entity.User;
import com.auto.test.platform.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户详情服务 - 用于Spring Security认证
 * 重要：此类不依赖UserService，避免AOP代理循环
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Spring Security加载用户详情，用户名：{}", username);

        // 直接通过Mapper查询，避免通过UserService代理
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            log.warn("用户不存在，用户名：{}", username);
            throw new UsernameNotFoundException("用户不存在：" + username);
        }

        //检查用户状态
        if (user.getStatus() == null && user.getStatus() == 0) {
            log.warn("用户已被禁用，用户名：{}", username);
            throw new UsernameNotFoundException("用户已被禁用：" + username);
        }

        //构建权限列表
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (user.getRole() != null && !user.getRole().isEmpty()){
            authorities.add(new SimpleGrantedAuthority(user.getRole()));
        }

        return new org.springframework.security.core.userdetails.User(user.getUserName(), user.getPassword(),
                true,
                true,
                true,
                true,
                authorities);
    }
}
