package com.ldd.initialization.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 响应邀请DTO
 */
@Data
public class InvitationRespondDTO {

    /**
     * 是否接受邀请（true=接受，false=拒绝）
     */
    @NotNull(message = "请选择接受或拒绝")
    private Boolean accept;
}
