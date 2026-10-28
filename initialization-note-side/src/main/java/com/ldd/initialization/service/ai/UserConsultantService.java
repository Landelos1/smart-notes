package com.ldd.initialization.service.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ldd.initialization.config.CozeConfigProperties;
import com.ldd.initialization.domain.FileUpInfo;
import com.ldd.initialization.mapper.FileInfoMapper;
import com.ldd.initialization.service.ai.adapter.ConsultantService;
import com.ldd.initialization.vo.ChatCitationVO;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import com.ldd.initialization.dto.SharedKnowledgeBaseChatDTO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户智能对话服务
 * 提供基于个人知识库和共享知识库的AI对话功能
 */
@Service
@Slf4j
public class UserConsultantService {
    
    @Autowired
    private ConsultantService consultantService;

    
    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private SharedKnowledgeBaseVectorService sharedVectorService;
    
    private String systemPrompt;

    @Autowired
    private UserVectorService userVectorService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FileInfoMapper fileInfoMapper;

    @Autowired
    private CozeConfigProperties cozeConfigProperties;

    @Value("#{${ai.agents.prompts:{}}}")
    private Map<String, String> agentSystemPrompts;

    private final WebClient webClient = WebClient.builder().build();

    @PostConstruct
    public void logCozeConfigKeys() {
        Map<String, String> streamRunUrls = cozeConfigProperties == null ? null : cozeConfigProperties.getStreamRunUrls();
        Map<String, String> apiTokens = cozeConfigProperties == null ? null : cozeConfigProperties.getApiTokens();
        Map<String, String> projectIds = cozeConfigProperties == null ? null : cozeConfigProperties.getProjectIds();
        Map<String, String> sessionIds = cozeConfigProperties == null ? null : cozeConfigProperties.getSessionIds();
        log.info("Coze 配置加载: streamRunUrls={}, apiTokens={}, projectIds={}, sessionIds={}",
            streamRunUrls == null ? List.of() : streamRunUrls.keySet(),
            apiTokens == null ? List.of() : apiTokens.keySet(),
            projectIds == null ? List.of() : projectIds.keySet(),
            sessionIds == null ? List.of() : sessionIds.keySet());
    }



    /**
     * 设置当前用户ID
     * 
     * @param userId 用户ID
     */
    public void setCurrentUserId(String userId) {
    }
    
    /**
     * 初始化系统提示词
     */
    public void initSystemPrompt() {
        if (systemPrompt == null) {
            ClassPathResource resource = new ClassPathResource("system.txt");
            if (resource.exists()) {
                try {
                    systemPrompt = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    log.warn("读取 system.txt 失败，使用默认系统提示词: {}", e.getMessage());
                }
            } else {
                log.warn("未找到 system.txt，使用默认系统提示词");
            }
            if (systemPrompt == null) {
                systemPrompt = "你是一个智能助手，专门帮助用户基于他们的个人知识库回答问题。\n\n" +
                              "重要规则：\n" +
                              "1. 当提供了「参考资料」时，你必须优先基于这些资料回答，这些是用户上传的个人文档内容\n" +
                              "2. 如果参考资料中包含了问题的答案，直接引用和总结这些内容\n" +
                              "3. 如果参考资料与当前问题不相关，或用户的问题是关于之前的对话内容（例如“我刚刚问了什么”“上一句你说了什么”），请直接结合对话历史作答，不要以“参考资料不包含相关信息”为由拒绝回答\n" +
                              "4. 回答要简洁、准确，突出关键信息";
            }
        }
    }


