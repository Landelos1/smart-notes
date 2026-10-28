package com.ldd.initialization.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 会员套餐表
 */
@TableName(value = "t_member_plan")
@Data
public class MemberPlan implements Serializable {

    /**
     * 套餐ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 套餐名称
     */
    @TableField("plan_name")
    private String planName;

    /**
     * 有效期（月）
     */
    @TableField("duration_months")
    private Integer durationMonths;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 套餐描述
     */
    private String description;

    /**
     * 状态：0-下架 1-上架
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
