package com.ldd.initialization.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 管理员创建用户 DTO
 */
@Data
public class AdminUserCreateDTO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^\\d{11}$", message = "手机号必须为11位数字")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^.{6,20}$", message = "密码长度必须在6-20位")
    private String password;

    @NotNull(message = "用户角色不能为空")
    @Min(value = 1, message = "角色类型最小为1")
    @Max(value = 2, message = "角色类型最大为2")
    private Integer roleType;

    private String username;

    private String avatarUrl;

    private String realName;

    /**
     * 性别 0-未设置 1-男 2-女
     */
    private Integer gender;

    /**
     * 状态 0-禁用 1-启用 2-待审核
     */
    private Integer status;
}
