package com.ldd.initialization.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 会员套餐VO
 */
@Data
public class MemberPlanVO {

    /**
     * 套餐ID
     */
    private Long id;

    /**
     * 套餐名称
     */
    private String planName;

    /**
     * 有效期（月）
     */
    private Integer durationMonths;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 套餐描述
     */
    private String description;
}
