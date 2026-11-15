package com.ldd.initialization.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Agent 受控工具请求。userId 永远由登录态决定。 */
@Data
public class AgentToolRequestDTO {
    @NotBlank(message = "Agent 工具动作不能为空")
    private String action;
    private String query;
    private Long noteId;
    private String title;
    private String description;
    private Integer priority;
    private LocalDateTime dueTime;
    private String goal;
    private String frequency;
    private Integer repeatDay;
    private Integer intervalDays;
    private LocalDate startDate;
    private LocalDate endDate;
}
