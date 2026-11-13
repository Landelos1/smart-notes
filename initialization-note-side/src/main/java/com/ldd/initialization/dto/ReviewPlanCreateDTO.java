package com.ldd.initialization.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReviewPlanCreateDTO {
    @NotBlank(message = "复习计划标题不能为空")
    private String title;
    private String goal;
    private Long noteId;
    private String frequency;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    private String sourceType;
}
