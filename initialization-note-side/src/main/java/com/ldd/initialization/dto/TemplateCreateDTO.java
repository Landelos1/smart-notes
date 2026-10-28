package com.ldd.initialization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建/更新模板请求DTO
 */
@Data
public class TemplateCreateDTO {

    /**
     * 模板ID（更新时使用）
     */
    private Long id;

    /**
     * 模板标题
     */
    @NotBlank(message = "模板标题不能为空")
    @Size(max = 255, message = "标题长度不能超过255个字符")
    private String title;

    /**
     * 模板描述
     */
    @Size(max = 2000, message = "描述长度不能超过2000个字符")
    private String description;

    /**
     * 分类
     */
    @NotBlank(message = "分类不能为空")
    private String category;

    /**
     * 缩略图URL
     */
    @Size(max = 500, message = "缩略图URL长度不能超过500个字符")
    private String thumbnail;

    /**
     * 模板内容（Markdown格式）
     */
    private String content;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 是否公开
     */
    private Boolean isPublic = true;
}
