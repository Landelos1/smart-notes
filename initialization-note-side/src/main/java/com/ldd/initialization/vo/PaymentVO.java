package com.ldd.initialization.vo;

import lombok.Data;

/**
 * 支付响应VO
 */
@Data
public class PaymentVO {

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 支付表单HTML（用于跳转支付宝页面）
     */
    private String payForm;

    /**
     * 订单金额
     */
    private String amount;

    /**
     * 套餐名称
     */
    private String planName;
}
