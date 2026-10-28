package com.ldd.initialization.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库邀请展示VO
 */
@Data
public class KbInvitationVO {

    /**
     * 邀请ID
     */
    private Long id;

    /**
     * 知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * 知识库名称
     */
    private String knowledgeBaseName;

    /**
     * 知识库封面URL
     */
    private String knowledgeBaseCover;

    /**
     * 知识库描述
     */
    private String knowledgeBaseDescription;

    /**
     * 邀请人用户ID
     */
    private Long inviterId;

    /**
     * 邀请人用户名
     */
    private String inviterName;

    /**
     * 邀请人头像URL
     */
    private String inviterAvatar;

    /**
     * 分配的角色：1-创建者 2-管理员 3-成员
     */
    private Integer role;

    /**
     * 角色显示名称
     */
    private String roleLabel;

    /**
     * 邀请状态：0-待处理 1-已接受 2-已拒绝 3-已过期
     */
    private Integer status;

    /**
     * 邀请时间
     */
    private String inviteTime;

    /**
     * 过期时间
     */
    private String expireTime;

    /**
     * 剩余天数
     */
    private Integer daysLeft;

    /**
     * 邀请附言
     */
    private String inviterMessage;
}
