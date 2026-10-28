package com.ldd.initialization.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充处理器
 * 自动填充 create_time 和 update_time 字段
 */
@Component
public class MyBatisMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入时自动填充
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        
        // 自动填充 create_time 字段
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        
        // 自动填充 update_time 字段
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        
        // 自动填充其他可能的时间字段
        this.strictInsertFill(metaObject, "uploadTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "joinTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
    }

    /**
     * 更新时自动填充
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        
        // 自动填充 update_time 字段
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, now);
        
        // 自动填充其他可能的时间字段
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, now);
    }
}
