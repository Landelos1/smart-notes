package com.ldd.initialization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 举报模板DTO
 */
@Data
public class TemplateReportDTO {
    
    /**
     * 举报原因
     */
    @NotBlank(message = "举报原因不能为空")
    @Size(max = 100, message = "举报原因长度不能超过100字符")
    private String reason;
    
    /**
     * 详细描述
     */
    private String description;
}
