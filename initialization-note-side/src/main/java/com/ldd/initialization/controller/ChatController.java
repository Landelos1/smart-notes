package com.ldd.initialization.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.ldd.initialization.config.ChatStreamConfig;
import com.ldd.initialization.domain.UserAISettings;
import com.ldd.initialization.dto.ChatRequestDTO;
import com.ldd.initialization.service.ChatHistoryService;
import com.ldd.initialization.service.UserAISettingsService;
import com.ldd.initialization.service.ai.adapter.ConsultantService;
import com.ldd.initialization.service.ai.UserConsultantService;
import com.ldd.initialization.service.ai.adapter.DynamicChatDispatcher;
import com.ldd.initialization.vo.ChatCitationVO;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import com.ldd.initialization.config.exception.BizException;
import com.ldd.initialization.result.BizResponseCode;
import com.ldd.initialization.utils.TokenEstimateUtil;
import com.ldd.initialization.result.Result;
import com.ldd.initialization.service.TokenStatsService;
import com.ldd.initialization.service.RequestStatsService;
import com.ldd.initialization.vo.TokenStatsVO;

@RestController
@RequestMapping("/api/chat")
@Slf4j
public class ChatController {
    
    @Autowired
    private ConsultantService consultantService;
    
    @Autowired
    private UserConsultantService userConsultantService;

    @Autowired
    @Qualifier("streamingExecutor")
    private ThreadPoolTaskExecutor streamingExecutor;

    @Autowired
    private ChatStreamConfig chatStreamConfig;

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Autowired
    private TokenStatsService tokenStatsService;

    @Autowired
    private RequestStatsService requestStatsService;

    @Autowired
    private UserAISettingsService userAISettingsService;

    @Autowired
    private ChatMemoryStore redisChatMemoryStore;

    /** 自定义 Key 分支的会话记忆滑动窗口大小，需与 CommonConfig 中保持一致 */
    private static final int CUSTOM_KEY_MEMORY_WINDOW = 20;

    @PostConstruct
    public void init() {
        // 验证配置参数
        chatStreamConfig.validate();
        log.info("流式聊天速率控制配置已加载: {}", chatStreamConfig);
    }

    /**
     * 获取当前用户 token 统计
     */
    @SaCheckLogin
    @GetMapping("/token-stats")
    public Result<TokenStatsVO> getTokenStats() {
        TokenStatsVO stats = tokenStatsService.getGlobalTokenStats();
        return Result.success(stats);
    }

    /**
     * 获取今日全用户请求次数（按用户消息统计）
     */
    @SaCheckLogin
    @GetMapping("/request-count/today")
    public Result<Long> getTodayRequestCount() {
        return Result.success(requestStatsService.getTodayRequestCount());
    }

