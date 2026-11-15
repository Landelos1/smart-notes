package com.ldd.initialization.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ReviewPlanVO {
    private Long id;
    private Long noteId;
    private String title;
    private String goal;
    private String frequency;
    private Integer repeatDay;
    private Integer intervalDays;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer status;
    private String sourceType;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
