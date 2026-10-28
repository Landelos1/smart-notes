package com.ldd.initialization.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.ldd.initialization.dto.NoteAIRequestDTO;
import com.ldd.initialization.service.NoteAIService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * 笔记AI助手控制器
 * 处理笔记编辑页的AI文本增强请求
 * 
 * @author ldd
 * @date 2025-12-24
 */
@RestController
@RequestMapping("/api/notes/ai")
@Slf4j
public class NoteAIController {
    
    @Autowired
    private NoteAIService noteAIService;
    
    @Autowired
    @Qualifier("streamingExecutor")
    private ThreadPoolTaskExecutor streamingExecutor;
    
    /**
     * 处理笔记文本的AI增强操作
     * 支持续写、优化、总结、扩展、翻译、转列表、自定义指令等操作
     * 
     * @param request 请求参数
     * @return SSE流式响应
     */
    @SaCheckLogin
    @PostMapping(value = "/process", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> processText(@Valid @RequestBody NoteAIRequestDTO request) {
        // 获取当前登录用户ID
        String userId = StpUtil.getLoginId().toString();
        
        log.info("[NoteAI] 用户 {} 发起AI文本处理请求，操作类型: {}", userId, request.getAction());
        
        try {
            return noteAIService.processText(userId, request)
                .subscribeOn(Schedulers.fromExecutor(streamingExecutor))
                // 换行符编码，防止SSE传输中丢失
                .map(chunk -> chunk.replace("\n", "[NL]"))
                .doOnNext(chunk -> log.debug("[NoteAI] 发送数据块: {}", 
                    chunk.length() > 100 ? chunk.substring(0, 100) + "..." : chunk))
                .doOnComplete(() -> log.info("[NoteAI] 用户 {} 的AI文本处理完成", userId))
                .doOnError(error -> log.error("[NoteAI] 用户 {} 的AI文本处理出错: {}", 
                    userId, error.getMessage()))
                .onErrorResume(throwable -> {
                    log.error("[NoteAI] 处理失败: {}", throwable.getMessage(), throwable);
                    
                    // 根据异常类型返回不同的错误提示
                    if (throwable instanceof IllegalArgumentException) {
                        return Flux.just("错误：" + throwable.getMessage());
                    }
                    return Flux.just("抱歉，AI服务暂时不可用，请稍后再试");
                });
                
        } catch (Exception e) {
            log.error("[NoteAI] 处理请求时发生异常: {}", e.getMessage(), e);
            return Flux.just("AI服务暂时不可用，请稍后再试");
        }
    }
}
