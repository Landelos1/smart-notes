package com.ldd.initialization.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 管理员更新用户 DTO（字段均可选）
 */
@Data
public class AdminUserUpdateDTO {

    private String username;

    private String avatarUrl;

    private String realName;

    /**
     * 性别 0-未设置 1-男 2-女
     */
    private Integer gender;

    /**
     * 新密码（可选）
     */
    @Pattern(regexp = "^.{0,20}$", message = "密码长度不能超过20位")
    private String password;

    /**
     * 角色类型 1-USER 2-ADMIN
     */
    @Min(value = 1, message = "角色类型最小为1")
    @Max(value = 2, message = "角色类型最大为2")
    private Integer roleType;

    /**
     * 状态 0-禁用 1-启用 2-待审核
     */
    private Integer status;

    /**
     * 会员是否有效
     */
    private Boolean memberActive;

    /**
     * 会员到期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.util.Date memberExpireTime;
}
