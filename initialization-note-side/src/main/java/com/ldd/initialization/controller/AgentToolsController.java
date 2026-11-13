package com.ldd.initialization.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.ldd.initialization.dto.AgentToolRequestDTO;
import com.ldd.initialization.result.Result;
import com.ldd.initialization.service.AgentToolsService;
import com.ldd.initialization.utils.UserUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent/tools")
@SaCheckLogin
public class AgentToolsController {
    private final AgentToolsService agentToolsService;

    public AgentToolsController(AgentToolsService agentToolsService) { this.agentToolsService = agentToolsService; }

    @PostMapping("/execute")
    public Result<Object> execute(@Valid @RequestBody AgentToolRequestDTO request) {
        return Result.success(agentToolsService.execute(request, UserUtils.getCurrentUserId()));
    }
}
