package com.ldd.initialization.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建订单DTO
 */
@Data
public class CreateOrderDTO {

    /**
     * 套餐ID
     */
    @NotNull(message = "套餐ID不能为空")
    private Long planId;
}