    /**
     * 为指定用户从个人知识库检索内容
     *
     * @param userId 用户ID
     * @param query 查询内容
     * @return 检索到的内容列表
     */
    public List<Content> retrieveForUser(String userId, String query) {
        log.info("为用户 {} 从个人知识库检索内容，查询: {}", userId, query);

        try {
            // 搜索用户的个人知识库
            List<EmbeddingMatch<TextSegment>> matches = userVectorService.searchForUser(
                    userId,
                    query,
                    10, // 最多返回10个结果
                    0.3 // 最小相似度阈值
            );

            // 转换为Content对象
            List<Content> contents = matches.stream()
                    .map(match -> {
                        TextSegment segment = match.embedded();
                        // 在LangChain4j 1.0.1-beta6中，直接使用TextSegment创建Content
                        return Content.from(segment);
                    })
                    .collect(Collectors.toList());

            log.info("为用户 {} 从个人知识库检索到 {} 个相关内容", userId, contents.size());
            return contents;

        } catch (Exception e) {
            log.error("为用户 {} 从个人知识库检索内容时发生错误: {}", userId, e.getMessage(), e);
            return List.of(); // 返回空列表
        }
    }
    
    /**
     * 构建增强的用户消息（包含个人知识库检索）
     * 
     * @param userId 用户ID
     * @param message 用户消息
     * @return 增强后的消息
     */
    public String buildEnhancedMessage(String userId, String message) {
        return buildEnhancedMessage(userId, message, null);
    }

    /**
     * 构建增强的用户消息（包含个人知识库检索）
     * 
     * @param userId 用户ID
     * @param message 用户消息
     * @param agentId 智能体标识
     * @return 增强后的消息
     */
    public String buildEnhancedMessage(String userId, String message, String agentId) {
        return buildEnhancedMessage(userId, message, agentId, 10, null, 2, 1);
    }

    /**
     * 构建增强的用户消息（带用户AI设置参数）
     *
     * @param userId             用户ID
     * @param message            用户消息
     * @param agentId            智能体标识（可为null）
     * @param maxDocs            个人知识库最大检索文档数
     * @param customSystemPrompt 自定义系统提示词（为null/空时使用默认）
     * @param chatStyle          回答风格 1简洁 2适中 3详细
     * @param replyLanguage      回复语言 1跟随问题 2只中文 3只英文
     * @return 增强后的消息
     */
    public String buildEnhancedMessage(String userId, String message, String agentId,
                                       int maxDocs, String customSystemPrompt,
                                       int chatStyle, int replyLanguage) {
        return buildEnhancedPayload(userId, message, agentId, maxDocs, customSystemPrompt, chatStyle, replyLanguage).getEnhancedMessage();
    }

    public EnhancedChatPayload buildEnhancedPayload(String userId, String message, String agentId,
                                                    int maxDocs, String customSystemPrompt,
                                                    int chatStyle, int replyLanguage) {
        try {
            initSystemPrompt();

            String resolvedSystemPrompt = resolveSystemPrompt(agentId);

            // 优先使用用户自定义系统提示词
            if (customSystemPrompt != null && !customSystemPrompt.isBlank()) {
                resolvedSystemPrompt = customSystemPrompt;
            }

            // 拼接风格/语言指令
            resolvedSystemPrompt = resolvedSystemPrompt + buildStyleInstruction(chatStyle, replyLanguage);

            List<EmbeddingMatch<TextSegment>> matches = retrieveMatchesForUserWithLimit(userId, message, maxDocs);

            String context = buildContext(matches);
            String enhancedMessage = buildUserMessage(message, context);
            EnhancedChatPayload payload = new EnhancedChatPayload();
            payload.setEnhancedMessage(resolvedSystemPrompt + "\n\n" + enhancedMessage);
            payload.setCitations(buildCitations(matches));
            return payload;

        } catch (Exception e) {
            log.error("构建增强消息失败: {}", e.getMessage(), e);
            EnhancedChatPayload payload = new EnhancedChatPayload();
            payload.setEnhancedMessage(systemPrompt + "\n\n用户问题：" + message);
            payload.setCitations(new ArrayList<>());
            return payload;
        }
    }

