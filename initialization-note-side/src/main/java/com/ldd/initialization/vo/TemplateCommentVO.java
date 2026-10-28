package com.ldd.initialization.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模板评论视图对象
 */
@Data
public class TemplateCommentVO {

    /**
     * 评论ID
     */
    private Long id;

    /**
     * 评论来源类型（TEMPLATE/NOTE）
     */
    private String sourceType;

    /**
     * 目标ID（模板ID/笔记ID）
     */
    private Long targetId;

    /**
     * 目标标题
     */
    private String targetTitle;

    /**
     * 评论作者名称
     */
    private String author;

    /**
     * 评论作者头像
     */
    private String avatar;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 评分（1-5）
     */
    private Integer rating;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 点赞数
     */
    private Integer likes;
}
