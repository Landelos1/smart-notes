package com.ldd.initialization.service;

import com.ldd.initialization.dto.NoteAIRequestDTO;
import reactor.core.publisher.Flux;

/**
 * 笔记AI助手服务接口
 * 
 * @author ldd
 * @date 2025-12-24
 */
public interface NoteAIService {
    
    /**
     * 处理文本的AI增强操作
     * 
     * @param userId 用户ID
     * @param request 请求参数
     * @return 流式响应
     */
    Flux<String> processText(String userId, NoteAIRequestDTO request);
}
