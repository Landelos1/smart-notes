package com.ldd.initialization.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模板评论表
 * 
 * @TableName t_template_comment
 */
@TableName(value = "t_template_comment")
@Data
public class TemplateComment implements Serializable {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模板ID
     */
    @TableField(value = "template_id")
    private Long templateId;

    /**
     * 评论用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

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
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 点赞数
     */
    private Integer likes;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
