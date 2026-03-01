package com.auto.test.platform.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 实体类字段自动填充配置（架构config层必备）
 * 作用：自动填充实体类中的createTime、updateTime字段，无需手动设置，减少冗余代码
 */
@Component //// 注入Spring容器，让MyBatis-Plus扫描到
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入操作时，自动填充createTime、updateTime字段
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        /// 兼容3.5.x版本的写法（非严格模式，更容易生效）
        this.setFieldValByName("createTime", LocalDateTime.now(), metaObject);
        this.setFieldValByName("updateTime", LocalDateTime.now(), metaObject);

        // 如果你坚持用严格模式，需确保字段名和类型完全匹配：
        // this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        // this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 更新数据时自动填充
     * @param metaObject 元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
    }
}
