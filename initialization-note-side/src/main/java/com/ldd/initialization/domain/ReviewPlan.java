package com.ldd.initialization.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 笔记复习计划。 */
@Data
@TableName("t_review_plan")
public class ReviewPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long noteId;
    private String title;
    private String goal;
    /** daily/weekly/custom。 */
    private String frequency;
    /** ISO weekday for weekly plans: 1=Monday ... 7=Sunday. */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer repeatDay;
    /** Recurrence interval in days for custom plans. */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer intervalDays;
    private LocalDate startDate;
    private LocalDate endDate;
    /** 0进行中，1已完成，2已归档。 */
    private Integer status;
    private String sourceType;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