    /**
     * 构建"精简版"知识库增强消息，仅包含【参考资料】+【用户问题】，
     * 不拼接本地系统提示词，避免与 Coze 等外部智能体自带 prompt 冲突。
     */
    public String buildKnowledgeContextOnly(String userId, String message, int maxDocs) {
        try {
            List<EmbeddingMatch<TextSegment>> matches = retrieveMatchesForUserWithLimit(userId, message, maxDocs);
            String context = buildContext(matches);
            if (context == null || context.isEmpty()) {
                return message;
            }
            return context + "【用户问题】\n" + message;
        } catch (Exception e) {
            log.warn("构建精简版知识库上下文失败，降级为原始消息: {}", e.getMessage());
            return message;
        }
    }

    private String buildStyleInstruction(int chatStyle, int replyLanguage) {
        StringBuilder sb = new StringBuilder();
        if (chatStyle == 1) {
            sb.append("\n回答要求：请尽量简洁，抓住关键点，避免冗余。");
        } else if (chatStyle == 3) {
            sb.append("\n回答要求：请详细展开，包含背景、步骤和原因。");
        }
        if (replyLanguage == 2) {
            sb.append("\n语言要求：请始终使用中文回答，无论问题是何种语言。");
        } else if (replyLanguage == 3) {
            sb.append("\n语言要求：请始终使用英文回答，无论问题是何种语言。");
        }
        return sb.toString();
    }

