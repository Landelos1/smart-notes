package com.ldd.initialization.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模板表
 * 
 * @TableName t_template
 */
@TableName(value = "t_template")
@Data
public class Template implements Serializable {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
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
     * 作者用户ID
     */
    @TableField(value = "author_id")
    private Long authorId;

    /**
     * 是否公开
     */
    @TableField(value = "is_public")
    private Boolean isPublic;

    /**
     * 状态：0-草稿, 1-已发布, 2-已下架
     */
    private Integer status;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
