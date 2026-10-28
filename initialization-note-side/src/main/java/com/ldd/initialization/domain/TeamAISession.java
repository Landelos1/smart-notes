package com.ldd.initialization.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 团队AI会话表
 * 
 * @TableName t_team_ai_session
 */
@TableName(value = "t_team_ai_session")
@Data
public class TeamAISession implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "team_id")
    private Long teamId;

    @TableField(value = "user_id")
    private Long userId;

    private String title;

    @TableField(value = "is_pinned")
    private Boolean isPinned;

    /** 状态：1=正常 0=删除/禁用 */
    private Integer status;

    @TableField(value = "last_message")
    private String lastMessage;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
