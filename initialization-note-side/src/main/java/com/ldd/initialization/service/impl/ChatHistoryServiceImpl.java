package com.ldd.initialization.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ldd.initialization.config.exception.BizException;
import com.ldd.initialization.domain.ChatMessage;
import com.ldd.initialization.domain.ChatSession;
import com.ldd.initialization.enums.ChatRoleEnum;
import com.ldd.initialization.mapper.ChatMessageMapper;
import com.ldd.initialization.mapper.ChatSessionMapper;
import com.ldd.initialization.result.BizResponseCode;
import com.ldd.initialization.service.ChatHistoryService;
import com.ldd.initialization.vo.ChatCitationVO;
import com.ldd.initialization.vo.ChatMessageVO;
import com.ldd.initialization.vo.ChatSessionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatHistoryServiceImpl implements ChatHistoryService {

    private static final String DEFAULT_SESSION_TITLE = "新对话";
    private static final int DEFAULT_TITLE_LENGTH = 20;

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<ChatSessionVO> listSessions(Long userId) {
        List<ChatSession> sessions = chatSessionMapper.selectList(
            new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdateTime)
        );
        return sessions.stream().map(this::toSessionVO).collect(Collectors.toList());
    }

    @Override
    public ChatSessionVO createSession(String title, Long userId) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(StringUtils.hasText(title) ? title : DEFAULT_SESSION_TITLE);
        chatSessionMapper.insert(session);
        return toSessionVO(session);
    }

    @Override
    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        ChatSession session = getSessionForUser(sessionId, userId);
        chatMessageMapper.delete(
            new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getSessionId, session.getId())
        );
        chatSessionMapper.deleteById(session.getId());
    }

    @Override
    public List<ChatMessageVO> listMessages(Long sessionId, Long userId) {
        getSessionForUser(sessionId, userId);
        List<ChatMessage> messages = chatMessageMapper.selectList(
            new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getTimestamp)
                .orderByAsc(ChatMessage::getId)
        );
        return messages.stream().map(this::toMessageVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void saveUserMessage(Long sessionId, Long userId, String content, Integer totalTokens) {
        ChatSession session = getSessionForUser(sessionId, userId);
        insertMessage(sessionId, ChatRoleEnum.USER, content, totalTokens);
        ChatSession update = new ChatSession();
        update.setId(sessionId);
        update.setLastMessage(content);
        if (!StringUtils.hasText(session.getTitle()) || DEFAULT_SESSION_TITLE.equals(session.getTitle())) {
            update.setTitle(buildTitle(content));
        }
        chatSessionMapper.updateById(update);
    }

    @Override
    @Transactional
    public void saveAssistantMessage(Long sessionId, Long userId, String content, Integer totalTokens, List<ChatCitationVO> citations) {
        if (!StringUtils.hasText(content)) {
            return;
        }
        getSessionForUser(sessionId, userId);
        insertMessage(sessionId, ChatRoleEnum.ASSISTANT, content, totalTokens, citations);
        ChatSession update = new ChatSession();
        update.setId(sessionId);
        update.setLastMessage(content);
        chatSessionMapper.updateById(update);
    }

    @Override
    public ChatSession getSessionForUser(Long sessionId, Long userId) {
        ChatSession session = chatSessionMapper.selectOne(
            new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getId, sessionId)
                .eq(ChatSession::getUserId, userId)
        );
        if (session == null) {
            throw new BizException(BizResponseCode.ERROR_1, "会话不存在或无权限");
        }
        return session;
    }

    private void insertMessage(Long sessionId, ChatRoleEnum role, String content, Integer totalTokens) {
        insertMessage(sessionId, role, content, totalTokens, null);
    }

    private void insertMessage(Long sessionId, ChatRoleEnum role, String content, Integer totalTokens, List<ChatCitationVO> citations) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role.getValue());
        message.setContent(content);
        message.setCitations(toJson(citations));
        message.setTotalTokens(totalTokens != null ? totalTokens : 0);
        message.setTimestamp(LocalDateTime.now());
        chatMessageMapper.insert(message);
    }

    private ChatSessionVO toSessionVO(ChatSession session) {
        ChatSessionVO vo = new ChatSessionVO();
        vo.setId(session.getId());
        vo.setTitle(session.getTitle());
        vo.setLastMessage(session.getLastMessage());
        vo.setTimestamp(session.getUpdateTime() != null ? session.getUpdateTime() : session.getCreateTime());
        return vo;
    }

    private ChatMessageVO toMessageVO(ChatMessage message) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(message.getId());
        vo.setRole(ChatRoleEnum.keyOf(message.getRole()));
        vo.setContent(message.getContent());
        vo.setCitations(parseCitations(message.getCitations()));
        vo.setTimestamp(message.getTimestamp());
        return vo;
    }

    private List<ChatCitationVO> parseCitations(String citations) {
        if (!StringUtils.hasText(citations)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(citations,
                objectMapper.getTypeFactory().constructCollectionType(List.class, ChatCitationVO.class));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String toJson(List<ChatCitationVO> citations) {
        if (citations == null || citations.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(citations);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private String buildTitle(String content) {
        if (!StringUtils.hasText(content)) {
            return DEFAULT_SESSION_TITLE;
        }
        String trimmed = content.trim();
        if (trimmed.length() <= DEFAULT_TITLE_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, DEFAULT_TITLE_LENGTH) + "...";
    }
}
