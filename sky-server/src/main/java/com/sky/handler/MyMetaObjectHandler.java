package com.sky.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.sky.context.BaseContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("开始自动填充插入字段...");
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "createUser", Long.class, currentId);
        this.strictInsertFill(metaObject, "updateUser", Long.class, currentId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("开始自动填充更新字段...");
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictUpdateFill(metaObject, "updateUser", Long.class, currentId);
    }
}