    /**
     * 流式聊天接口 - 带速率控制
     * 支持通过 useKnowledge 参数控制是否使用知识库增强
     */
    @SaCheckLogin
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@Valid @RequestBody ChatRequestDTO request) {
        // 获取当前登录用户ID
        Long currentUserId = StpUtil.getLoginIdAsLong();
        request.setUserId(currentUserId.toString());
        Long sessionId;
        try {
            sessionId = Long.valueOf(request.getMemoryId());
        } catch (NumberFormatException ex) {
            throw new BizException(BizResponseCode.ERROR_1, "会话ID格式不正确");
        }
        
        // 读取用户 AI 设置
        UserAISettings aiSettings = userAISettingsService.getOrInit(currentUserId);

        // 请求参数中的 useKnowledge 优先级高于用户设置，都未指定时按用户的 enablePersonalKb
        boolean useKnowledge;
        if (request.getUseKnowledge() != null) {
            useKnowledge = request.getUseKnowledge();
        } else {
            useKnowledge = Boolean.TRUE.equals(aiSettings.getEnablePersonalKb());
        }

        log.info("用户 {} 发起流式聊天请求，会话ID: {}, 使用知识库: {}, agentId: {}, useCustomKey: {}",
                request.getUserId(), request.getMemoryId(), useKnowledge, request.getAgentId(),
                aiSettings.getUseCustomKey());

        try {
            // 构建组合的会话ID，确保用户隔离
            String combinedMemoryId = request.getUserId() + "_" + request.getMemoryId();

            String displayMessage = request.getDisplayMessage() != null ? request.getDisplayMessage() : request.getMessage();
            chatHistoryService.saveUserMessage(sessionId, currentUserId, displayMessage, 0);

            // 根据 useKnowledge 和用户设置决定消息增强方式
            int maxDocs = aiSettings.getPersonalKbMaxDocs() != null ? aiSettings.getPersonalKbMaxDocs() : 10;
            int chatStyle = aiSettings.getChatStyle() != null ? aiSettings.getChatStyle() : 2;
            int replyLanguage = aiSettings.getReplyLanguage() != null ? aiSettings.getReplyLanguage() : 1;
            String customPrompt = aiSettings.getCustomSystemPrompt();

            String finalMessage;
            List<ChatCitationVO> citations = Collections.emptyList();
            if (useKnowledge) {
                UserConsultantService.EnhancedChatPayload payload = userConsultantService.buildEnhancedPayload(
                    request.getUserId(),
                    request.getMessage(),
                    request.getAgentId(),
                    maxDocs,
                    customPrompt,
                    chatStyle,
                    replyLanguage
                );
                finalMessage = payload.getEnhancedMessage();
                citations = payload.getCitations() != null ? payload.getCitations() : Collections.emptyList();
            } else {
                finalMessage = request.getMessage();
            }
            final List<ChatCitationVO> finalCitations = citations;

            // 调用流式聊天服务，添加速率控制
            StringBuilder assistantContent = new StringBuilder();
            final int promptTokens = TokenEstimateUtil.estimateTokens(finalMessage);

            // 选择模型：优先 Coze agent，其次用户自定义 key，最后全局默认模型
            Flux<String> responseFlux;
            if (userConsultantService.isCozeAgentConfigured(request.getAgentId())) {
                // Coze 智能体自带系统提示词；若开启本地知识库增强，则附带"精简版"参考资料（不含本地系统提示词）
                String cozeMessage = useKnowledge
                        ? userConsultantService.buildKnowledgeContextOnly(request.getUserId(), request.getMessage(), maxDocs)
                        : request.getMessage();
                responseFlux = userConsultantService.streamChatWithCoze(request.getAgentId(), cozeMessage);
            } else if (Boolean.TRUE.equals(aiSettings.getUseCustomKey())
                    && StringUtils.hasText(aiSettings.getChatApiKey())
                    && StringUtils.hasText(aiSettings.getChatApiEndpoint())) {
                responseFlux = streamChatWithUserModel(aiSettings, combinedMemoryId, finalMessage, request.getMessage());
            } else {
                responseFlux = consultantService.streamChat(combinedMemoryId, finalMessage);
            }

            return responseFlux
                .subscribeOn(Schedulers.fromExecutor(streamingExecutor)) // 使用自定义执行器
                .doOnNext(assistantContent::append)
                .map(chunk -> chunk.replace("\n", "[NL]")) // 将换行符编码为标记，防止SSE传输中丢失
                .transform(this::applyRateControl) // 应用速率控制
                .doOnNext(chunk -> log.debug("发送聊天块: {}", chunk.length() > 100 ? chunk.substring(0, 100) + "..." : chunk))
                .doOnComplete(() -> log.info("用户 {} 的流式聊天完成", request.getUserId()))
                .doOnComplete(() -> {
                    int completionTokens = TokenEstimateUtil.estimateTokens(assistantContent.toString());
                    int totalTokens = promptTokens + completionTokens;
                    chatHistoryService.saveAssistantMessage(
                        sessionId,
                        currentUserId,
                        assistantContent.toString(),
                        totalTokens,
                        finalCitations
                    );
                })
                .doOnError(error -> log.error("用户 {} 的流式聊天出错: {}", request.getUserId(), error.getMessage()))
                .onErrorResume(throwable -> {
                    log.error("流式聊天处理失败: {}", throwable.getMessage(), throwable);
                    return Flux.just("抱歉，聊天服务暂时不可用，请稍后再试。");
                });
                
        } catch (Exception e) {
            log.error("流式聊天处理失败: {}", e.getMessage(), e);
            return Flux.just("聊天服务暂时不可用，请稍后再试。");
        }
    }

    /**
     * 使用用户自定义 API Key/端点/模型 进行流式聊天
     * 直接调用 OpenAI 兼容接口，返回 Flux<String>
     */
    private Flux<String> streamChatWithUserModel(UserAISettings aiSettings, String memoryId, String message, String plainUserMessage) {
        String baseUrl = buildBaseUrl(aiSettings.getChatApiEndpoint());
        String modelName = StringUtils.hasText(aiSettings.getChatModel()) ? aiSettings.getChatModel() : "gpt-3.5-turbo";

        log.info("[自定义模型] 用户 {} 使用自定义模型: {}, baseUrl: {}", aiSettings.getUserId(), modelName, baseUrl);

        try {
            OpenAiStreamingChatModel streamingModel = OpenAiStreamingChatModel.builder()
                    .baseUrl(baseUrl)
                    .apiKey(aiSettings.getChatApiKey())
                    .modelName(modelName)
                    .build();

            // 从 Redis 加载历史会话消息，保证自定义 Key 分支也具备上下文记忆
            List<ChatMessage> history;
            try {
                history = new ArrayList<>(
                        Optional.ofNullable(redisChatMemoryStore.getMessages(memoryId))
                                .orElse(Collections.emptyList()));
            } catch (Exception e) {
                log.warn("[自定义模型] 加载 Redis 会话记忆失败，按无上下文处理: {}", e.getMessage());
                history = new ArrayList<>();
            }
            // 构造发送给模型的消息：历史（纯净） + 当前这一轮的增强消息
            final List<ChatMessage> pastHistory = history;
            List<ChatMessage> forModel = new ArrayList<>(pastHistory);
            forModel.add(dev.langchain4j.data.message.UserMessage.from(message));
            if (forModel.size() > CUSTOM_KEY_MEMORY_WINDOW) {
                forModel = new ArrayList<>(forModel.subList(forModel.size() - CUSTOM_KEY_MEMORY_WINDOW, forModel.size()));
            }

            final List<ChatMessage> requestMessages = forModel;
            // 用于回写 Redis 的纯净用户消息（不含 RAG 片段与系统提示词，避免污染后续上下文）
            final String cleanUserMessage = (plainUserMessage != null && !plainUserMessage.isBlank())
                    ? plainUserMessage : message;
            final StringBuilder fullReply = new StringBuilder();

            return Flux.create(sink -> {
                try {
                    streamingModel.generate(
                            requestMessages,
                            new dev.langchain4j.model.StreamingResponseHandler<AiMessage>() {
                                @Override
                                public void onNext(String token) {
                                    fullReply.append(token);
                                    sink.next(token);
                                }

                                @Override
                                public void onComplete(dev.langchain4j.model.output.Response<AiMessage> response) {
                                    // 将本轮对话以"纯净"形式写回 Redis：不保留 RAG 片段/系统提示词
                                    try {
                                        List<ChatMessage> updated = new ArrayList<>(pastHistory);
                                        updated.add(dev.langchain4j.data.message.UserMessage.from(cleanUserMessage));
                                        String replyText = response != null && response.content() != null
                                                ? response.content().text()
                                                : fullReply.toString();
                                        if (replyText == null || replyText.isEmpty()) {
                                            replyText = fullReply.toString();
                                        }
                                        updated.add(AiMessage.from(replyText));
                                        if (updated.size() > CUSTOM_KEY_MEMORY_WINDOW) {
                                            updated = new ArrayList<>(updated.subList(
                                                    updated.size() - CUSTOM_KEY_MEMORY_WINDOW, updated.size()));
                                        }
                                        redisChatMemoryStore.updateMessages(memoryId, updated);
                                    } catch (Exception ex) {
                                        log.warn("[自定义模型] 写回 Redis 会话记忆失败: {}", ex.getMessage());
                                    }
                                    sink.complete();
                                }

                                @Override
                                public void onError(Throwable error) {
                                    log.error("[自定义模型] 流式响应出错: {}", error.getMessage(), error);
                                    sink.error(error);
                                }
                            }
                    );
                } catch (Exception e) {
                    sink.error(e);
                }
            });
        } catch (Exception e) {
            log.error("[自定义模型] 创建模型失败: {}", e.getMessage(), e);
            return Flux.error(e);
        }
    }

    private String buildBaseUrl(String endpoint) {
        try {
            java.net.URI u = new java.net.URI(endpoint);
            String path = u.getPath();
            int vIdx = path.indexOf("/v");
            if (vIdx >= 0) {
                int slashAfterV = path.indexOf("/", vIdx + 1);
                String vPart = slashAfterV > 0 ? path.substring(0, slashAfterV) : path;
                return u.getScheme() + "://" + u.getAuthority() + vPart;
            }
            return u.getScheme() + "://" + u.getAuthority() + "/v1";
        } catch (Exception e) {
            if (endpoint.contains("/v1/")) {
                return endpoint.split("/v1/")[0] + "/v1";
            }
            return endpoint.replaceAll("/chat/completions$", "");
        }
    }

    /**
     * 应用速率控制的转换器
     * 使用多种策略来平滑响应速度：
     * 1. 基础延迟 - 确保最小间隔
     * 2. 自适应延迟 - 根据内容长度调整
     * 3. 平滑处理 - 避免突发性传输
     */
    private Flux<String> applyRateControl(Flux<String> source) {
        // 如果全局禁用速率控制，直接返回原始流
        if (!chatStreamConfig.isEnableRateControl()) {
            return source;
        }
        
        AtomicInteger chunkCounter = new AtomicInteger(0);
        
        Flux<String> processedFlux = source
            // 1. 基础延迟控制 - 确保每个块之间有最小间隔
            .delayElements(chatStreamConfig.getBaseDelay(), Schedulers.parallel());
        
        // 2. 自适应延迟 - 根据内容长度和频率动态调整
        if (chatStreamConfig.isEnableAdaptiveDelay()) {
            processedFlux = processedFlux.concatMap(chunk -> {
                int chunkIndex = chunkCounter.incrementAndGet();
                Duration adaptiveDelay = calculateAdaptiveDelay(chunk, chunkIndex);
                
                if (chatStreamConfig.isEnableLogging()) {
                    log.debug("块 {} 长度: {}, 延迟: {}ms", chunkIndex, chunk.length(), adaptiveDelay.toMillis());
                }
                
                return Flux.just(chunk)
                    .delayElements(adaptiveDelay, Schedulers.parallel());
            });
        }
        
        // 3. 平滑处理 - 使用滑动窗口平滑传输
        if (chatStreamConfig.isEnableSmoothTransmission()) {
            processedFlux = processedFlux
                .window(chatStreamConfig.getRateLimitWindow())
                .concatMap(window -> 
                    window.concatMap(chunk -> 
                        Flux.just(chunk)
                            .delayElements(chatStreamConfig.getWindowDelay(), Schedulers.parallel())
                    )
                );
        }
        
        // 4. 缓冲控制 - 避免过快传输
        return processedFlux.onBackpressureBuffer(chatStreamConfig.getBackpressureBufferSize());
    }

    /**
     * 计算自适应延迟
     * 根据内容长度、块索引等因素动态调整延迟时间
     */
    private Duration calculateAdaptiveDelay(String chunk, int chunkIndex) {
        // 基础延迟
        long baseDelayMs = chatStreamConfig.getBaseDelay().toMillis();
        
        // 根据内容长度调整延迟
        int contentLength = chunk.length();
        long contentDelay = 0;
        
        if (contentLength > chatStreamConfig.getChunkSizeThreshold()) {
            // 内容较长时，适当增加延迟
            contentDelay = Math.min(
                (long)(contentLength * chatStreamConfig.getContentDelayFactor()), 
                chatStreamConfig.getMaxDelay().toMillis() - baseDelayMs
            );
        }
        
        // 根据块索引调整延迟（前几个块稍微快一点，后面稳定）
        long indexDelay = 0;
        if (chunkIndex <= 5) {
            indexDelay = Math.max(0, baseDelayMs - (chunkIndex * chatStreamConfig.getIndexDelayReduction()));
        }
        
        // 计算最终延迟
        long finalDelay = Math.max(baseDelayMs, baseDelayMs + contentDelay - indexDelay);
        finalDelay = Math.min(finalDelay, chatStreamConfig.getMaxDelay().toMillis());
        
        return Duration.ofMillis(finalDelay);
    }
    @Resource
    DynamicChatDispatcher dispatcher;
    /**
     * 流式聊天接口 - 快速版本（无速率控制）
     * 提供给需要快速响应的场景使用
     */
    @SaIgnore
    @PostMapping(value = "/stream/local", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStreamFast(@RequestParam("modelName") String modelName, @RequestParam("memoryId") String memoryId,
                                @RequestParam("message") String message) {
        Flux<String> chatStream = dispatcher.streamChat(modelName,memoryId, message);
        return chatStream
            .subscribeOn(Schedulers.fromExecutor(streamingExecutor))
            .doOnNext(chunk -> log.info("发送聊天块: {}", chunk.length() > 100 ? chunk.substring(0, 100) + "..." : chunk))
            .doOnComplete(() -> log.info("流式聊天完成"))
            .doOnError(error -> log.error("流式聊天出错: {}", error.getMessage()));

    }


}
