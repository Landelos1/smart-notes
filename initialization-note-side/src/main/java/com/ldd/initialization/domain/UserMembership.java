package com.ldd.initialization.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName(value = "t_user_membership")
@Data
public class UserMembership implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("expire_time")
    private Date expireTime;

    @TableField("is_active")
    private Boolean isActive;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableField("ai_call_quota")
    private Integer aiCallQuota;

    @TableField("ai_call_used")
    private Integer aiCallUsed;

    @TableField("last_ai_reset_time")
    private Date lastAiResetTime;

    @TableField("kb_quota")
    private Integer kbQuota;

    @TableField("kb_used")
    private Integer kbUsed;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
