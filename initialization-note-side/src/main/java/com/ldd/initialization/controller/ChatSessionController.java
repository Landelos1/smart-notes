package com.ldd.initialization.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.ldd.initialization.dto.ChatSessionCreateDTO;
import com.ldd.initialization.result.Result;
import com.ldd.initialization.service.ChatHistoryService;
import com.ldd.initialization.vo.ChatMessageVO;
import com.ldd.initialization.vo.ChatSessionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat/sessions")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatHistoryService chatHistoryService;

    @SaCheckLogin
    @GetMapping
    public Result<List<ChatSessionVO>> listSessions() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(chatHistoryService.listSessions(userId));
    }

    @SaCheckLogin
    @PostMapping
    public Result<ChatSessionVO> createSession(@Valid @RequestBody(required = false) ChatSessionCreateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        String title = dto != null ? dto.getTitle() : null;
        return Result.success(chatHistoryService.createSession(title, userId));
    }

    @SaCheckLogin
    @DeleteMapping("/{sessionId}")
    public Result<Void> deleteSession(@PathVariable Long sessionId) {
        Long userId = StpUtil.getLoginIdAsLong();
        chatHistoryService.deleteSession(sessionId, userId);
        return Result.success();
    }

    @SaCheckLogin
    @GetMapping("/{sessionId}/messages")
    public Result<List<ChatMessageVO>> listMessages(@PathVariable Long sessionId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(chatHistoryService.listMessages(sessionId, userId));
    }
}
