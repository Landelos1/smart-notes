package com.ldd.initialization.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 笔记评论表
 *
 * @TableName t_note_comment
 */
@TableName(value = "t_note_comment")
@Data
public class NoteComment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "note_id")
    private Long noteId;

    @TableField(value = "user_id")
    private Long userId;

    @TableField(value = "parent_id")
    private Long parentId;

    @TableField(value = "reply_to_user_id")
    private Long replyToUserId;

    private String content;

    private Integer likes;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
