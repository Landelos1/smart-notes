package com.ldd.initialization.service.impl;

import com.ldd.initialization.dto.NoteAIRequestDTO;
import com.ldd.initialization.enums.NoteAIAction;
import com.ldd.initialization.service.NoteAIService;
import com.ldd.initialization.service.ai.adapter.NoteAIConsultantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * 笔记AI助手服务实现类
 * 
 * @author ldd
 * @date 2025-12-24
 */
@Service
@Slf4j
public class NoteAIServiceImpl implements NoteAIService {
    
    @Autowired
    private NoteAIConsultantService noteAIConsultantService;
    
    // 文本长度限制常量
    private static final int MAX_TEXT_LENGTH = 10000;
    private static final int MAX_SELECTED_TEXT_LENGTH = 5000;
    
    @Override
    public Flux<String> processText(String userId, NoteAIRequestDTO request) {
        try {
            // 1. 参数验证
            validateRequest(request);
            
            // 2. 获取待处理文本
            String textToProcess = getTextToProcess(request);
            int textLength = textToProcess != null ? textToProcess.length() : 0;
            
            log.info("[NoteAI] 用户{} 执行{}操作，文本长度: {}", userId, request.getAction(), textLength);
            
            // 3. 生成独立的会话ID
            String memoryId = userId + "_note_ai_" + UUID.randomUUID().toString();
            
            // 4. 构建用户消息
            String userMessage = buildUserMessage(request);
            
            // 5. 调用AI服务（根据操作类型调用不同的方法）
            long startTime = System.currentTimeMillis();
            Flux<String> resultFlux = callAIService(request.getAction(), memoryId, userMessage);
            
            return resultFlux
                .doOnComplete(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("[NoteAI] 用户{} AI处理完成，耗时: {}ms", userId, duration);
                })
                .doOnError(error -> {
                    log.error("[NoteAI] 用户{} AI处理失败: {}", userId, error.getMessage(), error);
                });
                
        } catch (Exception e) {
            log.error("[NoteAI] 处理请求失败: {}", e.getMessage(), e);
            return Flux.error(e);
        }
    }
    
    /**
     * 验证请求参数
     */
    private void validateRequest(NoteAIRequestDTO request) {
        // 验证操作类型
        if (!NoteAIAction.isValid(request.getAction())) {
            throw new IllegalArgumentException("不支持的操作类型: " + request.getAction());
        }
        
        // 验证文本内容
        if ((request.getSelectedText() == null || request.getSelectedText().isEmpty()) &&
            (request.getFullContent() == null || request.getFullContent().isEmpty())) {
            throw new IllegalArgumentException("请提供待处理的文本内容");
        }
        
        // 验证自定义指令
        if ("custom".equals(request.getAction()) && 
            (request.getCustomPrompt() == null || request.getCustomPrompt().isEmpty())) {
            throw new IllegalArgumentException("自定义指令不能为空");
        }
        
        // 验证文本长度
        if (request.getSelectedText() != null && request.getSelectedText().length() > MAX_SELECTED_TEXT_LENGTH) {
            throw new IllegalArgumentException("选中文本长度超出限制（最大5000字符）");
        }
        
        if (request.getFullContent() != null && request.getFullContent().length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("全文内容长度超出限制（最大10000字符）");
        }
    }
    
    /**
     * 获取待处理的文本
     */
    private String getTextToProcess(NoteAIRequestDTO request) {
        if (request.getSelectedText() != null && !request.getSelectedText().isEmpty()) {
            return request.getSelectedText();
        }
        return request.getFullContent();
    }
    
    /**
     * 构建用户消息
     */
    private String buildUserMessage(NoteAIRequestDTO request) {
        String action = request.getAction();
        
        // 翻译操作
        if ("translate".equals(action)) {
            String targetLang = request.getTargetLanguage();
            if (targetLang == null || targetLang.isEmpty()) {
                targetLang = "英语";
            }
            return "请将以下内容翻译为" + targetLang + "：\n\n" + getTextToProcess(request);
        }
        
        // 自定义指令操作
        if ("custom".equals(action)) {
            return "指令：" + request.getCustomPrompt() + "\n\n待处理文本：\n" + getTextToProcess(request);
        }
        
        // 其他操作：如果有选中文本和全文，提供上下文
        if (request.getSelectedText() != null && !request.getSelectedText().isEmpty() &&
            request.getFullContent() != null && !request.getFullContent().isEmpty() &&
            !request.getSelectedText().equals(request.getFullContent())) {
            return "选中部分：\n" + request.getSelectedText() + 
                   "\n\n完整上下文：\n" + request.getFullContent();
        }
        
        // 默认：仅返回待处理文本
        return getTextToProcess(request);
    }
    
    /**
     * 根据操作类型调用不同的AI服务方法
     */
    private Flux<String> callAIService(String action, String memoryId, String userMessage) {
        NoteAIAction actionEnum = NoteAIAction.fromCode(action);
        if (actionEnum == null) {
            return Flux.error(new IllegalArgumentException("不支持的操作类型: " + action));
        }
        
        return switch (actionEnum) {
            case CONTINUE -> noteAIConsultantService.continueWriting(memoryId, userMessage);
            case OPTIMIZE -> noteAIConsultantService.optimize(memoryId, userMessage);
            case SUMMARIZE -> noteAIConsultantService.summarize(memoryId, userMessage);
            case EXPAND -> noteAIConsultantService.expand(memoryId, userMessage);
            case TRANSLATE -> noteAIConsultantService.translate(memoryId, userMessage);
            case LIST -> noteAIConsultantService.toList(memoryId, userMessage);
            case CUSTOM -> noteAIConsultantService.customProcess(memoryId, userMessage);
        };
    }
}
