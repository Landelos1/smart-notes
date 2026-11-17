package com.ldd.initialization.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ldd.initialization.dto.NoteCommentCreateDTO;
import com.ldd.initialization.dto.NoteSquarePublishDTO;
import com.ldd.initialization.dto.NoteSquareSearchDTO;
import com.ldd.initialization.dto.NoteSquareSearchSummaryDTO;
import com.ldd.initialization.result.PageResult;
import com.ldd.initialization.result.Result;
import com.ldd.initialization.service.NoteSquareService;
import com.ldd.initialization.service.ai.adapter.ConsultantService;
import com.ldd.initialization.vo.NoteCommentVO;
import com.ldd.initialization.vo.NoteSquareDetailVO;
import com.ldd.initialization.vo.NoteSquareVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * Note square controller.
 */
@RestController
@RequestMapping("/api/note-square")
@RequiredArgsConstructor
  public class NoteSquareController {

    private final NoteSquareService noteSquareService;
    private final ConsultantService consultantService;
    @Qualifier("streamingExecutor")
    @Autowired
    private ThreadPoolTaskExecutor streamingExecutor;

    /**
     * 发布笔记到笔记广场。
     *
     * @param publishDTO 发布笔记请求参数
     * @return 发布后的笔记详情
     */
    @PostMapping("/publish")
    public Result<NoteSquareDetailVO> publishNote(@Valid @RequestBody NoteSquarePublishDTO publishDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        NoteSquareDetailVO detail = noteSquareService.publishNote(publishDTO, userId);
        return Result.success(detail);
    }

    /**
     * 将笔记从笔记广场下架。
     *
     * @param noteId 笔记ID
     * @return 操作结果
     */
    @PostMapping("/{noteId}/unpublish")
    public Result<Void> unpublishNote(@PathVariable Long noteId) {
        Long userId = StpUtil.getLoginIdAsLong();
        noteSquareService.unpublishNote(noteId, userId);
        return Result.success();
    }

    /**
     * 分页获取笔记广场列表。
     *
     * @param keyword 搜索关键词
     * @param sortBy 排序字段
     * @param sortOrder 排序方向
     * @param page 页码
     * @param size 每页条数
     * @return 笔记广场分页数据
     */
    @GetMapping
    public Result<PageResult<NoteSquareVO>> getSquareNotes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "recommend") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        Long userId = StpUtil.getLoginIdAsLong();
        NoteSquareSearchDTO searchDTO = new NoteSquareSearchDTO();
        searchDTO.setKeyword(keyword);
        searchDTO.setCategory(category);
        searchDTO.setSortBy(sortBy);
        searchDTO.setSortOrder(sortOrder);
        searchDTO.setPage(page);
        searchDTO.setSize(size);
        return Result.success(noteSquareService.getSquareNotes(searchDTO, userId));
    }

    @GetMapping("/categories")
    public Result<List<String>> getPublishedCategories() {
        return Result.success(noteSquareService.getPublishedCategories());
    }

    /**
     * 分页获取当前用户收藏的广场笔记。
     *
     * @param keyword 搜索关键词
     * @param page 页码
     * @param size 每页条数
     * @return 收藏笔记分页数据
     */
    @GetMapping("/favorites")
    public Result<PageResult<NoteSquareVO>> getFavoriteNotes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        Long userId = StpUtil.getLoginIdAsLong();
        NoteSquareSearchDTO searchDTO = new NoteSquareSearchDTO();
        searchDTO.setKeyword(keyword);
        searchDTO.setPage(page);
        searchDTO.setSize(size);
        return Result.success(noteSquareService.getFavoriteNotes(searchDTO, userId));
    }

    /**
     * 分页获取当前用户点赞的广场笔记。
     *
     * @param keyword 搜索关键词
     * @param page 页码
     * @param size 每页条数
     * @return 点赞笔记分页数据
     */
    @GetMapping("/likes")
    public Result<PageResult<NoteSquareVO>> getLikedNotes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        Long userId = StpUtil.getLoginIdAsLong();
        NoteSquareSearchDTO searchDTO = new NoteSquareSearchDTO();
        searchDTO.setKeyword(keyword);
        searchDTO.setPage(page);
        searchDTO.setSize(size);
        return Result.success(noteSquareService.getLikedNotes(searchDTO, userId));
    }

    /**
     * 分页获取当前用户发布的广场笔记。
     *
     * @param keyword 搜索关键词
     * @param page 页码
     * @param size 每页条数
     * @return 我的笔记分页数据
     */
    @GetMapping("/my")
    public Result<PageResult<NoteSquareVO>> getMyNotes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        Long userId = StpUtil.getLoginIdAsLong();
        NoteSquareSearchDTO searchDTO = new NoteSquareSearchDTO();
        searchDTO.setKeyword(keyword);
        searchDTO.setPage(page);
        searchDTO.setSize(size);
        return Result.success(noteSquareService.getMyNotes(searchDTO, userId));
    }

    /**
     * 获取指定广场笔记详情。
     *
     * @param noteId 笔记ID
     * @return 笔记详情
     */
    @GetMapping("/{noteId}")
    public Result<NoteSquareDetailVO> getNoteDetail(@PathVariable Long noteId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(noteSquareService.getNoteDetail(noteId, userId));
    }

    /**
     * 获取指定广场笔记的评论列表。
     *
     * @param noteId 笔记ID
     * @return 评论列表
     */
    @GetMapping("/{noteId}/comments")
    public Result<List<NoteCommentVO>> getNoteComments(@PathVariable Long noteId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(noteSquareService.getNoteComments(noteId, userId));
    }

    /**
     * 发表广场笔记评论。
     *
     * @param noteId 笔记ID
     * @param commentDTO 评论内容
     * @return 新创建的评论信息
     */
    @PostMapping("/{noteId}/comments")
    public Result<NoteCommentVO> createNoteComment(@PathVariable Long noteId,
                                                   @Valid @RequestBody NoteCommentCreateDTO commentDTO) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(noteSquareService.createNoteComment(noteId, commentDTO, userId));
    }

    /**
     * 删除当前用户可操作的评论。
     *
     * @param commentId 评论ID
     * @return 操作结果
     */
    @DeleteMapping("/comments/{commentId}")
    public Result<Void> deleteNoteComment(@PathVariable Long commentId) {
        Long userId = StpUtil.getLoginIdAsLong();
        noteSquareService.deleteNoteComment(commentId, userId);
        return Result.success();
    }

    /**
     * 点赞指定广场笔记。
     *
     * @param noteId 笔记ID
     * @return 操作结果
     */
    @PostMapping("/{noteId}/like")
    public Result<Void> likeNote(@PathVariable Long noteId) {
        Long userId = StpUtil.getLoginIdAsLong();
        noteSquareService.likeNote(noteId, userId);
        return Result.success();
    }

    /**
     * 取消点赞指定广场笔记。
     *
     * @param noteId 笔记ID
     * @return 操作结果
     */
    @DeleteMapping("/{noteId}/like")
    public Result<Void> unlikeNote(@PathVariable Long noteId) {
        Long userId = StpUtil.getLoginIdAsLong();
        noteSquareService.unlikeNote(noteId, userId);
        return Result.success();
    }

    /**
     * 收藏指定广场笔记。
     *
     * @param noteId 笔记ID
     * @return 操作结果
     */
    @PostMapping("/{noteId}/favorite")
    public Result<Void> favoriteNote(@PathVariable Long noteId) {
        Long userId = StpUtil.getLoginIdAsLong();
        noteSquareService.favoriteNote(noteId, userId);
        return Result.success();
    }

    /**
     * 取消收藏指定广场笔记。
     *
     * @param noteId 笔记ID
     * @return 操作结果
     */
    @DeleteMapping("/{noteId}/favorite")
    public Result<Void> unfavoriteNote(@PathVariable Long noteId) {
        Long userId = StpUtil.getLoginIdAsLong();
        noteSquareService.unfavoriteNote(noteId, userId);
        return Result.success();
    }

    /**
     * 基于当前搜索结果生成AI总结。
     *
     * @param request 搜索总结请求参数
     * @return SSE流式总结内容
     */
    @PostMapping(value = "/search-summary", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> getSearchSummary(@Valid @RequestBody NoteSquareSearchSummaryDTO request) {
        Long userId = StpUtil.getLoginIdAsLong();
        String keyword = request.getKeyword();
        int maxNotes = normalizeMaxNotes(request.getMaxNotes());

        NoteSquareSearchDTO searchDTO = new NoteSquareSearchDTO();
        searchDTO.setKeyword(keyword);
        searchDTO.setCategory(request.getCategory());
        searchDTO.setSortBy(request.getSortBy());
        searchDTO.setSortOrder(request.getSortOrder());
        searchDTO.setPage(1);
        searchDTO.setSize(maxNotes);

        PageResult<NoteSquareVO> result = noteSquareService.getSquareNotes(searchDTO, userId);
        List<NoteSquareVO> notes = result != null ? result.getData() : null;
        if (notes == null || notes.isEmpty()) {
            return Flux.just("未找到可总结的笔记。");
        }

        String prompt = buildSummaryPrompt(keyword, notes);
        String memoryId = "note_square_search_" + userId + "_" + System.currentTimeMillis();

        return consultantService.streamChat(memoryId, prompt)
                .subscribeOn(Schedulers.fromExecutor(streamingExecutor))
                .map(chunk -> chunk.replace("\n", "[NL]"))
                .onErrorResume(throwable -> Flux.just("抱歉，AI总结暂时不可用，请稍后再试。"));
    }

    /**
     * 规范化可参与总结的笔记数量范围。
     *
     * @param maxNotes 请求中的笔记数量
     * @return 限制在 1 到 20 之间的笔记数量，默认10
     */
    private int normalizeMaxNotes(Integer maxNotes) {
        if (maxNotes == null) {
            return 10;
        }
        return Math.min(Math.max(maxNotes, 1), 20);
    }

    /**
     * 构建用于AI总结的提示词内容。
     *
     * @param keyword 用户搜索关键词
     * @param notes 搜索得到的笔记列表
     * @return 发送给AI模型的提示词
     */
    private String buildSummaryPrompt(String keyword, List<NoteSquareVO> notes) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("你是笔记广场搜索助手，请根据用户搜索结果生成智能总结。\n");
        sb.append("用户搜索关键词：").append(keyword).append("\n\n");
        sb.append("以下是搜索到的").append(notes.size()).append("篇相关笔记摘要：\n\n");

        int index = 1;
        for (NoteSquareVO note : notes) {
            String title = StringUtils.hasText(note.getTitle()) ? note.getTitle() : "未命名";
            sb.append(index++).append(". 标题：").append(title).append("\n");
            String summary = StringUtils.hasText(note.getSummary()) ? note.getSummary() : note.getContent();
            summary = normalizeSnippet(summary, 240);
            if (StringUtils.hasText(summary)) {
                sb.append("   摘要：").append(summary).append("\n");
            }
            if (note.getTags() != null && !note.getTags().isEmpty()) {
                sb.append("   标签：").append(String.join(", ", note.getTags())).append("\n");
            }
            sb.append("\n");
        }

        sb.append("请输出：\n");
        sb.append("1. 核心主题归纳（1-2句话）\n");
        sb.append("2. 关键要点（3-5条，使用1)、2)编号）\n");
        sb.append("3. 内容分布或亮点（1-2句话）\n");
        sb.append("表达简洁、友好，避免编造未提及的信息。\n");
        sb.append("请使用纯文本输出，不要使用Markdown符号（例如 **、#、`、-）。");
        return sb.toString();
    }

    /**
     * 规范化文本摘要片段长度。
     *
     * @param text 原始文本
     * @param limit 最大长度
     * @return 处理后的摘要片段
     */
    private String normalizeSnippet(String text, int limit) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= limit) {
            return normalized;
        }
        return normalized.substring(0, limit) + "...";
    }
}
