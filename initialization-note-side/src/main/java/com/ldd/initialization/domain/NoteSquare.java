package com.ldd.initialization.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Note square publish metadata.
 */
@Data
@TableName("t_note_square")
public class NoteSquare {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noteId;

    private Long userId;

    private Integer status;

    private Integer recommendScore;

    @TableField("is_pinned")
    private Boolean isPinned;

    private String summary;

    private String coverUrl;

    private Integer viewCount;

    private Integer likeCount;

    private Integer favoriteCount;

    private Integer commentCount;

    private LocalDateTime publishTime;

    private LocalDateTime updateTime;
}
