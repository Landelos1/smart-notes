package com.ldd.initialization.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 团队文档表
 * 
 * @TableName t_team_doc
 */
@TableName(value = "t_team_doc")
@Data
public class TeamDoc implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "team_id")
    private Long teamId;

    private String title;

    @TableField(value = "content_md")
    private String contentMd;

    @TableField(value = "content_html")
    private String contentHtml;

    /** 状态：1=正常 0=删除/禁用 */
    private Integer status;

    @TableField(value = "is_pinned")
    private Boolean isPinned;

    private Integer version;

    @TableField(value = "locked_by")
    private Long lockedBy;

    @TableField(value = "lock_token")
    private String lockToken;

    @TableField(value = "lock_time")
    private LocalDateTime lockTime;

    @TableField(value = "lock_expire_time")
    private LocalDateTime lockExpireTime;

    @TableField(value = "created_by")
    private Long createdBy;

    @TableField(value = "updated_by")
    private Long updatedBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 锁定人信息（非数据库字段） */
    @TableField(exist = false)
    private User lockedByUser;

    /** 更新人信息（非数据库字段） */
    @TableField(exist = false)
    private User updatedByUser;

    /** 创建人信息（非数据库字段） */
    @TableField(exist = false)
    private User createdByUser;
}
