package com.ldd.initialization.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskUpdateDTO {
    @NotBlank(message = "任务标题不能为空")
    private String title;
    private String description;
    private Integer status;
    private Integer priority;
    private LocalDateTime dueTime;
}
