package com.ldd.initialization.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 笔记评论视图对象
 */
@Data
public class NoteCommentVO {

    private Long id;

    private Long noteId;

    private Long userId;

    private Long parentId;

    private Long replyToUserId;

    private String content;

    private Integer likes;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String authorName;

    private String authorAvatar;

    private String replyToUserName;

    private String replyToUserAvatar;

    private Boolean isOwner;

    private List<NoteCommentVO> replies;
}
