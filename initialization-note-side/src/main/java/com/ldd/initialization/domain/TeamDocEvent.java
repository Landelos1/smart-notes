package com.ldd.initialization.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.ldd.initialization.config.handler.JsonbTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 团队文档事件表（版本/评论/动态）
 * 
 * @TableName t_team_doc_event
 */
@TableName(value = "t_team_doc_event")
@Data
public class TeamDocEvent implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "team_id")
    private Long teamId;

    @TableField(value = "doc_id")
    private Long docId;

    @TableField(value = "user_id")
    private Long userId;

    /** 事件类型：1=版本快照 2=评论/批注 3=动态 */
    @TableField(value = "event_type")
    private Integer eventType;

    /** 父事件ID（楼中楼回复） */
    @TableField(value = "parent_id")
    private Long parentId;

    private String content;

    /** 事件元数据（JSON格式） */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String metadata;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 用户信息（非数据库字段） */
    @TableField(exist = false)
    private User user;

    /** 用户名（SQL关联查询结果） */
    @TableField(exist = false)
    private String username;

    /** 头像URL（SQL关联查询结果） */
    @TableField(exist = false)
    private String avatarUrl;

    /** 引用文本（从metadata解析，用于批注） */
    @TableField(exist = false)
    private String quote;

    /** 是否已解决（从metadata解析，用于批注） */
    @TableField(exist = false)
    private Boolean resolved;

    /** 版本号（从metadata解析，用于版本快照） */
    @TableField(exist = false)
    private Integer version;

    /** 版本说明（从metadata解析） */
    @TableField(exist = false)
    private String message;
}
