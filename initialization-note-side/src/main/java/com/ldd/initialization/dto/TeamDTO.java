package com.ldd.initialization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 团队相关DTO集合
 */
public class TeamDTO {

    /**
     * 创建团队DTO
     */
    @Data
    public static class TeamCreateDTO {
        @NotBlank(message = "团队名称不能为空")
        @Size(max = 128, message = "团队名称不能超过128个字符")
        private String name;

        @Size(max = 500, message = "团队描述不能超过500个字符")
        private String description;

        private String avatarUrl;

        private Boolean isPublic = false;
    }

    /**
     * 加入团队DTO
     */
    @Data
    public static class TeamJoinDTO {
        @NotBlank(message = "邀请码不能为空")
        private String inviteCode;
    }

    @Data
    public static class TeamUpdateDTO {
        @Size(max = 128, message = "团队名称不能超过128个字符")
        private String name;

        @Size(max = 500, message = "团队描述不能超过500个字符")
        private String description;

        private String avatarUrl;

        private Boolean isPublic;

        private Integer inviteRole;

        private java.time.LocalDateTime inviteExpireTime;

        private Integer inviteMaxUses;
    }

    /**
     * 修改成员角色DTO
     */
    @Data
    public static class MemberRoleDTO {
        @NotNull(message = "角色不能为空")
        private Integer role;
    }

    /**
     * 创建文档DTO
     */
    @Data
    public static class DocCreateDTO {
        @NotBlank(message = "文档标题不能为空")
        @Size(max = 255, message = "文档标题不能超过255个字符")
        private String title;

        private String contentMd;
        
        private String contentHtml;
        
        private Boolean isPinned = false;
    }

    /**
     * 更新文档DTO
     */
    @Data
    public static class DocUpdateDTO {
        @Size(max = 255, message = "文档标题不能超过255个字符")
        private String title;

        private String contentMd;
        
        private String contentHtml;
        
        private Boolean isPinned;

        /** 版本说明 */
        private String message;
    }

    /**
     * 恢复版本DTO
     */
    @Data
    public static class DocRestoreDTO {
        @NotNull(message = "版本ID不能为空")
        private Long eventId;
    }

    /**
     * 添加评论DTO
     */
    @Data
    public static class CommentCreateDTO {
        @NotBlank(message = "评论内容不能为空")
        private String content;

        /** 父评论ID（回复） */
        private Long parentId;

        /** 引用文本（批注） */
        private String quote;

        /** 类型：1=评论 2=批注 */
        private Integer type = 1;
    }

    /**
     * 创建AI会话DTO
     */
    @Data
    public static class AISessionCreateDTO {
        @Size(max = 255, message = "会话标题不能超过255个字符")
        private String title;
    }

    /**
     * 发送AI消息DTO
     */
    @Data
    public static class AIMessageDTO {
        @NotBlank(message = "消息内容不能为空")
        private String content;
    }
}
