package com.ldd.initialization.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 团队协作空间表
 * 
 * @TableName t_team_space
 */
@TableName(value = "t_team_space")
@Data
public class TeamSpace implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    @TableField(value = "avatar_url")
    private String avatarUrl;

    @TableField(value = "creator_id")
    private Long creatorId;

    @TableField(value = "is_public")
    private Boolean isPublic;

    @TableField(value = "member_count")
    private Integer memberCount;

    @TableField(value = "doc_count")
    private Integer docCount;

    @TableField(value = "last_active_time")
    private LocalDateTime lastActiveTime;

    @TableField(value = "invite_code")
    private String inviteCode;

    @TableField(value = "invite_role")
    private Integer inviteRole;

    @TableField(value = "invite_expire_time")
    private LocalDateTime inviteExpireTime;

    @TableField(value = "invite_max_uses")
    private Integer inviteMaxUses;

    @TableField(value = "invite_used_count")
    private Integer inviteUsedCount;

    private Integer status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 当前用户在团队中的角色（非数据库字段） */
    @TableField(exist = false)
    private Integer myRole;
}
