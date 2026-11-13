package com.ldd.initialization.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户任务。 */
@Data
@TableName("t_task")
public class Task {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long noteId;
    private String title;
    private String description;
    /** 0待处理，1进行中，2已完成。 */
    private Integer status;
    /** 1低，2中，3高。 */
    private Integer priority;
    private LocalDateTime dueTime;
    private LocalDateTime completedTime;
    private String sourceType;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