    /**
     * 带数量限制的知识库检索
     */
    public List<Content> retrieveForUserWithLimit(String userId, String query, int maxDocs) {
        log.info("为用户 {} 从个人知识库检索内容，查询: {}, maxDocs: {}", userId, query, maxDocs);
        try {
            List<EmbeddingMatch<TextSegment>> matches = retrieveMatchesForUserWithLimit(userId, query, maxDocs);
            return matches.stream()
                    .map(match -> Content.from(match.embedded()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("为用户 {} 从个人知识库检索内容时发生错误: {}", userId, e.getMessage(), e);
            return List.of();
        }
    }

    private String resolveSystemPrompt(String agentId) {
        if (agentId == null || agentId.isBlank()) {
            return systemPrompt;
        }
        if (agentSystemPrompts == null || agentSystemPrompts.isEmpty()) {
            return systemPrompt;
        }
        return agentSystemPrompts.getOrDefault(agentId, systemPrompt);
    }

    private List<EmbeddingMatch<TextSegment>> retrieveMatchesForUserWithLimit(String userId, String query, int maxDocs) {
        log.info("为用户 {} 从个人知识库检索命中，查询: {}, maxDocs: {}", userId, query, maxDocs);
        try {
            return userVectorService.searchForUser(userId, query, maxDocs, 0.3);
        } catch (Exception e) {
            log.error("为用户 {} 从个人知识库检索命中时发生错误: {}", userId, e.getMessage(), e);
            return List.of();
        }
    }

    public boolean isCozeAgentConfigured(String agentId) {
        return resolveCozeConfig(agentId) != null;
    }

    public Flux<String> streamChatWithCoze(String agentId, String message) {
        CozeConfig config = resolveCozeConfig(agentId);
        if (config == null) {
            return Flux.error(new IllegalStateException("Coze 配置缺失: " + agentId));
        }

        log.info("调用 Coze 智能体: agentId={}, streamRunUrl={}, projectId={}, sessionId={}",
            agentId, config.streamRunUrl, config.projectId, config.sessionId);

        Map<String, Object> promptContent = new HashMap<>();
        promptContent.put("text", message);

        Map<String, Object> prompt = new HashMap<>();
        prompt.put("type", "text");
        prompt.put("content", promptContent);

        Map<String, Object> query = new HashMap<>();
        query.put("prompt", List.of(prompt));

        Map<String, Object> content = new HashMap<>();
        content.put("query", query);

        Map<String, Object> payload = new HashMap<>();
        payload.put("content", content);
        payload.put("type", "query");
        payload.put("session_id", config.sessionId);
        payload.put("project_id", config.projectId);

        log.info("[Coze] 请求 agentId={}, 消息长度={} 字符", agentId, message == null ? 0 : message.length());

        return webClient.post()
            .uri(config.streamRunUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + config.apiToken)
            .bodyValue(payload)
            .retrieve()
            .bodyToFlux(String.class)
            .doOnNext(raw -> {
                if (log.isDebugEnabled()) {
                    log.debug("[Coze] raw chunk: {}", raw);
                }
            })
            .flatMapIterable(this::extractCozeAnswerChunks)
            .onErrorResume(error -> {
                log.error("Coze 流式请求失败: {}", error.getMessage(), error);
                return Flux.just("抱歉，Coze 服务暂时不可用，请稍后再试。");
            });
    }

    private List<String> extractCozeAnswerChunks(String rawChunk) {
        if (rawChunk == null || rawChunk.isEmpty()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        boolean sawLifecycleEvent = false;

        String[] lines = rawChunk.split("\\R");
        for (String line : lines) {
            String payload = line;
            if (payload.isEmpty()) {
                continue;
            }
            if (payload.startsWith("data:")) {
                payload = payload.substring(5);
                if (payload.isEmpty() || "[DONE]".equals(payload.trim())) {
                    continue;
                }
            }

            List<String> jsonParts = new ArrayList<>();
            if (payload.contains("}{")) {
                String[] parts = payload.split("}\\s*\\{");
                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i].trim();
                    if (part.isEmpty()) {
                        continue;
                    }
                    String json = part;
                    if (i == 0 && !part.startsWith("{")) {
                        json = "{" + part;
                    } else if (i != 0) {
                        json = "{" + part;
                    }
                    if (i == parts.length - 1 && !part.endsWith("}")) {
                        json = json + "}";
                    } else if (i != parts.length - 1) {
                        json = json + "}";
                    }
                    jsonParts.add(json);
                }
            } else {
                jsonParts.add(payload);
            }

            for (String json : jsonParts) {
                try {
                    Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
                    });
                    Object type = map.get("type");
                    // Coze 生命周期事件，没有回答文本，静默跳过
                    if ("message_start".equals(type) || "message_end".equals(type)
                            || "tool_request".equals(type) || "tool_response".equals(type)
                            || "thinking".equals(type)) {
                        sawLifecycleEvent = true;
                        log.debug("[Coze] 跳过生命周期事件 type={}", type);
                        continue;
                    }
                    // 兼容多种回答事件类型
                    boolean isAnswerEvent = type == null
                            || "answer".equals(type)
                            || "message".equals(type)
                            || "message_delta".equals(type)
                            || "delta".equals(type)
                            || "text".equals(type);
                    if (!isAnswerEvent) {
                        log.debug("[Coze] 忽略未知事件 type={}", type);
                        continue;
                    }
                    String text = extractAnswerText(map);
                    if (text != null && !text.isEmpty()) {
                        chunks.add(text);
                    } else {
                        // answer 事件但文本为空（通常是 finish=true 的收尾事件），不算异常
                        sawLifecycleEvent = true;
                    }
                } catch (Exception e) {
                    log.debug("解析 Coze 响应片段失败: {}, raw={}", e.getMessage(), json);
                }
            }
        }

        if (chunks.isEmpty() && !sawLifecycleEvent) {
            // 真正没有任何可识别事件时才告警，便于定位接口协议变化
            String preview = rawChunk.length() > 500 ? rawChunk.substring(0, 500) + "..." : rawChunk;
            log.warn("[Coze] 未从响应中解析出任何回答片段，raw chunk preview: {}", preview);
        }

