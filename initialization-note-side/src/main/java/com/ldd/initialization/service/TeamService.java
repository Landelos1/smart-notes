package com.ldd.initialization.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ldd.initialization.domain.TeamSpace;
import com.ldd.initialization.dto.TeamDTO.*;
import com.ldd.initialization.vo.TeamVO.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 团队空间Service接口
 */
public interface TeamService extends IService<TeamSpace> {

    // ========== 团队管理 ==========

    /**
     * 获取用户所属团队列表
     */
    List<TeamSpaceVO> getMyTeams(Long userId);

    List<TeamSpaceVO> getDiscoverableTeams(Long userId);

    /**
     * 创建团队
     */
    TeamSpaceVO createTeam(TeamCreateDTO dto, Long userId);

    /**
     * 通过邀请码加入团队
     */
    TeamSpaceVO joinByInviteCode(String inviteCode, Long userId);

    /**
     * 获取团队详情
     */
    TeamSpaceVO getTeamDetail(Long teamId, Long userId);

    /**
     * 解散团队（仅创建者）
     */
    void dissolveTeam(Long teamId, Long userId);

    /**
     * 退出团队
     */
    void leaveTeam(Long teamId, Long userId);

    /**
     * 重置邀请码
     */
    TeamSpaceVO resetInviteCode(Long teamId, Long userId);

    TeamSpaceVO updateTeam(Long teamId, TeamUpdateDTO dto, Long userId);

    // ========== 成员管理 ==========

    /**
     * 获取团队成员列表
     */
    List<TeamMemberVO> getMembers(Long teamId, Long userId);

    /**
     * 修改成员角色
     */
    void updateMemberRole(Long teamId, Long targetUserId, Integer newRole, Long operatorId);

    /**
     * 移除成员
     */
    void removeMember(Long teamId, Long targetUserId, Long operatorId);

    // ========== 文档管理 ==========

    /**
     * 获取团队文档列表（分页）
     */
    IPage<DocBriefVO> getDocList(Long teamId, int page, int size, String keyword, Long userId);

    /**
     * 创建文档
     */
    DocDetailVO createDoc(Long teamId, DocCreateDTO dto, Long userId);

    /**
     * 获取文档详情
     */
    DocDetailVO getDocDetail(Long teamId, Long docId, Long userId);

    /**
     * 更新文档
     */
    DocDetailVO updateDoc(Long teamId, Long docId, DocUpdateDTO dto, Long userId);

    /**
     * 删除文档
     */
    void deleteDoc(Long teamId, Long docId, Long userId);

    /**
     * 获取编辑锁
     */
    LockInfoVO acquireLock(Long teamId, Long docId, Long userId);

    /**
     * 释放编辑锁
     */
    void releaseLock(Long teamId, Long docId, String lockToken, Long userId);

    /**
     * 获取版本历史
     */
    List<DocVersionVO> getVersionHistory(Long teamId, Long docId, Long userId);

    /**
     * 恢复到指定版本
     */
    DocDetailVO restoreVersion(Long teamId, Long docId, Long eventId, Long userId);

    /**
     * 获取文档动态列表
     */
    List<DocActivityVO> getActivities(Long teamId, Long docId, Long userId);

    // ========== 评论管理 ==========

    /**
     * 获取文档评论列表
     */
    List<DocCommentVO> getComments(Long teamId, Long docId, Integer type, Long userId);

    /**
     * 添加评论
     */
    DocCommentVO addComment(Long teamId, Long docId, CommentCreateDTO dto, Long userId);

    /**
     * 标记评论已解决
     */
    void resolveComment(Long teamId, Long docId, Long commentId, Long userId);

    // ========== AI会话管理 ==========

    /**
     * 获取AI会话列表
     */
    List<AISessionVO> getAISessions(Long teamId, Long userId);

    /**
     * 创建AI会话
     */
    AISessionVO createAISession(Long teamId, AISessionCreateDTO dto, Long userId);

    /**
     * 删除AI会话
     */
    void deleteAISession(Long teamId, Long sessionId, Long userId);

    /**
     * 置顶/取消置顶AI会话
     */
    void togglePinSession(Long teamId, Long sessionId, Long userId);

    /**
     * 获取AI消息列表
     */
    List<AIMessageVO> getAIMessages(Long teamId, Long sessionId, Long userId);

    /**
     * 发送AI消息（同步返回）
     */
    AIMessageVO sendAIMessage(Long teamId, Long sessionId, AIMessageDTO dto, Long userId);

    Flux<String> sendAIMessageStream(Long teamId, Long sessionId, AIMessageDTO dto, Long userId);

    void reindexTeamKnowledgeBase(Long teamId, Long userId);

    // ========== 权限校验 ==========

    /**
     * 检查用户是否是团队成员
     */
    boolean isMember(Long teamId, Long userId);

    /**
     * 获取用户在团队中的角色
     */
    Integer getUserRole(Long teamId, Long userId);

    /**
     * 检查用户是否有指定权限
     */
    void checkPermission(Long teamId, Long userId, Integer requiredRole);
}
