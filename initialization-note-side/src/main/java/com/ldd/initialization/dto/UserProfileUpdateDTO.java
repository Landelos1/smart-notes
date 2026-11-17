package com.ldd.initialization.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** 个人资料白名单。账号角色、状态、密码和认证字段不允许从此入口写入。 */
@Data
public class UserProfileUpdateDTO {
    @Pattern(regexp = "(?s).*\\S.*", message = "用户名不能为空白")
    @Size(max = 50, message = "用户名不能超过50字")
    private String username;

    @Pattern(regexp = "^1\\d{10}$", message = "请输入有效的11位手机号")
    private String phone;

    @Size(max = 2048, message = "头像地址过长")
    private String avatarUrl;

    @Size(max = 50, message = "姓名不能超过50字")
    private String realName;

    @Min(0)
    @Max(2)
    private Integer gender;

    @PastOrPresent(message = "出生日期不能晚于今天")
    private LocalDate birthDate;

    @JsonIgnore
    private boolean birthDateProvided;

    @JsonSetter("birthDate")
    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
        this.birthDateProvided = true;
    }
}
