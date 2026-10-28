package com.ldd.initialization.service;

import com.ldd.initialization.domain.ChatSession;
import com.ldd.initialization.vo.ChatCitationVO;
import com.ldd.initialization.vo.ChatMessageVO;
import com.ldd.initialization.vo.ChatSessionVO;

import java.util.List;

public interface ChatHistoryService {

    List<ChatSessionVO> listSessions(Long userId);

    ChatSessionVO createSession(String title, Long userId);

    void deleteSession(Long sessionId, Long userId);

    List<ChatMessageVO> listMessages(Long sessionId, Long userId);

    void saveUserMessage(Long sessionId, Long userId, String content, Integer totalTokens);

    void saveAssistantMessage(Long sessionId, Long userId, String content, Integer totalTokens, List<ChatCitationVO> citations);

    ChatSession getSessionForUser(Long sessionId, Long userId);
}
