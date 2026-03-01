package com.auto.test.platform.common.util;


import com.auto.test.platform.entity.User;
import com.auto.test.platform.mapper.UserMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Token工具类（适配SecurityConfig，用于Token解析、有效性校验）
 * 与UserServiceImpl中generateToken方法逻辑对应，可直接复用，无需修改
 */
@Slf4j
@Component
public class TokenUtil {

    @Resource
    private UserMapper userMapper;

    //Token有效期：2小时（7200000毫秒，可根据需求调整）
    private static final long TOKEN_EXPIRE_TIME = 7200000L;

    //解析Token，获取用户名和角色
    public String[] parseToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        //移除Bearer前缀
        token = removeBearerPrefix(token);

        // 拆分Token（与generateToken拼接规则一致：用户名-角色-时间戳）
        try {
            String[] tokenParts = token.split("-");
            // 校验Token格式（必须是3段）
            if (tokenParts.length != 3) {
                log.warn("Token格式错误，应为3段：{}", token);
                return null;
            }

            // 校验Token有效期（时间戳差值是否超过2小时）
            long timestamp = Long.parseLong(tokenParts[2]);
            if (System.currentTimeMillis() - timestamp > TOKEN_EXPIRE_TIME) {
                log.warn("Token已过期：{}", token);
                return null;
            }

            //校验用户名是否存在（避免无效Token）
            User user = userMapper.selectByUsername(tokenParts[0]);
            if (Objects.isNull(user) || user.getStatus() == 0) {
                log.warn("用户不存在或已禁用：{}", user);
                return null;
            }

            //校验Token中角色与数据库角色一致（防止Token篡改）
            if (!tokenParts[1].equals(user.getRole())){
                log.warn("Token角色与数据库一致：Token角色={}, 数据库角色={}", tokenParts[1], user.getRole());
                return null;
            }

            return tokenParts;
        } catch (Exception e) {
            log.error("解析Token失败：{}",token,e);
           return null;
        }
    }

    // 移除Bearer前缀
    private String removeBearerPrefix(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return token;
    }

    //校验Token有效性
    public boolean validateToken(String token) {
        return parseToken(token) != null;
    }

    //根据Token获取用户角色
    public String getRoleByToken(String token) {
        String[] tokenParts = parseToken(token);
        return tokenParts != null ? tokenParts[1] : null;
    }

    //根据Token获取用户名
    public  String getUsernameByToken(String token) {
        String[] tokenParts = parseToken(token);
        return tokenParts != null ? tokenParts[0] : null;
    }
}
