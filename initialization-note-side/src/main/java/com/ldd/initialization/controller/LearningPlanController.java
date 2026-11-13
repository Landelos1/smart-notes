package com.ldd.initialization.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.ldd.initialization.dto.LearningPlanGenerateDTO;
import com.ldd.initialization.result.Result;
import com.ldd.initialization.service.LearningPlanService;
import com.ldd.initialization.utils.UserUtils;
import com.ldd.initialization.vo.LearningPlanVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/learning-plans")
@SaCheckLogin
public class LearningPlanController {
    private final LearningPlanService learningPlanService;

    public LearningPlanController(LearningPlanService learningPlanService) { this.learningPlanService = learningPlanService; }

    @PostMapping("/generate/{noteId}")
    public Result<LearningPlanVO> generate(@PathVariable Long noteId,
                                           @RequestBody(required = false) LearningPlanGenerateDTO request) {
        boolean replaceExisting = request != null && Boolean.TRUE.equals(request.getReplaceExisting());
        return Result.success(learningPlanService.generateForNote(
                noteId, UserUtils.getCurrentUserId(), replaceExisting));
    }
}
