package com.ldd.initialization.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskCreateDTO {
    @NotBlank(message = "任务标题不能为空")
    private String title;
    private String description;
    private Long noteId;
    private Integer priority;
    @JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm[:ss]][ HH:mm[:ss]]")
    private LocalDateTime dueTime;
    private String sourceType;
}
