package com.ldd.initialization.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.ldd.initialization.config.handler.JsonbTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 团队AI消息表
 * 
 * @TableName t_team_ai_message
 */
@TableName(value = "t_team_ai_message")
@Data
public class TeamAIMessage implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "session_id")
    private Long sessionId;

    @TableField(value = "user_id")
    private Long userId;

    /** 角色：0=system 1=user 2=assistant */
    private Integer role;

    private String content;

    /** 引用信息（JSON格式） */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String citations;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
