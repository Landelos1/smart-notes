package com.ldd.initialization.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskVO {
    private Long id;
    private Long noteId;
    private String title;
    private String description;
    private Integer status;
    private Integer priority;
    private LocalDateTime dueTime;
    private LocalDateTime completedTime;
    private String sourceType;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
