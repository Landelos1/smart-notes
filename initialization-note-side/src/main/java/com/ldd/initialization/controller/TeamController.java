package com.ldd.initialization.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ldd.initialization.dto.TeamDTO.*;
import com.ldd.initialization.result.PageResult;
import com.ldd.initialization.result.Result;
import com.ldd.initialization.service.TeamService;
import com.ldd.initialization.utils.UserUtils;
import com.ldd.initialization.vo.TeamVO.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 团队协作空间Controller
 */
@RestController
@RequestMapping("/api/teams")
@SaCheckLogin
public class TeamController {

    @Autowired
    private TeamService teamService;

    // ==================== 团队管理 ====================

    /**
     * 获取用户所属团队列表
     */
    @GetMapping
    public Result<List<TeamSpaceVO>> getMyTeams() {
        Long userId = UserUtils.getCurrentUserId();
        List<TeamSpaceVO> teams = teamService.getMyTeams(userId);
        return Result.success(teams);
    }

    @GetMapping("/discover")
    public Result<List<TeamSpaceVO>> getDiscoverableTeams() {
        Long userId = UserUtils.getCurrentUserId();
        List<TeamSpaceVO> teams = teamService.getDiscoverableTeams(userId);
        return Result.success(teams);
    }

    /**
     * 创建团队
     */
    @PostMapping
    public Result<TeamSpaceVO> createTeam(@Valid @RequestBody TeamCreateDTO dto) {
        Long userId = UserUtils.getCurrentUserId();
        TeamSpaceVO team = teamService.createTeam(dto, userId);
        return Result.success(team);
    }

    /**
     * 通过邀请码加入团队
     */
    @PostMapping("/join")
    public Result<TeamSpaceVO> joinTeam(@Valid @RequestBody TeamJoinDTO dto) {
        Long userId = UserUtils.getCurrentUserId();
        TeamSpaceVO team = teamService.joinByInviteCode(dto.getInviteCode(), userId);
        return Result.success(team);
    }

    /**
     * 获取团队详情
     */
    @GetMapping("/{teamId}")
    public Result<TeamSpaceVO> getTeamDetail(@PathVariable Long teamId) {
        Long userId = UserUtils.getCurrentUserId();
        TeamSpaceVO team = teamService.getTeamDetail(teamId, userId);
        return Result.success(team);
    }

    @PutMapping("/{teamId}")
    public Result<TeamSpaceVO> updateTeam(
            @PathVariable Long teamId,
            @Valid @RequestBody TeamUpdateDTO dto) {
        Long userId = UserUtils.getCurrentUserId();
        TeamSpaceVO team = teamService.updateTeam(teamId, dto, userId);
        return Result.success(team);
    }

    /**
     * 解散团队（仅创建者）
     */
    @DeleteMapping("/{teamId}")
    public Result<Void> dissolveTeam(@PathVariable Long teamId) {
        Long userId = UserUtils.getCurrentUserId();
        teamService.dissolveTeam(teamId, userId);
        return Result.success(null);
    }

    /**
     * 退出团队
     */
    @PostMapping("/{teamId}/leave")
    public Result<Void> leaveTeam(@PathVariable Long teamId) {
        Long userId = UserUtils.getCurrentUserId();
        teamService.leaveTeam(teamId, userId);
        return Result.success(null);
    }

    /**
     * 重置邀请码
     */
    @PostMapping("/{teamId}/invite-code/reset")
    public Result<TeamSpaceVO> resetInviteCode(@PathVariable Long teamId) {
        Long userId = UserUtils.getCurrentUserId();
        TeamSpaceVO team = teamService.resetInviteCode(teamId, userId);
        return Result.success(team);
    }

    // ==================== 成员管理 ====================

    /**
     * 获取团队成员列表
     */
    @GetMapping("/{teamId}/members")
    public Result<List<TeamMemberVO>> getMembers(@PathVariable Long teamId) {
        Long userId = UserUtils.getCurrentUserId();
        List<TeamMemberVO> members = teamService.getMembers(teamId, userId);
        return Result.success(members);
    }

    /**
     * 修改成员角色
     */
    @PutMapping("/{teamId}/members/{targetUserId}/role")
    public Result<Void> updateMemberRole(
            @PathVariable Long teamId,
            @PathVariable Long targetUserId,
            @Valid @RequestBody MemberRoleDTO dto) {
        Long userId = UserUtils.getCurrentUserId();
        teamService.updateMemberRole(teamId, targetUserId, dto.getRole(), userId);
        return Result.success(null);
    }

