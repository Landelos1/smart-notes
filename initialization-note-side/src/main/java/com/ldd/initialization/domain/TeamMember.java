package com.ldd.initialization.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 团队成员表
 * 
 * @TableName t_team_member
 */
@TableName(value = "t_team_member")
@Data
public class TeamMember implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "team_id")
    private Long teamId;

    @TableField(value = "user_id")
    private Long userId;

    /** 角色：0=创建者 1=管理员 2=编辑者 3=查看者 */
    private Integer role;

    /** 状态：1=正常 0=禁用/退出 */
    private Integer status;

    @TableField(value = "join_time", fill = FieldFill.INSERT)
    private LocalDateTime joinTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 用户信息（非数据库字段） */
    @TableField(exist = false)
    private User user;

    /** 用户名（SQL关联查询结果） */
    @TableField(exist = false)
    private String username;

    /** 头像URL（SQL关联查询结果） */
    @TableField(exist = false)
    private String avatarUrl;

    /** 邮箱/手机（SQL关联查询结果） */
    @TableField(exist = false)
    private String email;
}
