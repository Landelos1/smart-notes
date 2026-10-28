package com.ldd.initialization.dto;

import lombok.Data;

/**
 * 管理员订单查询DTO
 */
@Data
public class AdminOrderQueryDTO {
    
    /**
     * 当前页码
     */
    private Integer current = 1;
    
    /**
     * 每页大小
     */
    private Integer size = 20;
    
    /**
     * 订单号
     */
    private String orderNo;
    
    /**
     * 套餐名称
     */
    private String planName;
    
    /**
     * 支付方式：1-微信 2-支付宝
     */
    private Integer payMethod;
    
    /**
     * 状态：0-待支付 1-已支付 2-已取消
     */
    private Integer status;
    
    /**
     * 开始时间
     */
    private String startTime;
    
    /**
     * 结束时间
     */
    private String endTime;
}
