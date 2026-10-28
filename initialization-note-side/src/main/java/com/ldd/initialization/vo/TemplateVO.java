package com.ldd.initialization.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 模板视图对象
 */
@Data
public class TemplateVO {

    /**
     * 模板ID
     */
    private Long id;

    /**
     * 模板标题
     */
    private String title;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 分类
     */
    private String category;

    /**
     * 缩略图URL
     */
    private String thumbnail;

    /**
     * 模板内容（Markdown格式）
     */
    private String content;

    /**
     * 下载次数
     */
    private Integer downloads;

    /**
     * 评分（0-5）
     */
    private BigDecimal rating;

    /**
     * 浏览次数
     */
    private Integer views;

    /**
     * 作者名称
     */
    private String author;

    /**
     * 作者ID
     */
    private Long authorId;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 评论数量
     */
    private Integer commentCount;
}
