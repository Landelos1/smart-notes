package com.ldd.initialization.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单VO
 */
@Data
public class OrderVO {

    /**
     * 订单ID
     */
    private Long id;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 套餐名称
     */
    private String planName;

    /**
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 支付方式：1-微信 2-支付宝
     */
    private Integer payMethod;

    /**
     * 支付方式名称
     */
    private String payMethodName;

    /**
     * 支付时间
     */
    private Date payTime;

    /**
     * 状态：0-待支付 1-已支付 2-已取消
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 创建时间
     */
    private Date createTime;
}
