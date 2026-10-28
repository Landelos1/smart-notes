package com.ldd.initialization.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 团队相关VO集合
 */
public class TeamVO {

    /**
     * 简单用户信息（用于嵌套）
     */
    @Data
    public static class SimpleUserVO {
        private Long id;
        private String username;
        private String avatarUrl;
    }

    /**
     * 团队空间VO
     */
    @Data
    public static class TeamSpaceVO {
        private Long id;
        private String name;
        private String description;
        private String avatarUrl;
        private Long creatorId;
        private Boolean isPublic;
        private Integer memberCount;
        private Integer docCount;
        private LocalDateTime lastActiveTime;
        /** 邀请码（仅管理员可见） */
        private String inviteCode;
        private LocalDateTime inviteExpireTime;
        /** 当前用户在团队中的角色：0=创建者 1=管理员 2=编辑者 3=查看者 */
        private Integer myRole;
        private LocalDateTime createTime;
    }

    /**
     * 团队成员VO
     */
    @Data
    public static class TeamMemberVO {
        private Long id;
        private Long userId;
        private String username;
        private String avatarUrl;
        private String email;
        /** 角色：0=创建者 1=管理员 2=编辑者 3=查看者 */
        private Integer role;
        private Integer status;
        private LocalDateTime joinTime;
    }

    /**
     * 文档简要VO（列表用）
     */
    @Data
    public static class DocBriefVO {
        private Long id;
        private Long teamId;
        private String title;
        private String excerpt;
        private Integer status;
        private Boolean isPinned;
        private Integer version;
        private Long lockedBy;
        private SimpleUserVO lockedByUser;
        private LocalDateTime lockExpireTime;
        private SimpleUserVO updatedByUser;
        private LocalDateTime updateTime;
        private List<String> tags;
    }

    /**
     * 文档详情VO
     */
    @Data
    public static class DocDetailVO {
        private Long id;
        private Long teamId;
        private String title;
        private String contentMd;
        private String contentHtml;
        private Integer status;
        private Boolean isPinned;
        private Integer version;
        private Long lockedBy;
        private SimpleUserVO lockedByUser;
        private String lockToken;
        private LocalDateTime lockExpireTime;
        private SimpleUserVO createdByUser;
        private SimpleUserVO updatedByUser;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }

    /**
     * 文档版本VO
     */
    @Data
    public static class DocVersionVO {
        private Long id;
        private Integer version;
        private String message;
        private SimpleUserVO user;
        private LocalDateTime createTime;
    }

    /**
     * 文档评论VO
     */
    @Data
    public static class DocCommentVO {
        private Long id;
        private String content;
        private SimpleUserVO user;
        private Long parentId;
        /** 类型：1=评论 2=批注 */
        private Integer type;
        private String quote;
        private Boolean resolved;
        private LocalDateTime createTime;
    }

    /**
     * 文档动态VO
     */
    @Data
    public static class DocActivityVO {
        private Long id;
        /** 动作类型：created/updated/commented/restored 等 */
        private String action;
        private String message;
        private SimpleUserVO user;
        private LocalDateTime createTime;
    }

    /**
     * 锁信息VO
     */
    @Data
    public static class LockInfoVO {
        private String lockToken;
        private LocalDateTime lockExpireTime;
        private Boolean success;
        private String message;
        private SimpleUserVO lockedByUser;
    }

    /**
     * AI会话VO
     */
    @Data
    public static class AISessionVO {
        private Long id;
        private Long teamId;
        private Long userId;
        private String title;
        private String lastMessage;
        private Boolean isPinned;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }

    /**
     * AI消息VO
     */
    @Data
    public static class AIMessageVO {
        private Long id;
        private Long sessionId;
        private Long userId;
        /** 角色：1=user 2=assistant */
        private Integer role;
        private String content;
        private List<CitationVO> citations;
        private LocalDateTime createTime;
    }

    /**
     * AI引用VO
     */
    @Data
    public static class CitationVO {
        private Long docId;
        private String title;
        private String text;
        private String match;
    }
}