    /**
     * 移除成员
     */
    @DeleteMapping("/{teamId}/members/{targetUserId}")
    public Result<Void> removeMember(
            @PathVariable Long teamId,
            @PathVariable Long targetUserId) {
        Long userId = UserUtils.getCurrentUserId();
        teamService.removeMember(teamId, targetUserId, userId);
        return Result.success(null);
    }

    // ==================== 文档管理 ====================

    /**
     * 获取团队文档列表（分页）
     */
    @GetMapping("/{teamId}/docs")
    public Result<PageResult<DocBriefVO>> getDocList(
            @PathVariable Long teamId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        Long userId = UserUtils.getCurrentUserId();
        IPage<DocBriefVO> docPage = teamService.getDocList(teamId, page, size, keyword, userId);
        return Result.success(PageResult.convert(docPage));
    }

    /**
     * 创建文档
     */
    @PostMapping("/{teamId}/docs")
    public Result<DocDetailVO> createDoc(
            @PathVariable Long teamId,
            @Valid @RequestBody DocCreateDTO dto) {
        Long userId = UserUtils.getCurrentUserId();
        DocDetailVO doc = teamService.createDoc(teamId, dto, userId);
        return Result.success(doc);
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/{teamId}/docs/{docId}")
    public Result<DocDetailVO> getDocDetail(
            @PathVariable Long teamId,
            @PathVariable Long docId) {
        Long userId = UserUtils.getCurrentUserId();
        DocDetailVO doc = teamService.getDocDetail(teamId, docId, userId);
        return Result.success(doc);
    }

    /**
     * 更新文档
     */
    @PutMapping("/{teamId}/docs/{docId}")
    public Result<DocDetailVO> updateDoc(
            @PathVariable Long teamId,
            @PathVariable Long docId,
            @Valid @RequestBody DocUpdateDTO dto) {
        Long userId = UserUtils.getCurrentUserId();
        DocDetailVO doc = teamService.updateDoc(teamId, docId, dto, userId);
        return Result.success(doc);
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{teamId}/docs/{docId}")
    public Result<Void> deleteDoc(
            @PathVariable Long teamId,
            @PathVariable Long docId) {
        Long userId = UserUtils.getCurrentUserId();
        teamService.deleteDoc(teamId, docId, userId);
        return Result.success(null);
    }

    /**
     * 获取编辑锁
     */
    @PostMapping("/{teamId}/docs/{docId}/lock")
    public Result<LockInfoVO> acquireLock(
            @PathVariable Long teamId,
            @PathVariable Long docId) {
        Long userId = UserUtils.getCurrentUserId();
        LockInfoVO lockInfo = teamService.acquireLock(teamId, docId, userId);
        return Result.success(lockInfo);
    }

    /**
     * 释放编辑锁
     */
    @DeleteMapping("/{teamId}/docs/{docId}/lock")
    public Result<Void> releaseLock(
            @PathVariable Long teamId,
            @PathVariable Long docId,
            @RequestParam(required = false) String lockToken) {
        Long userId = UserUtils.getCurrentUserId();
        teamService.releaseLock(teamId, docId, lockToken, userId);
        return Result.success(null);
    }

    /**
     * 获取版本历史
     */
    @GetMapping("/{teamId}/docs/{docId}/versions")
    public Result<List<DocVersionVO>> getVersionHistory(
            @PathVariable Long teamId,
            @PathVariable Long docId) {
        Long userId = UserUtils.getCurrentUserId();
        List<DocVersionVO> versions = teamService.getVersionHistory(teamId, docId, userId);
        return Result.success(versions);
    }

    /**
     * 获取文档动态列表
     */
    @GetMapping("/{teamId}/docs/{docId}/activities")
    public Result<List<DocActivityVO>> getActivities(
            @PathVariable Long teamId,
            @PathVariable Long docId) {
        Long userId = UserUtils.getCurrentUserId();
        List<DocActivityVO> activities = teamService.getActivities(teamId, docId, userId);
        return Result.success(activities);
    }

    /**
     * 恢复到指定版本
     */
    @PostMapping("/{teamId}/docs/{docId}/restore")
    public Result<DocDetailVO> restoreVersion(
            @PathVariable Long teamId,
            @PathVariable Long docId,
            @Valid @RequestBody DocRestoreDTO dto) {
        Long userId = UserUtils.getCurrentUserId();
        DocDetailVO doc = teamService.restoreVersion(teamId, docId, dto.getEventId(), userId);
        return Result.success(doc);
    }

    // ==================== 评论管理 ====================

    /**
     * 获取文档评论列表
     */
    @GetMapping("/{teamId}/docs/{docId}/comments")
    public Result<List<DocCommentVO>> getComments(
            @PathVariable Long teamId,
            @PathVariable Long docId,
            @RequestParam(required = false) Integer type) {
        Long userId = UserUtils.getCurrentUserId();
        List<DocCommentVO> comments = teamService.getComments(teamId, docId, type, userId);
        return Result.success(comments);
    }

    /**
     * 添加评论
     */
    @PostMapping("/{teamId}/docs/{docId}/comments")
    public Result<DocCommentVO> addComment(
            @PathVariable Long teamId,
            @PathVariable Long docId,
            @Valid @RequestBody CommentCreateDTO dto) {
        Long userId = UserUtils.getCurrentUserId();
        DocCommentVO comment = teamService.addComment(teamId, docId, dto, userId);
        return Result.success(comment);
    }

    /**
     * 标记评论已解决
     */
    @PutMapping("/{teamId}/docs/{docId}/comments/{commentId}/resolve")
    public Result<Void> resolveComment(
            @PathVariable Long teamId,
            @PathVariable Long docId,
            @PathVariable Long commentId) {
        Long userId = UserUtils.getCurrentUserId();
        teamService.resolveComment(teamId, docId, commentId, userId);
        return Result.success(null);
    }

    // ==================== AI会话管理 ====================

    /**
     * 获取AI会话列表
     */
    @GetMapping("/{teamId}/ai/sessions")
    public Result<List<AISessionVO>> getAISessions(@PathVariable Long teamId) {
        Long userId = UserUtils.getCurrentUserId();
        List<AISessionVO> sessions = teamService.getAISessions(teamId, userId);
        return Result.success(sessions);
    }

    /**
     * 创建AI会话
     */
    @PostMapping("/{teamId}/ai/sessions")
    public Result<AISessionVO> createAISession(
            @PathVariable Long teamId,
            @Valid @RequestBody(required = false) AISessionCreateDTO dto) {
        Long userId = UserUtils.getCurrentUserId();
        AISessionVO session = teamService.createAISession(teamId, dto != null ? dto : new AISessionCreateDTO(), userId);
        return Result.success(session);
    }

    /**
     * 删除AI会话
     */
    @DeleteMapping("/{teamId}/ai/sessions/{sessionId}")
    public Result<Void> deleteAISession(
            @PathVariable Long teamId,
            @PathVariable Long sessionId) {
        Long userId = UserUtils.getCurrentUserId();
        teamService.deleteAISession(teamId, sessionId, userId);
        return Result.success(null);
    }

    /**
     * 置顶/取消置顶AI会话
     */
    @PutMapping("/{teamId}/ai/sessions/{sessionId}/pin")
    public Result<Void> togglePinSession(
            @PathVariable Long teamId,
            @PathVariable Long sessionId) {
        Long userId = UserUtils.getCurrentUserId();
        teamService.togglePinSession(teamId, sessionId, userId);
        return Result.success(null);
    }

    /**
     * 获取AI消息列表
     */
    @GetMapping("/{teamId}/ai/sessions/{sessionId}/messages")
    public Result<List<AIMessageVO>> getAIMessages(
            @PathVariable Long teamId,
            @PathVariable Long sessionId) {
        Long userId = UserUtils.getCurrentUserId();
        List<AIMessageVO> messages = teamService.getAIMessages(teamId, sessionId, userId);
        return Result.success(messages);
    }

    /**
     * 发送AI消息
     */
    @PostMapping("/{teamId}/ai/sessions/{sessionId}/messages")
    public Result<AIMessageVO> sendAIMessage(
            @PathVariable Long teamId,
            @PathVariable Long sessionId,
            @Valid @RequestBody AIMessageDTO dto) {
        Long userId = UserUtils.getCurrentUserId();
        AIMessageVO message = teamService.sendAIMessage(teamId, sessionId, dto, userId);
        return Result.success(message);
    }

    @PostMapping(value = "/{teamId}/ai/sessions/{sessionId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> sendAIMessageStream(
            @PathVariable Long teamId,
            @PathVariable Long sessionId,
            @Valid @RequestBody AIMessageDTO dto) {
        Long userId = UserUtils.getCurrentUserId();
        return teamService.sendAIMessageStream(teamId, sessionId, dto, userId);
    }

    @PostMapping("/{teamId}/ai/knowledge-base/reindex")
    public Result<Void> reindexTeamKnowledgeBase(@PathVariable Long teamId) {
        Long userId = UserUtils.getCurrentUserId();
        teamService.reindexTeamKnowledgeBase(teamId, userId);
        return Result.success(null);
    }
}
