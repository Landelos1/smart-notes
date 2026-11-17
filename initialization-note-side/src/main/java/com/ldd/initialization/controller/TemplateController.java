package com.ldd.initialization.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.ldd.initialization.dto.TemplateCommentCreateDTO;
import com.ldd.initialization.dto.TemplateCreateDTO;
import com.ldd.initialization.dto.TemplateReportDTO;
import com.ldd.initialization.dto.TemplateSearchDTO;
import com.ldd.initialization.result.PageResult;
import com.ldd.initialization.result.Result;
import com.ldd.initialization.service.TemplateService;
import com.ldd.initialization.vo.TemplateCommentVO;
import com.ldd.initialization.vo.TemplateShareVO;
import com.ldd.initialization.vo.TemplateVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模板中心控制器
 */
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    /**
     * 搜索模板列表
     */
    @GetMapping
    public Result<PageResult<TemplateVO>> searchTemplates(
            @RequestParam(required = false, defaultValue = "all") String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        
        TemplateSearchDTO searchDTO = new TemplateSearchDTO();
        searchDTO.setCategory(category);
        searchDTO.setKeyword(keyword);
        searchDTO.setStatus(status);
        searchDTO.setPage(page);
        searchDTO.setPageSize(pageSize);
        
        PageResult<TemplateVO> result = templateService.searchTemplates(searchDTO);
        return Result.success(result);
    }

    /**
     * 获取模板详情
     */
    @GetMapping("/{id}")
    public Result<TemplateVO> getTemplateById(@PathVariable Long id) {
        TemplateVO template = templateService.getTemplateById(id);
        if (template == null) {
            return Result.error("模板不存在");
        }
        return Result.success(template);
    }

    /**
     * 获取相关模板
     */
    @GetMapping("/{id}/related")
    public Result<List<TemplateVO>> getRelatedTemplates(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "3") Integer limit) {
        List<TemplateVO> templates = templateService.getRelatedTemplates(id, limit);
        return Result.success(templates);
    }

    /**
     * 获取模板评论
     */
    @GetMapping("/{id}/comments")
    public Result<List<TemplateCommentVO>> getTemplateComments(@PathVariable Long id) {
        List<TemplateCommentVO> comments = templateService.getTemplateComments(id);
        return Result.success(comments);
    }

    /**
     * 管理员评论管理列表
     */
    @SaCheckRole("ADMIN")
    @GetMapping("/admin/comments")
    public Result<PageResult<TemplateCommentVO>> listAdminComments(
            @RequestParam(required = false, defaultValue = "1") Integer current,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false, defaultValue = "ALL") String sourceType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) String keyword) {
        PageResult<TemplateCommentVO> result = templateService.listAdminComments(current, size, sourceType, targetId, keyword);
        return Result.success(result);
    }

    /**
     * 管理员删除评论
     */
    @SaCheckRole("ADMIN")
    @DeleteMapping("/admin/comments/{commentId}")
    public Result<Void> deleteAdminComment(@PathVariable Long commentId,
                                           @RequestParam(required = false, defaultValue = "TEMPLATE") String sourceType) {
        Long userId = StpUtil.getLoginIdAsLong();
        templateService.deleteAdminComment(commentId, sourceType, userId);
        return Result.success();
    }

    /**
     * 使用模板（增加下载次数）
     */
    @PostMapping("/{id}/use")
    public Result<Void> useTemplate(@PathVariable Long id) {
        templateService.useTemplate(id);
        return Result.success();
    }

    /**
     * 点赞评论
     */
    @PostMapping("/comments/{commentId}/like")
    public Result<Void> likeComment(@PathVariable Long commentId) {
        templateService.likeComment(commentId);
        return Result.success();
    }

    /**
     * 创建模板
     */
    @PostMapping
    public Result<TemplateVO> createTemplate(@Valid @RequestBody TemplateCreateDTO createDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        TemplateVO template = templateService.createTemplate(createDTO, userId);
        return Result.success(template);
    }

    /**
     * 更新模板
     */
    @PutMapping("/{id}")
    public Result<TemplateVO> updateTemplate(@PathVariable Long id, @Valid @RequestBody TemplateCreateDTO createDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        createDTO.setId(id);
        TemplateVO template = templateService.updateTemplate(createDTO, userId);
        return Result.success(template);
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        templateService.deleteTemplate(id, userId);
        return Result.success();
    }

    /**
     * 获取我的模板列表
     */
    @GetMapping("/my")
    public Result<List<TemplateVO>> getMyTemplates() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<TemplateVO> templates = templateService.getMyTemplates(userId);
        return Result.success(templates);
    }

    /**
     * 生成模板分享信息
     */
    @PostMapping("/{id}/share")
    public Result<TemplateShareVO> shareTemplate(@PathVariable Long id) {
        TemplateShareVO shareInfo = templateService.generateShareInfo(id);
        return Result.success(shareInfo);
    }

    /**
     * 收藏模板
     */
    @PostMapping("/{id}/favorite")
    public Result<Void> favoriteTemplate(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        templateService.favoriteTemplate(id, userId);
        return Result.success();
    }

    /**
     * 取消收藏模板
     */
    @DeleteMapping("/{id}/favorite")
    public Result<Void> unfavoriteTemplate(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        templateService.unfavoriteTemplate(id, userId);
        return Result.success();
    }

    /**
     * 查询收藏状态
     */
    @GetMapping("/{id}/favorite/status")
    public Result<Boolean> getFavoriteStatus(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean isFavorited = templateService.getFavoriteStatus(id, userId);
        return Result.success(isFavorited);
    }

    /**
     * 获取收藏的模板列表
     */
    @GetMapping("/favorites")
    public Result<PageResult<TemplateVO>> getFavoriteTemplates(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        Long userId = StpUtil.getLoginIdAsLong();
        PageResult<TemplateVO> result = templateService.getFavoriteTemplates(userId, page, size);
        return Result.success(result);
    }

    /**
     * 举报模板
     */
    @PostMapping("/{id}/report")
    public Result<Void> reportTemplate(@PathVariable Long id, @Valid @RequestBody TemplateReportDTO reportDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        templateService.reportTemplate(id, reportDTO, userId);
        return Result.success();
    }

    /**
     * 发表评论
     */
    @PostMapping("/{id}/comments")
    public Result<TemplateCommentVO> createComment(@PathVariable Long id, @Valid @RequestBody TemplateCommentCreateDTO commentDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        TemplateCommentVO comment = templateService.createComment(id, commentDTO, userId);
        return Result.success(comment);
    }

    /**
     * 取消点赞评论
     */
    @DeleteMapping("/comments/{commentId}/like")
    public Result<Void> unlikeComment(@PathVariable Long commentId) {
        Long userId = StpUtil.getLoginIdAsLong();
        templateService.unlikeComment(commentId, userId);
        return Result.success();
    }
}
