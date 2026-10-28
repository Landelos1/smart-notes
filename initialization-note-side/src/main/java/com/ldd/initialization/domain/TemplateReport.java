package com.ldd.initialization.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模板举报实体类
 */
@Data
@TableName("t_template_report")
public class TemplateReport {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 被举报的模板ID
     */
    private Long templateId;
    
    /**
     * 举报人ID
     */
    private Long reporterId;
    
    /**
     * 举报原因
     */
    private String reason;
    
    /**
     * 详细描述
     */
    private String description;
    
    /**
     * 处理状态（0-待处理，1-已处理，2-已驳回）
     */
    private Integer status;
    
    /**
     * 举报时间
     */
    private LocalDateTime createTime;
    
    /**
     * 处理时间
     */
    private LocalDateTime handleTime;
    
    /**
     * 处理人ID
     */
    private Long handlerId;
}
