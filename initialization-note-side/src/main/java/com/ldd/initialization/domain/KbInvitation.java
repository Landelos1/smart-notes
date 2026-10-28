package com.ldd.initialization.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库邀请记录表
 * 
 * @TableName t_kb_invitation
 */
@TableName(value = "t_kb_invitation")
@Data
public class KbInvitation implements Serializable {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 知识库ID
     */
    @TableField(value = "knowledge_base_id")
    private Long knowledgeBaseId;

    /**
     * 邀请人用户ID
     */
    @TableField(value = "inviter_id")
    private Long inviterId;

    /**
     * 被邀请人用户ID
     */
    @TableField(value = "invitee_id")
    private Long inviteeId;

    /**
     * 分配的角色：1-创建者 2-管理员 3-成员
     */
    private Integer role;

    /**
     * 邀请状态：0-待处理 1-已接受 2-已拒绝 3-已过期
     */
    private Integer status;

    /**
     * 邀请时间
     */
    @TableField(value = "invite_time", fill = FieldFill.INSERT)
    private LocalDateTime inviteTime;

    /**
     * 响应时间（接受或拒绝的时间）
     */
    @TableField(value = "response_time")
    private LocalDateTime responseTime;

    /**
     * 过期时间（默认邀请发出后7天）
     */
    @TableField(value = "expire_time")
    private LocalDateTime expireTime;

    /**
     * 邀请附言（可选）
     */
    @TableField(value = "inviter_message")
    private String inviterMessage;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 被邀请人信息
     */
    @TableField(exist = false)
    private User invitee;

    /**
     * 邀请人信息
     */
    @TableField(exist = false)
    private User inviter;

    /**
     * 知识库信息
     */
    @TableField(exist = false)
    private SharedKnowledgeBase knowledgeBase;
}
