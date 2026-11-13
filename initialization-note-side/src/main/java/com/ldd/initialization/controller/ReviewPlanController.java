package com.ldd.initialization.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.ldd.initialization.dto.ReviewPlanCreateDTO;
import com.ldd.initialization.dto.ReviewPlanUpdateDTO;
import com.ldd.initialization.result.Result;
import com.ldd.initialization.service.ReviewPlanService;
import com.ldd.initialization.utils.UserUtils;
import com.ldd.initialization.vo.ReviewPlanVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/review-plans")
@SaCheckLogin
public class ReviewPlanController {
    private final ReviewPlanService reviewPlanService;

    public ReviewPlanController(ReviewPlanService reviewPlanService) { this.reviewPlanService = reviewPlanService; }

    @GetMapping
    public Result<List<ReviewPlanVO>> list() { return Result.success(reviewPlanService.list(UserUtils.getCurrentUserId())); }

    @PostMapping
    public Result<ReviewPlanVO> create(@Valid @RequestBody ReviewPlanCreateDTO dto) { return Result.success(reviewPlanService.create(dto, UserUtils.getCurrentUserId())); }

    @PutMapping("/{id}")
    public Result<ReviewPlanVO> update(@PathVariable Long id, @Valid @RequestBody ReviewPlanUpdateDTO dto) { return Result.success(reviewPlanService.update(id, dto, UserUtils.getCurrentUserId())); }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { reviewPlanService.delete(id, UserUtils.getCurrentUserId()); return Result.success(); }
}
