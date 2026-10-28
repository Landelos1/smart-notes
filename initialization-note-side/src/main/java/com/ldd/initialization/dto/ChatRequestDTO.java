package com.ldd.initialization.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 
 */
@Data
public class ChatRequestDTO {

    /**
     * 
     */
    private String userId;
    
    @NotBlank(message = "会话ID不能为空")
    private String memoryId;
    
    @NotBlank(message = "消息内容不能为空")
    private String message;

    /**
     * 智能体标识（可选），用于选择不同的系统提示词
     */
    private String agentId;

    /**
     * 用户侧展示的原始消息内容（可选）
     */
    private String displayMessage;
    
    /**
     * 是否使用知识库增强回答
     * true: 使用用户个人知识库中的文档来增强回答
     * false: 普通对话模式，不使用知识库
     */
    private Boolean useKnowledge = true;
} 
