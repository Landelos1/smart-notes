package com.ldd.initialization.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发表模板评论DTO
 */
@Data
public class TemplateCommentCreateDTO {
    
    /**
     * 评论内容，长度1-500
     */
    @NotBlank(message = "评论内容不能为空")
    private String content;
    
    /**
     * 评分，1-5分
     */
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    private Integer rating;
}