        return chunks;
    }

    /**
     * 兼容多种 Coze 响应结构：content.answer / content.text / delta / text / message.content 等
     */
    @SuppressWarnings("unchecked")
    private String extractAnswerText(Map<String, Object> map) {
        // 1) content.answer / content.text
        Object content = map.get("content");
        if (content instanceof Map) {
            Map<String, Object> contentMap = (Map<String, Object>) content;
            Object v = contentMap.get("answer");
            if (v instanceof String && !((String) v).isEmpty()) return (String) v;
            v = contentMap.get("text");
            if (v instanceof String && !((String) v).isEmpty()) return (String) v;
        } else if (content instanceof String && !((String) content).isEmpty()) {
            return (String) content;
        }
        // 2) delta / text 字段
        Object v = map.get("delta");
        if (v instanceof String && !((String) v).isEmpty()) return (String) v;
        v = map.get("text");
        if (v instanceof String && !((String) v).isEmpty()) return (String) v;
        // 3) message.content
        Object msg = map.get("message");
        if (msg instanceof Map) {
            Object c = ((Map<String, Object>) msg).get("content");
            if (c instanceof String && !((String) c).isEmpty()) return (String) c;
        }
        return null;
    }

    private CozeConfig resolveCozeConfig(String agentId) {
        if (agentId == null || agentId.isBlank()) {
            return null;
        }
        Map<String, String> streamRunUrls = cozeConfigProperties == null || cozeConfigProperties.getStreamRunUrls() == null
            ? Map.of()
            : cozeConfigProperties.getStreamRunUrls();
        Map<String, String> apiTokens = cozeConfigProperties == null || cozeConfigProperties.getApiTokens() == null
            ? Map.of()
            : cozeConfigProperties.getApiTokens();
        Map<String, String> projectIds = cozeConfigProperties == null || cozeConfigProperties.getProjectIds() == null
            ? Map.of()
            : cozeConfigProperties.getProjectIds();
        Map<String, String> sessionIds = cozeConfigProperties == null || cozeConfigProperties.getSessionIds() == null
            ? Map.of()
            : cozeConfigProperties.getSessionIds();

        String defaultStreamRunUrl = cozeConfigProperties == null
            ? null
            : cozeConfigProperties.getDefaultStreamRunUrl();
        String streamRunUrl = streamRunUrls.getOrDefault(agentId, defaultStreamRunUrl);
        String apiToken = apiTokens.get(agentId);
        String projectId = projectIds.get(agentId);
        String sessionId = sessionIds.get(agentId);
        if (streamRunUrl == null || streamRunUrl.isBlank()
                || apiToken == null || apiToken.isBlank()
                || projectId == null || projectId.isBlank()
                || sessionId == null || sessionId.isBlank()) {
            StringBuilder missing = new StringBuilder();
            if (streamRunUrl == null || streamRunUrl.isBlank()) {
                missing.append("streamRunUrl");
            }
            if (apiToken == null || apiToken.isBlank()) {
                if (!missing.isEmpty()) {
                    missing.append(", ");
                }
                missing.append("apiToken");
            }
            if (projectId == null || projectId.isBlank()) {
                if (!missing.isEmpty()) {
                    missing.append(", ");
                }
                missing.append("projectId");
            }
            if (sessionId == null || sessionId.isBlank()) {
                if (!missing.isEmpty()) {
                    missing.append(", ");
                }
                missing.append("sessionId");
            }
            log.warn("Coze 配置缺失: agentId={}, missing={}", agentId, missing);
            return null;
        }
        return new CozeConfig(streamRunUrl, apiToken, projectId, sessionId);
    }

    private static class CozeConfig {
        private final String streamRunUrl;
        private final String apiToken;
        private final String projectId;
        private final String sessionId;

        private CozeConfig(String streamRunUrl, String apiToken, String projectId, String sessionId) {
            this.streamRunUrl = streamRunUrl;
            this.apiToken = apiToken;
            this.projectId = projectId;
            this.sessionId = sessionId;
        }
    }


    /**
     * 构建上下文信息
     */
    private String buildContext(List<EmbeddingMatch<TextSegment>> matches) {
        if (matches == null || matches.isEmpty()) {
            return "";
        }
        
        String context = matches.stream()
                .map(EmbeddingMatch::embedded)
                .filter(Objects::nonNull)
                .map(segment -> {
                    try {
                        return segment.text();
                    } catch (Exception e) {
                        return segment.toString();
                    }
                })
                .filter(Objects::nonNull)
                .filter(text -> !text.isBlank())
                .limit(5) // 限制上下文长度
                .collect(Collectors.joining("\n---\n"));
        
        return "【来自你的个人知识库的参考资料】\n以下是从你上传的文档中检索到的相关内容，请优先基于这些内容回答：\n\n" + context + "\n\n";
    }

    private List<ChatCitationVO> buildCitations(List<EmbeddingMatch<TextSegment>> matches) {
        if (matches == null || matches.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, ChatCitationVO> dedup = new LinkedHashMap<>();
        List<Long> fileIdsNeedingLookup = new ArrayList<>();

        for (EmbeddingMatch<TextSegment> match : matches) {
            if (match == null || match.embedded() == null) {
                continue;
            }
            TextSegment segment = match.embedded();
            Long fileId = extractLongMetadata(segment, "fileId");
            String title = extractTitle(segment);

            if (fileId == null) {
                continue;
            }
            if (dedup.containsKey(fileId)) {
                continue;
            }

            ChatCitationVO citation = new ChatCitationVO();
            citation.setFileId(fileId);
            citation.setTitle(title);
            dedup.put(fileId, citation);
            if (title == null || title.isBlank()) {
                fileIdsNeedingLookup.add(fileId);
            }
        }

        if (!fileIdsNeedingLookup.isEmpty()) {
            List<FileUpInfo> files = fileInfoMapper.selectBatchIds(fileIdsNeedingLookup);
            if (files != null) {
                Map<Long, FileUpInfo> fileMap = files.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(FileUpInfo::getId, f -> f, (a, b) -> a));
                for (Long fileId : fileIdsNeedingLookup) {
                    ChatCitationVO citation = dedup.get(fileId);
                    if (citation == null || (citation.getTitle() != null && !citation.getTitle().isBlank())) {
                        continue;
                    }
                    FileUpInfo file = fileMap.get(fileId);
                    if (file == null) {
                        continue;
                    }
                    if (file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()) {
                        citation.setTitle(file.getOriginalFilename());
                    } else if (file.getFileName() != null && !file.getFileName().isBlank()) {
                        citation.setTitle(file.getFileName());
                    }
                }
            }
        }

        return new ArrayList<>(dedup.values());
    }

    private Long extractLongMetadata(TextSegment segment, String key) {
        if (segment == null || segment.metadata() == null || key == null) {
            return null;
        }
        Object value = segment.metadata().toMap().get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String && !((String) value).isBlank()) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String extractTitle(TextSegment segment) {
        if (segment == null || segment.metadata() == null) {
            return null;
        }
        Map<String, Object> metadata = segment.metadata().toMap();
        List<String> keys = List.of("originalFilename", "original_filename", "title", "fileName", "file_name", "sourceNoteTitle");
        for (String key : keys) {
            Object value = metadata.get(key);
            if (value instanceof String && !((String) value).isBlank()) {
                return (String) value;
            }
        }
        return null;
    }
    
    /**
     * 构建用户消息
     */
    private String buildUserMessage(String originalMessage, String context) {
        if (context.isEmpty()) {
            return "【用户问题】\n" + originalMessage + "\n\n注意：未找到相关的个人知识库内容，请结合对话历史或已知信息直接回答。";
        }
        
        return context + "【用户问题】\n" + originalMessage + "\n\n请优先基于上述参考资料回答用户的问题；若参考资料与问题不相关，请结合对话历史或已知信息直接回答，不要以“参考资料不包含相关信息”为由拒绝。";
    }

    @lombok.Data
    public static class EnhancedChatPayload {
        private String enhancedMessage;
        private List<ChatCitationVO> citations = Collections.emptyList();
    }


    // ==================== 共享知识库相关方法 ====================


    /**
     * 基于共享知识库进行流式对话
     */
    public Flux<String> chatWithSharedKnowledgeBaseStream(SharedKnowledgeBaseChatDTO chatDTO, Long userId) {
        log.info("共享知识库流式对话，用户: {}, 知识库: {}", userId, chatDTO.getKnowledgeBaseId());
        
        try {
            // 设置当前用户ID到内容检索器
           // setCurrentUserId(String.valueOf(userId));
            
            // 构建增强消息（基于共享知识库）
            String enhancedMessage = buildSharedKnowledgeBaseMessage(chatDTO.getKnowledgeBaseId(), chatDTO.getMessage());
            
            // 构建会话ID
            String memoryId = chatDTO.getMemoryId() != null ? chatDTO.getMemoryId() : 
                            "shared_kb_" + chatDTO.getKnowledgeBaseId() + "_" + userId;
            
            log.info("调用流式AI模型，共享知识库: {}, 用户: {}, 会话: {}", 
                    chatDTO.getKnowledgeBaseId(), userId, memoryId);
            
            // 调用流式聊天服务
            return consultantService.streamChat(memoryId, enhancedMessage)
                .doOnNext(chunk -> log.debug("共享知识库流式响应块: {}", chunk))
                .doOnComplete(() -> log.info("共享知识库流式对话完成，知识库: {}, 用户: {}", 
                             chatDTO.getKnowledgeBaseId(), userId))
                .doOnError(error -> log.error("共享知识库流式对话失败: {}", error.getMessage()));
            
        } catch (Exception e) {
            log.error("共享知识库流式对话处理失败: {}", e.getMessage(), e);
            return Flux.error(new RuntimeException("共享知识库对话服务暂时不可用，请稍后再试"));
        }
    }

    /**
     * 构建基于共享知识库的增强消息
     */
    private String buildSharedKnowledgeBaseMessage(Long knowledgeBaseId, String message) {
        try {
            // 初始化系统提示词
            initSystemPrompt();
            
            // 为用户消息生成embedding
            Embedding queryEmbedding = embeddingModel.embed(message).content();
            
            // 创建搜索请求
            //EmbeddingSearchRequest.builder() ：创建一个搜索请求，
            // 其中包含了查询向量、期望返回的最大结果数 ( maxResults=5 )
            // 和最低相似度分数阈值 ( minScore=0.7 )。
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(5)
                    .minScore(0.7)
                    .build();
            
            // 从共享知识库搜索相关内容
            List<EmbeddingMatch<TextSegment>> matches = sharedVectorService.searchInSharedKnowledgeBase(
                    knowledgeBaseId, searchRequest);
            
            // 构建上下文
            String context = buildSharedKnowledgeBaseContext(matches);
            
            // 构建完整的用户消息
            String enhancedMessage = buildUserMessage(message, context);
            
            // 返回包含系统提示词的完整消息
            return systemPrompt + "\n\n" + enhancedMessage;
            
        } catch (Exception e) {
            log.error("构建共享知识库增强消息失败: {}", e.getMessage(), e);
            return systemPrompt + "\n\n用户问题：" + message;
        }
    }

    /**
     * 构建共享知识库上下文信息
     */
    private String buildSharedKnowledgeBaseContext(List<EmbeddingMatch<TextSegment>> matches) {
        if (matches == null || matches.isEmpty()) {
            return "";
        }
        
        String context = matches.stream()
                .map(match -> {
                    try {
                        return match.embedded().text();
                    } catch (Exception e) {
                        log.warn("获取匹配文本失败: {}", e.getMessage());
                        return "";
                    }
                })
                .filter(text -> !text.isEmpty())
                .limit(5) // 限制上下文长度
                .collect(Collectors.joining("\n\n"));
        
        if (context.isEmpty()) {
            return "";
        }
        
        return "共享知识库参考资料：\n" + context + "\n\n";
    }
} 
