package com.ldd.initialization.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 笔记AI助手请求DTO
 * 
 * @author ldd
 * @date 2025-12-24
 */
@Data
public class NoteAIRequestDTO {
    
    /**
     * 操作类型：continue/optimize/summarize/expand/translate/list/custom
     */
    @NotBlank(message = "操作类型不能为空")
    private String action;
    
    /**
     * 用户选中的文本
     */
    private String selectedText;
    
    /**
     * 笔记全文内容
     */
    private String fullContent;
    
    /**
     * 自定义指令（action=custom时必填）
     */
    private String customPrompt;
    
    /**
     * 目标语言（action=translate时可选）
     */
    private String targetLanguage;
}
