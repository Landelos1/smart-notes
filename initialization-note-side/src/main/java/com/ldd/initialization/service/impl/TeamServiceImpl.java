package com.ldd.initialization.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ldd.initialization.config.exception.BizException;
import com.ldd.initialization.domain.*;
import com.ldd.initialization.dto.TeamDTO.*;
import com.ldd.initialization.mapper.*;
import com.ldd.initialization.result.BizResponseCode;
import com.ldd.initialization.service.ai.TeamVectorService;
import com.ldd.initialization.service.ai.adapter.ConsultantService;
import com.ldd.initialization.service.TeamService;
import com.ldd.initialization.utils.DocumentUtils;
import com.ldd.initialization.vo.TeamVO.*;
import dev.langchain4j.data.document.Document;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 团队空间Service实现类
 */
@Service
@Slf4j
public class TeamServiceImpl extends ServiceImpl<TeamSpaceMapper, TeamSpace> implements TeamService {

    @Autowired
    private TeamMemberMapper memberMapper;
    @Autowired
    private TeamDocMapper docMapper;
    @Autowired
    private TeamDocEventMapper eventMapper;
    @Autowired
    private TeamAISessionMapper sessionMapper;
    @Autowired
    private TeamAIMessageMapper messageMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TeamVectorService teamVectorService;
    @Autowired
    private ConsultantService consultantService;

    @Autowired
    @Qualifier("streamingExecutor")
    private ThreadPoolTaskExecutor streamingExecutor;

    // ========== 团队管理 ==========

    @Override
    public List<TeamSpaceVO> getMyTeams(Long userId) {
        List<TeamSpace> teams = baseMapper.selectTeamsByUserId(userId);
        return teams.stream().map(t -> convertToTeamSpaceVO(t, t.getMyRole())).collect(Collectors.toList());
    }

    @Override
    public List<TeamSpaceVO> getDiscoverableTeams(Long userId) {
        List<TeamSpace> teams = baseMapper.selectDiscoverableTeams(userId);
        return teams.stream().map(t -> convertToTeamSpaceVO(t, null)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TeamSpaceVO createTeam(TeamCreateDTO dto, Long userId) {
        // 创建团队
        TeamSpace team = new TeamSpace();
        BeanUtils.copyProperties(dto, team);
        team.setCreatorId(userId);
        team.setMemberCount(1);
        team.setDocCount(0);
        team.setStatus(1);
        team.setInviteCode(generateInviteCode());
        team.setInviteRole(3); // 默认邀请角色为查看者
        team.setInviteExpireTime(LocalDateTime.now().plusDays(30));
        team.setInviteMaxUses(100);
        team.setInviteUsedCount(0);
        team.setLastActiveTime(LocalDateTime.now());
        this.save(team);

        // 添加创建者为成员
        TeamMember member = new TeamMember();
        member.setTeamId(team.getId());
        member.setUserId(userId);
        member.setRole(0); // 创建者
        member.setStatus(1);
        memberMapper.insert(member);

        log.info("用户 {} 创建团队成功: {}", userId, team.getName());
        return getTeamDetail(team.getId(), userId);
    }

    @Override
    @Transactional
    public TeamSpaceVO joinByInviteCode(String inviteCode, Long userId) {
        TeamSpace team = baseMapper.selectByInviteCode(inviteCode);
        if (team == null) {
            throw new BizException(BizResponseCode.ERROR_1, "邀请码无效或已过期");
        }
        
        // 检查邀请码是否过期
        if (team.getInviteExpireTime() != null && team.getInviteExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException(BizResponseCode.ERROR_1, "邀请码已过期");
        }
        
        // 检查使用次数
        if (team.getInviteMaxUses() != null && team.getInviteUsedCount() >= team.getInviteMaxUses()) {
            throw new BizException(BizResponseCode.ERROR_1, "邀请码使用次数已达上限");
        }
        
        // 检查是否已是成员
        if (memberMapper.existsByTeamIdAndUserId(team.getId(), userId)) {
            throw new BizException(BizResponseCode.ERROR_1, "您已是该团队成员");
        }
        
        // 添加成员
        TeamMember member = new TeamMember();
        member.setTeamId(team.getId());
        member.setUserId(userId);
        member.setRole(team.getInviteRole() != null ? team.getInviteRole() : 3);
        member.setStatus(1);
        memberMapper.insert(member);
        
        // 更新统计
        baseMapper.updateMemberCount(team.getId(), 1);
        baseMapper.incrementInviteUsedCount(team.getId());
        baseMapper.updateLastActiveTime(team.getId());

        log.info("用户 {} 通过邀请码加入团队: {}", userId, team.getName());
        return getTeamDetail(team.getId(), userId);
    }

    @Override
    public TeamSpaceVO getTeamDetail(Long teamId, Long userId) {
        TeamSpace team = this.getById(teamId);
        if (team == null || team.getStatus() != 1) {
            throw new BizException(BizResponseCode.ERROR_1, "团队不存在");
        }
        Integer myRole = getUserRole(teamId, userId);
        if (myRole == null) {
            throw new BizException(BizResponseCode.ERROR_1, "您不是该团队成员");
        }
        return convertToTeamSpaceVO(team, myRole);
    }

    @Override
    @Transactional
    public void dissolveTeam(Long teamId, Long userId) {
        checkPermission(teamId, userId, 0); // 仅创建者
        
        // 软删除团队
        TeamSpace team = new TeamSpace();
        team.setId(teamId);
        team.setStatus(0);
        this.updateById(team);
        
        // 软删除所有成员
        LambdaUpdateWrapper<TeamMember> memberWrapper = new LambdaUpdateWrapper<>();
        memberWrapper.eq(TeamMember::getTeamId, teamId).set(TeamMember::getStatus, 0);
        memberMapper.update(null, memberWrapper);

        log.info("用户 {} 解散团队: {}", userId, teamId);
    }

    @Override
    @Transactional
    public void leaveTeam(Long teamId, Long userId) {
        Integer role = getUserRole(teamId, userId);
        if (role == null) {
            throw new BizException(BizResponseCode.ERROR_1, "您不是该团队成员");
        }
        if (role == 0) {
            throw new BizException(BizResponseCode.ERROR_1, "创建者无法退出团队，请先转让或解散");
        }
        
        // 软删除成员
        LambdaUpdateWrapper<TeamMember> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(TeamMember::getTeamId, teamId)
               .eq(TeamMember::getUserId, userId)
               .set(TeamMember::getStatus, 0);
        memberMapper.update(null, wrapper);
        
        baseMapper.updateMemberCount(teamId, -1);
        log.info("用户 {} 退出团队: {}", userId, teamId);
    }

    @Override
    @Transactional
    public TeamSpaceVO resetInviteCode(Long teamId, Long userId) {
        checkPermission(teamId, userId, 1); // 管理员以上
        
        TeamSpace team = new TeamSpace();
        team.setId(teamId);
        team.setInviteCode(generateInviteCode());
        team.setInviteExpireTime(LocalDateTime.now().plusDays(30));
        team.setInviteUsedCount(0);
        this.updateById(team);

        return getTeamDetail(teamId, userId);
    }

    @Override
    @Transactional
    public TeamSpaceVO updateTeam(Long teamId, TeamUpdateDTO dto, Long userId) {
        checkPermission(teamId, userId, 1); // 管理员以上

        TeamSpace exist = this.getById(teamId);
        if (exist == null || exist.getStatus() == null || exist.getStatus() != 1) {
            throw new BizException(BizResponseCode.ERROR_1, "团队不存在");
        }

        TeamSpace update = new TeamSpace();
        update.setId(teamId);

        if (StrUtil.isNotBlank(dto.getName())) {
            update.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            update.setDescription(dto.getDescription());
        }
        if (dto.getAvatarUrl() != null) {
            update.setAvatarUrl(dto.getAvatarUrl());
        }
        if (dto.getIsPublic() != null) {
            update.setIsPublic(dto.getIsPublic());
        }
        if (dto.getInviteRole() != null) {
            update.setInviteRole(dto.getInviteRole());
        }
        if (dto.getInviteExpireTime() != null) {
            update.setInviteExpireTime(dto.getInviteExpireTime());
        }
        if (dto.getInviteMaxUses() != null) {
            update.setInviteMaxUses(dto.getInviteMaxUses());
        }

        this.updateById(update);
        baseMapper.updateLastActiveTime(teamId);

        return getTeamDetail(teamId, userId);
    }

    // ========== 成员管理 ==========

    @Override
    public List<TeamMemberVO> getMembers(Long teamId, Long userId) {
        checkPermission(teamId, userId, 3); // 所有成员可查看
        
        List<TeamMember> members = memberMapper.selectMembersWithUser(teamId);
        return members.stream().map(this::convertToTeamMemberVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateMemberRole(Long teamId, Long targetUserId, Integer newRole, Long operatorId) {
        checkPermission(teamId, operatorId, 1); // 管理员以上
        
        Integer targetRole = getUserRole(teamId, targetUserId);
        if (targetRole == null) {
            throw new BizException(BizResponseCode.ERROR_1, "目标用户不是团队成员");
        }
        if (targetRole == 0) {
            throw new BizException(BizResponseCode.ERROR_1, "无法修改创建者角色");
        }
        
        Integer operatorRole = getUserRole(teamId, operatorId);
        if (operatorRole > targetRole) {
            throw new BizException(BizResponseCode.ERROR_1, "无法修改权限比您高的成员");
        }
        
        LambdaUpdateWrapper<TeamMember> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(TeamMember::getTeamId, teamId)
               .eq(TeamMember::getUserId, targetUserId)
               .set(TeamMember::getRole, newRole);
        memberMapper.update(null, wrapper);
    }

    @Override
    @Transactional
    public void removeMember(Long teamId, Long targetUserId, Long operatorId) {
        checkPermission(teamId, operatorId, 1); // 管理员以上
        
        Integer targetRole = getUserRole(teamId, targetUserId);
        if (targetRole == null) {
            throw new BizException(BizResponseCode.ERROR_1, "目标用户不是团队成员");
        }
        if (targetRole == 0) {
            throw new BizException(BizResponseCode.ERROR_1, "无法移除创建者");
        }
        
        LambdaUpdateWrapper<TeamMember> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(TeamMember::getTeamId, teamId)
               .eq(TeamMember::getUserId, targetUserId)
               .set(TeamMember::getStatus, 0);
        memberMapper.update(null, wrapper);
        
        baseMapper.updateMemberCount(teamId, -1);
    }

    // ========== 文档管理 ==========

    @Override
    public IPage<DocBriefVO> getDocList(Long teamId, int page, int size, String keyword, Long userId) {
        checkPermission(teamId, userId, 3);
        
        Page<TeamDoc> pageObj = new Page<>(page, size);
        IPage<TeamDoc> docPage = docMapper.selectDocListByPage(pageObj, teamId, keyword);
        
        return docPage.convert(this::convertToDocBriefVO);
    }

    @Override
    @Transactional
    public DocDetailVO createDoc(Long teamId, DocCreateDTO dto, Long userId) {
        checkPermission(teamId, userId, 2); // 编辑者以上
        
        TeamDoc doc = new TeamDoc();
        doc.setTeamId(teamId);
        doc.setTitle(dto.getTitle());
        doc.setContentMd(dto.getContentMd());
        doc.setContentHtml(dto.getContentHtml());
        doc.setStatus(1);
        doc.setIsPinned(dto.getIsPinned() != null && dto.getIsPinned());
        doc.setVersion(1);
        doc.setCreatedBy(userId);
        doc.setUpdatedBy(userId);
        docMapper.insert(doc);
        
        baseMapper.updateDocCount(teamId, 1);
        baseMapper.updateLastActiveTime(teamId);

        saveActivityEvent(teamId, doc.getId(), userId, "created", "创建了文档");

        rebuildTeamDocIndex(teamId, doc.getId(), doc.getTitle(), doc.getContentMd());

        return getDocDetail(teamId, doc.getId(), userId);
    }

    @Override
    public DocDetailVO getDocDetail(Long teamId, Long docId, Long userId) {
        checkPermission(teamId, userId, 3);
        
        // 清理过期锁
        docMapper.clearExpiredLock(docId);
        
        TeamDoc doc = docMapper.selectDocWithUserInfo(docId);
        if (doc == null || !doc.getTeamId().equals(teamId)) {
            throw new BizException(BizResponseCode.ERROR_1, "文档不存在");
        }
        return convertToDocDetailVO(doc);
    }

    @Override
    @Transactional
    public DocDetailVO updateDoc(Long teamId, Long docId, DocUpdateDTO dto, Long userId) {
        checkPermission(teamId, userId, 2);
        
        TeamDoc doc = docMapper.selectById(docId);
        if (doc == null || !doc.getTeamId().equals(teamId) || doc.getStatus() != 1) {
            throw new BizException(BizResponseCode.ERROR_1, "文档不存在");
        }
        
        // 检查锁
        if (doc.getLockedBy() != null && !doc.getLockedBy().equals(userId)) {
            if (doc.getLockExpireTime() != null && doc.getLockExpireTime().isAfter(LocalDateTime.now())) {
                throw new BizException(BizResponseCode.ERROR_1, "文档正在被其他人编辑");
            }
        }
        
        // 保存版本快照
        saveVersionSnapshot(doc, userId, dto.getMessage());
        
        // 更新文档
        TeamDoc update = new TeamDoc();
        update.setId(docId);
        if (StrUtil.isNotBlank(dto.getTitle())) {
            update.setTitle(dto.getTitle());
        }
        if (dto.getContentMd() != null) {
            update.setContentMd(dto.getContentMd());
        }
        if (dto.getContentHtml() != null) {
            update.setContentHtml(dto.getContentHtml());
        }
        if (dto.getIsPinned() != null) {
            update.setIsPinned(dto.getIsPinned());
        }
        update.setVersion(doc.getVersion() + 1);
        update.setUpdatedBy(userId);
        docMapper.updateById(update);
        
        baseMapper.updateLastActiveTime(teamId);

        saveActivityEvent(teamId, docId, userId, "updated", "更新了文档");

        String finalTitle = StrUtil.isNotBlank(dto.getTitle()) ? dto.getTitle() : doc.getTitle();
        String finalContentMd = dto.getContentMd() != null ? dto.getContentMd() : doc.getContentMd();
        rebuildTeamDocIndex(teamId, docId, finalTitle, finalContentMd);
        return getDocDetail(teamId, docId, userId);
    }

    @Override
    @Transactional
    public void deleteDoc(Long teamId, Long docId, Long userId) {
        checkPermission(teamId, userId, 2);
        
        TeamDoc doc = docMapper.selectById(docId);
        if (doc == null || !doc.getTeamId().equals(teamId)) {
            throw new BizException(BizResponseCode.ERROR_1, "文档不存在");
        }
        
        TeamDoc update = new TeamDoc();
        update.setId(docId);
        update.setStatus(0);
        docMapper.updateById(update);
        
        baseMapper.updateDocCount(teamId, -1);

        teamVectorService.deleteVectorsByTeamDocId(teamId, docId);
    }

    @Override
    public LockInfoVO acquireLock(Long teamId, Long docId, Long userId) {
        checkPermission(teamId, userId, 2);
        requireActiveDoc(teamId, docId);
        
        String token = UUID.randomUUID().toString();
        int rows = docMapper.acquireLock(docId, userId, token);
        
        LockInfoVO vo = new LockInfoVO();
        if (rows > 0) {
            vo.setSuccess(true);
            vo.setLockToken(token);
            vo.setLockExpireTime(LocalDateTime.now().plusMinutes(5));
            vo.setMessage("获取锁成功");

            saveActivityEvent(teamId, docId, userId, "lock_acquired", "开始编辑");
        } else {
            vo.setSuccess(false);
            vo.setMessage("文档正在被其他人编辑");
            // 查询锁定人信息
            TeamDoc doc = docMapper.selectDocWithUserInfo(docId);
            if (doc != null && doc.getLockedBy() != null) {
                vo.setLockedByUser(convertToSimpleUserVO(doc.getLockedByUser()));
                vo.setLockExpireTime(doc.getLockExpireTime());
            }
        }
        return vo;
    }

    @Override
    public void releaseLock(Long teamId, Long docId, String lockToken, Long userId) {
        checkPermission(teamId, userId, 2);
        requireActiveDoc(teamId, docId);
        docMapper.releaseLock(docId, userId, lockToken);

        saveActivityEvent(teamId, docId, userId, "lock_released", "结束编辑");
    }

    @Override
    public List<DocVersionVO> getVersionHistory(Long teamId, Long docId, Long userId) {
        checkPermission(teamId, userId, 3);
        requireActiveDoc(teamId, docId);
        
        List<TeamDocEvent> events = eventMapper.selectVersionHistory(docId);
        return events.stream().map(this::convertToDocVersionVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DocDetailVO restoreVersion(Long teamId, Long docId, Long eventId, Long userId) {
        checkPermission(teamId, userId, 2);
        TeamDoc doc = requireActiveDoc(teamId, docId);
        if (doc.getLockedBy() != null && !doc.getLockedBy().equals(userId)
                && doc.getLockExpireTime() != null && doc.getLockExpireTime().isAfter(LocalDateTime.now())) {
            throw new BizException(BizResponseCode.ERROR_1, "文档正在被其他人编辑");
        }
        
        TeamDocEvent event = eventMapper.selectById(eventId);
        if (event == null || !event.getDocId().equals(docId) || event.getEventType() != 1) {
            throw new BizException(BizResponseCode.ERROR_1, "版本不存在");
        }
        
        // 保存当前版本
        saveVersionSnapshot(doc, userId, "恢复前备份");
        
        // 恢复内容
        TeamDoc update = new TeamDoc();
        update.setId(docId);
        update.setContentMd(event.getContent());
        update.setVersion(doc.getVersion() + 1);
        update.setUpdatedBy(userId);
        docMapper.updateById(update);

        saveActivityEvent(teamId, docId, userId, "restored", "恢复了历史版本");

        rebuildTeamDocIndex(teamId, docId, doc.getTitle(), event.getContent());
        
        return getDocDetail(teamId, docId, userId);
    }

    @Override
    public List<DocActivityVO> getActivities(Long teamId, Long docId, Long userId) {
        checkPermission(teamId, userId, 3);
        requireActiveDoc(teamId, docId);

        List<TeamDocEvent> events = eventMapper.selectActivities(docId);
        return events.stream().map(this::convertToDocActivityVO).collect(Collectors.toList());
    }

    // ========== 评论管理 ==========

    @Override
    public List<DocCommentVO> getComments(Long teamId, Long docId, Integer type, Long userId) {
        checkPermission(teamId, userId, 3);
        requireActiveDoc(teamId, docId);
        
        List<TeamDocEvent> events = eventMapper.selectComments(docId);
        return events.stream()
                .filter(e -> type == null || type.equals(parseCommentType(e.getMetadata())))
                .map(this::convertToDocCommentVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DocCommentVO addComment(Long teamId, Long docId, CommentCreateDTO dto, Long userId) {
        checkPermission(teamId, userId, 3);
        requireActiveDoc(teamId, docId);
        if (dto.getParentId() != null) {
            TeamDocEvent parent = eventMapper.selectById(dto.getParentId());
            if (parent == null || !teamId.equals(parent.getTeamId()) || !docId.equals(parent.getDocId())
                    || !Integer.valueOf(2).equals(parent.getEventType())) {
                throw new BizException(BizResponseCode.ERROR_1, "回复的评论不存在于当前文档");
            }
        }
        
        TeamDocEvent event = new TeamDocEvent();
        event.setTeamId(teamId);
        event.setDocId(docId);
        event.setUserId(userId);
        event.setEventType(2); // 评论/批注
        event.setParentId(dto.getParentId());
        event.setContent(dto.getContent());
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", dto.getType() != null ? dto.getType() : 1);
        if (StrUtil.isNotBlank(dto.getQuote())) {
            metadata.put("quote", dto.getQuote());
        }
        metadata.put("resolved", false);
        event.setMetadata(toJson(metadata));
        
        eventMapper.insert(event);

        saveActivityEvent(teamId, docId, userId, "commented", "发表了评论");
        
        baseMapper.updateLastActiveTime(teamId);
        return convertToDocCommentVO(event);
    }

    @Override
    @Transactional
    public void resolveComment(Long teamId, Long docId, Long commentId, Long userId) {
        checkPermission(teamId, userId, 2);
        requireActiveDoc(teamId, docId);
        
        TeamDocEvent event = eventMapper.selectById(commentId);
        if (event == null || !event.getDocId().equals(docId) || event.getEventType() != 2) {
            throw new BizException(BizResponseCode.ERROR_1, "评论不存在");
        }
        
        Map<String, Object> metadata = parseMetadata(event.getMetadata());
        metadata.put("resolved", true);
        
        TeamDocEvent update = new TeamDocEvent();
        update.setId(commentId);
        update.setMetadata(toJson(metadata));
        eventMapper.updateById(update);
    }

    // ========== AI会话管理 ==========

    @Override
    public List<AISessionVO> getAISessions(Long teamId, Long userId) {
        checkPermission(teamId, userId, 3);
        
        List<TeamAISession> sessions = sessionMapper.selectSessionsByTeamId(teamId);
        return sessions.stream().map(this::convertToAISessionVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AISessionVO createAISession(Long teamId, AISessionCreateDTO dto, Long userId) {
        checkPermission(teamId, userId, 3);
        
        TeamAISession session = new TeamAISession();
        session.setTeamId(teamId);
        session.setUserId(userId);
        session.setTitle(StrUtil.isNotBlank(dto.getTitle()) ? dto.getTitle() : "新会话");
        session.setIsPinned(false);
        session.setStatus(1);
        sessionMapper.insert(session);
        
        return convertToAISessionVO(session);
    }

    @Override
    @Transactional
    public void deleteAISession(Long teamId, Long sessionId, Long userId) {
        checkPermission(teamId, userId, 3);
        
        TeamAISession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getTeamId().equals(teamId)) {
            throw new BizException(BizResponseCode.ERROR_1, "会话不存在");
        }
        
        TeamAISession update = new TeamAISession();
        update.setId(sessionId);
        update.setStatus(0);
        sessionMapper.updateById(update);
    }

    @Override
    @Transactional
    public void togglePinSession(Long teamId, Long sessionId, Long userId) {
        checkPermission(teamId, userId, 3);
        
        TeamAISession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getTeamId().equals(teamId)) {
            throw new BizException(BizResponseCode.ERROR_1, "会话不存在");
        }
        
        TeamAISession update = new TeamAISession();
        update.setId(sessionId);
        update.setIsPinned(!Boolean.TRUE.equals(session.getIsPinned()));
        sessionMapper.updateById(update);
    }

    @Override
    public List<AIMessageVO> getAIMessages(Long teamId, Long sessionId, Long userId) {
        checkPermission(teamId, userId, 3);
        TeamAISession session = sessionMapper.selectById(sessionId);
        if (session == null || !teamId.equals(session.getTeamId()) || !Integer.valueOf(1).equals(session.getStatus())) {
            throw new BizException(BizResponseCode.ERROR_1, "会话不存在");
        }
        
        List<TeamAIMessage> messages = messageMapper.selectMessagesBySessionId(sessionId);
        return messages.stream().map(this::convertToAIMessageVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AIMessageVO sendAIMessage(Long teamId, Long sessionId, AIMessageDTO dto, Long userId) {
        checkPermission(teamId, userId, 3);
        
        TeamAISession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getTeamId().equals(teamId) || session.getStatus() == null || session.getStatus() != 1) {
            throw new BizException(BizResponseCode.ERROR_1, "会话不存在");
        }

        maybeUpdateSessionTitleFromFirstUserMessage(session, dto.getContent());
         
        // 保存用户消息
        TeamAIMessage userMsg = new TeamAIMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setUserId(userId);
        userMsg.setRole(1); // user
        userMsg.setContent(dto.getContent());
        messageMapper.insert(userMsg);

        TeamKnowledgeBaseContext teamKbContext = null;
        List<TeamVectorService.TeamDocChunkMatch> matches = null;
        String enhancedMessage;
        List<CitationVO> citations;
        if (isTeamKnowledgeBaseOverviewQuery(dto.getContent())) {
            teamKbContext = buildTeamKnowledgeBaseOverview(teamId, dto.getContent());
            enhancedMessage = teamKbContext.getEnhancedMessage();
            citations = teamKbContext.getCitations();
        } else {
            matches = searchTeamDocMatches(teamId, dto.getContent());
            if (matches == null || matches.isEmpty()) {
                teamKbContext = buildTeamKnowledgeBaseOverview(teamId, dto.getContent());
                enhancedMessage = teamKbContext.getEnhancedMessage();
                citations = teamKbContext.getCitations();
            } else {
                enhancedMessage = buildTeamEnhancedMessage(dto.getContent(), matches);
                citations = buildCitations(matches);
            }
        }

        String memoryId = "team_" + teamId + "_session_" + sessionId;
        String aiResponse;
        try {
            aiResponse = consultantService.simpleChat(memoryId, enhancedMessage);
        } catch (Exception e) {
            log.error("团队 {} 会话 {} 调用AI失败: {}", teamId, sessionId, e.getMessage(), e);
            aiResponse = "抱歉，AI服务暂时不可用，请稍后再试。";
        }
         
        // 保存AI响应
        TeamAIMessage aiMsg = new TeamAIMessage();
        aiMsg.setSessionId(sessionId);
        aiMsg.setUserId(userId);
        aiMsg.setRole(2); // assistant
        aiMsg.setContent(aiResponse);
        aiMsg.setCitations(toJson(citations));
        messageMapper.insert(aiMsg);
         
        // 更新会话最后消息
        TeamAISession sessionUpdate = new TeamAISession();
        sessionUpdate.setId(sessionId);
        sessionUpdate.setLastMessage(aiResponse.length() > 100 ? aiResponse.substring(0, 100) : aiResponse);
        sessionMapper.updateById(sessionUpdate);
         
        baseMapper.updateLastActiveTime(teamId);
        return convertToAIMessageVO(aiMsg);
    }

    @Override
    public Flux<String> sendAIMessageStream(Long teamId, Long sessionId, AIMessageDTO dto, Long userId) {
        checkPermission(teamId, userId, 3);

        TeamAISession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getTeamId().equals(teamId) || session.getStatus() == null || session.getStatus() != 1) {
            return Flux.error(new BizException(BizResponseCode.ERROR_1, "会话不存在"));
        }

        maybeUpdateSessionTitleFromFirstUserMessage(session, dto.getContent());

        TeamAIMessage userMsg = new TeamAIMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setUserId(userId);
        userMsg.setRole(1);
        userMsg.setContent(dto.getContent());
        messageMapper.insert(userMsg);

        TeamKnowledgeBaseContext teamKbContext = null;
        List<TeamVectorService.TeamDocChunkMatch> matches = null;
        String enhancedMessage;
        List<CitationVO> citations;
        if (isTeamKnowledgeBaseOverviewQuery(dto.getContent())) {
            teamKbContext = buildTeamKnowledgeBaseOverview(teamId, dto.getContent());
            enhancedMessage = teamKbContext.getEnhancedMessage();
            citations = teamKbContext.getCitations();
        } else {
            matches = searchTeamDocMatches(teamId, dto.getContent());
            if (matches == null || matches.isEmpty()) {
                teamKbContext = buildTeamKnowledgeBaseOverview(teamId, dto.getContent());
                enhancedMessage = teamKbContext.getEnhancedMessage();
                citations = teamKbContext.getCitations();
            } else {
                enhancedMessage = buildTeamEnhancedMessage(dto.getContent(), matches);
                citations = buildCitations(matches);
            }
        }

        String memoryId = "team_" + teamId + "_session_" + sessionId;
        StringBuilder assistantContent = new StringBuilder();

        return consultantService.streamChat(memoryId, enhancedMessage)
                .subscribeOn(Schedulers.fromExecutor(streamingExecutor))
                .doOnNext(assistantContent::append)
                .map(chunk -> chunk.replace("\n", "[NL]"))
                .doOnComplete(() -> {
                    String aiResponse = assistantContent.toString();

                    TeamAIMessage aiMsg = new TeamAIMessage();
                    aiMsg.setSessionId(sessionId);
                    aiMsg.setUserId(userId);
                    aiMsg.setRole(2);
                    aiMsg.setContent(aiResponse);
                    aiMsg.setCitations(toJson(citations));
                    messageMapper.insert(aiMsg);

                    TeamAISession sessionUpdate = new TeamAISession();
                    sessionUpdate.setId(sessionId);
                    sessionUpdate.setLastMessage(aiResponse.length() > 100 ? aiResponse.substring(0, 100) : aiResponse);
                    sessionMapper.updateById(sessionUpdate);
                    baseMapper.updateLastActiveTime(teamId);
                })
                .onErrorResume(throwable -> {
                    log.error("团队 {} 会话 {} 流式调用AI失败: {}", teamId, sessionId, throwable.getMessage(), throwable);
                    String fallback = "抱歉，AI服务暂时不可用，请稍后再试。";

                    TeamAIMessage aiMsg = new TeamAIMessage();
                    aiMsg.setSessionId(sessionId);
                    aiMsg.setUserId(userId);
                    aiMsg.setRole(2);
                    aiMsg.setContent(fallback);
                    aiMsg.setCitations(toJson(new ArrayList<>()));
                    messageMapper.insert(aiMsg);

                    TeamAISession sessionUpdate = new TeamAISession();
                    sessionUpdate.setId(sessionId);
                    sessionUpdate.setLastMessage(fallback);
                    sessionMapper.updateById(sessionUpdate);
                    baseMapper.updateLastActiveTime(teamId);

                    return Flux.just(fallback);
                });
    }

    @Override
    @Transactional
    public void reindexTeamKnowledgeBase(Long teamId, Long userId) {
        checkPermission(teamId, userId, 1);

        LambdaQueryWrapper<TeamDoc> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeamDoc::getTeamId, teamId)
                .eq(TeamDoc::getStatus, 1);
        List<TeamDoc> docs = docMapper.selectList(wrapper);

        for (TeamDoc doc : docs) {
            rebuildTeamDocIndex(teamId, doc.getId(), doc.getTitle(), doc.getContentMd());
        }
    }

    // ========== 权限校验 ==========

    @Override
    public boolean isMember(Long teamId, Long userId) {
        return memberMapper.existsByTeamIdAndUserId(teamId, userId);
    }

    @Override
    public Integer getUserRole(Long teamId, Long userId) {
        return memberMapper.selectUserRole(teamId, userId);
    }

    @Override
    public void checkPermission(Long teamId, Long userId, Integer requiredRole) {
        Integer userRole = getUserRole(teamId, userId);
        if (userRole == null) {
            throw new BizException(BizResponseCode.ERROR_1, "您不是该团队成员");
        }
        if (userRole > requiredRole) {
            throw new BizException(BizResponseCode.ERROR_1, "权限不足");
        }
    }

    // ========== 私有方法 ==========

    private String generateInviteCode() {
        return "TEAM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void saveActivityEvent(Long teamId, Long docId, Long userId, String action, String message) {
        TeamDocEvent event = new TeamDocEvent();
        event.setTeamId(teamId);
        event.setDocId(docId);
        event.setUserId(userId);
        event.setEventType(3);
        event.setContent(null);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("action", action);
        metadata.put("message", message);
        event.setMetadata(toJson(metadata));

        eventMapper.insert(event);
    }

    private void saveVersionSnapshot(TeamDoc doc, Long userId, String message) {
        TeamDocEvent event = new TeamDocEvent();
        event.setTeamId(doc.getTeamId());
        event.setDocId(doc.getId());
        event.setUserId(userId);
        event.setEventType(1); // 版本快照
        event.setContent(doc.getContentMd());
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("version", doc.getVersion());
        metadata.put("title", doc.getTitle());
        metadata.put("message", StrUtil.isNotBlank(message) ? message : "版本 " + doc.getVersion());
        event.setMetadata(toJson(metadata));
        
        eventMapper.insert(event);
    }

    private TeamSpaceVO convertToTeamSpaceVO(TeamSpace team, Integer myRole) {
        TeamSpaceVO vo = new TeamSpaceVO();
        BeanUtils.copyProperties(team, vo);
        vo.setMyRole(myRole);
        // 非管理员隐藏邀请码
        if (myRole != null && myRole > 1) {
            vo.setInviteCode(null);
        }
        return vo;
    }

    private TeamMemberVO convertToTeamMemberVO(TeamMember member) {
        TeamMemberVO vo = new TeamMemberVO();
        BeanUtils.copyProperties(member, vo);
        // 优先使用关联查询出的临时字段
        if (member.getUsername() != null) {
            vo.setUsername(member.getUsername());
            vo.setAvatarUrl(member.getAvatarUrl());
            vo.setEmail(member.getEmail());
        } else if (member.getUser() != null) {
            vo.setUsername(member.getUser().getUsername());
            vo.setAvatarUrl(member.getUser().getAvatarUrl());
            vo.setEmail(member.getUser().getPhone());
        }
        return vo;
    }

    private TeamDoc requireActiveDoc(Long teamId, Long docId) {
        TeamDoc doc = docMapper.selectById(docId);
        if (doc == null || !teamId.equals(doc.getTeamId()) || !Integer.valueOf(1).equals(doc.getStatus())) {
            throw new BizException(BizResponseCode.ERROR_1, "文档不存在");
        }
        return doc;
    }

    private DocBriefVO convertToDocBriefVO(TeamDoc doc) {
        DocBriefVO vo = new DocBriefVO();
        BeanUtils.copyProperties(doc, vo);
        if (doc.getContentMd() != null) {
            vo.setExcerpt(doc.getContentMd().length() > 100 ? 
                doc.getContentMd().substring(0, 100) + "..." : doc.getContentMd());
        }
        if (doc.getUpdatedByUser() != null) {
            vo.setUpdatedByUser(convertToSimpleUserVO(doc.getUpdatedByUser()));
        }
        if (doc.getLockedByUser() != null) {
            vo.setLockedByUser(convertToSimpleUserVO(doc.getLockedByUser()));
        }
        return vo;
    }

    private DocDetailVO convertToDocDetailVO(TeamDoc doc) {
        DocDetailVO vo = new DocDetailVO();
        BeanUtils.copyProperties(doc, vo);
        if (doc.getCreatedByUser() != null) {
            vo.setCreatedByUser(convertToSimpleUserVO(doc.getCreatedByUser()));
        }
        if (doc.getUpdatedByUser() != null) {
            vo.setUpdatedByUser(convertToSimpleUserVO(doc.getUpdatedByUser()));
        }
        if (doc.getLockedByUser() != null) {
            vo.setLockedByUser(convertToSimpleUserVO(doc.getLockedByUser()));
        }
        return vo;
    }

    private SimpleUserVO convertToSimpleUserVO(User user) {
        if (user == null) return null;
        SimpleUserVO vo = new SimpleUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setAvatarUrl(user.getAvatarUrl());
        return vo;
    }

    private DocVersionVO convertToDocVersionVO(TeamDocEvent event) {
        DocVersionVO vo = new DocVersionVO();
        vo.setId(event.getId());
        vo.setCreateTime(event.getCreateTime());
        
        Map<String, Object> metadata = parseMetadata(event.getMetadata());
        Object versionObj = metadata.get("version");
        vo.setVersion(versionObj instanceof Number ? ((Number) versionObj).intValue() : null);
        vo.setMessage((String) metadata.get("message"));
        
        // 优先使用关联查询出的临时字段构建用户信息
        if (event.getUsername() != null) {
            SimpleUserVO userVO = new SimpleUserVO();
            userVO.setId(event.getUserId());
            userVO.setUsername(event.getUsername());
            userVO.setAvatarUrl(event.getAvatarUrl());
            vo.setUser(userVO);
        } else if (event.getUser() != null) {
            vo.setUser(convertToSimpleUserVO(event.getUser()));
        }
        return vo;
    }

    private DocActivityVO convertToDocActivityVO(TeamDocEvent event) {
        DocActivityVO vo = new DocActivityVO();
        vo.setId(event.getId());
        vo.setCreateTime(event.getCreateTime());

        Map<String, Object> metadata = parseMetadata(event.getMetadata());
        vo.setAction((String) metadata.get("action"));
        vo.setMessage((String) metadata.get("message"));

        if (event.getUsername() != null) {
            SimpleUserVO userVO = new SimpleUserVO();
            userVO.setId(event.getUserId());
            userVO.setUsername(event.getUsername());
            userVO.setAvatarUrl(event.getAvatarUrl());
            vo.setUser(userVO);
        } else if (event.getUser() != null) {
            vo.setUser(convertToSimpleUserVO(event.getUser()));
        }
        return vo;
    }

    private DocCommentVO convertToDocCommentVO(TeamDocEvent event) {
        DocCommentVO vo = new DocCommentVO();
        vo.setId(event.getId());
        vo.setContent(event.getContent());
        vo.setParentId(event.getParentId());
        vo.setCreateTime(event.getCreateTime());
        
        Map<String, Object> metadata = parseMetadata(event.getMetadata());
        Object typeObj = metadata.get("type");
        vo.setType(typeObj instanceof Number ? ((Number) typeObj).intValue() : 1);
        vo.setQuote((String) metadata.get("quote"));
        vo.setResolved((Boolean) metadata.get("resolved"));
        
        // 优先使用关联查询出的临时字段构建用户信息
        if (event.getUsername() != null) {
            SimpleUserVO userVO = new SimpleUserVO();
            userVO.setId(event.getUserId());
            userVO.setUsername(event.getUsername());
            userVO.setAvatarUrl(event.getAvatarUrl());
            vo.setUser(userVO);
        } else if (event.getUser() != null) {
            vo.setUser(convertToSimpleUserVO(event.getUser()));
        }
        return vo;
    }

    private AISessionVO convertToAISessionVO(TeamAISession session) {
        AISessionVO vo = new AISessionVO();
        BeanUtils.copyProperties(session, vo);
        return vo;
    }

    private AIMessageVO convertToAIMessageVO(TeamAIMessage msg) {
        AIMessageVO vo = new AIMessageVO();
        BeanUtils.copyProperties(msg, vo);
        if (StrUtil.isNotBlank(msg.getCitations())) {
            try {
                vo.setCitations(objectMapper.readValue(msg.getCitations(), 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, CitationVO.class)));
            } catch (Exception e) {
                vo.setCitations(new ArrayList<>());
            }
        }
        return vo;
    }

    private Integer parseCommentType(String metadata) {
        Map<String, Object> map = parseMetadata(metadata);
        Object type = map.get("type");
        return type instanceof Integer ? (Integer) type : 1;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMetadata(String metadata) {
        if (StrUtil.isBlank(metadata)) return new HashMap<>();
        try {
            return objectMapper.readValue(metadata, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private void rebuildTeamDocIndex(Long teamId, Long docId, String title, String contentMd) {
        try {
            teamVectorService.deleteVectorsByTeamDocId(teamId, docId);
            if (StrUtil.isBlank(contentMd)) {
                return;
            }

            Document root = DocumentUtils.createDocumentWithMetadata(contentMd, Collections.emptyMap());
            List<Document> chunks = DocumentUtils.splitDocuments(null, List.of(root));
            teamVectorService.addDocumentsToTeam(teamId, docId, title, chunks);
        } catch (Exception e) {
            log.error("重建团队 {} 文档 {} 向量索引失败: {}", teamId, docId, e.getMessage(), e);
            throw e;
        }
    }

    private List<TeamVectorService.TeamDocChunkMatch> searchTeamDocMatches(Long teamId, String query) {
        List<TeamVectorService.TeamDocChunkMatch> matches = teamVectorService.searchInTeam(teamId, query, 5, 0.7);
        if (matches == null || matches.isEmpty()) {
            matches = teamVectorService.searchInTeam(teamId, query, 8, 0.3);
        }
        return matches;
    }

    private boolean isTeamKnowledgeBaseOverviewQuery(String query) {
        if (StrUtil.isBlank(query)) {
            return false;
        }
        String q = query.trim();
        return q.contains("总结")
                || q.contains("概览")
                || q.contains("总览")
                || q.contains("整体")
                || q.contains("全部")
                || q.contains("所有")
                || q.contains("有哪些")
                || q.contains("目录")
                || q.contains("知识库")
                || q.contains("文档") && (q.contains("汇总") || q.contains("总的") || q.contains("整体"));
    }

    private TeamKnowledgeBaseContext buildTeamKnowledgeBaseOverview(Long teamId, String userQuestion) {
        TeamKnowledgeBaseContext ctx = new TeamKnowledgeBaseContext();
        ctx.setCitations(new ArrayList<>());

        LambdaQueryWrapper<TeamDoc> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeamDoc::getTeamId, teamId)
                .eq(TeamDoc::getStatus, 1)
                .orderByDesc(TeamDoc::getIsPinned)
                .orderByDesc(TeamDoc::getUpdateTime)
                .last("LIMIT 20");
        List<TeamDoc> docs = docMapper.selectList(wrapper);

        if (docs == null || docs.isEmpty()) {
            ctx.setEnhancedMessage("【用户问题】\n" + userQuestion
                    + "\n\n【团队知识库概览】\n团队知识库目前没有可用文档。请先创建/导入团队文档后再提问。\n\n请先给出一个通用回答，并提示用户补充团队文档。");
            return ctx;
        }

        int maxTotalChars = 8000;
        int maxExcerptChars = 500;

        StringBuilder context = new StringBuilder();
        context.append("【团队知识库概览资料】\n");
        context.append("以下是团队知识库中的部分文档标题与摘要片段（按置顶/最近更新排序）。请优先基于这些资料回答。\n\n");

        int used = context.length();
        int idx = 1;
        for (TeamDoc d : docs) {
            if (d == null) {
                continue;
            }
            String title = StrUtil.isNotBlank(d.getTitle()) ? d.getTitle() : ("文档" + d.getId());
            String excerpt = d.getContentMd();
            if (excerpt == null) {
                excerpt = "";
            }
            excerpt = excerpt.trim();
            if (excerpt.length() > maxExcerptChars) {
                excerpt = excerpt.substring(0, maxExcerptChars);
            }

            String block = "【文档" + idx + "】\n" +
                    "docId: " + d.getId() + "\n" +
                    "title: " + title + "\n" +
                    "excerpt: " + excerpt + "\n\n---\n\n";
            if (used + block.length() > maxTotalChars) {
                break;
            }

            context.append(block);
            used += block.length();
            idx++;

            CitationVO c = new CitationVO();
            c.setDocId(d.getId());
            c.setTitle(title);
            ctx.getCitations().add(c);
        }

        String enhancedMessage = context
                + "【用户问题】\n" + userQuestion
                + "\n\n请基于上述团队知识库概览资料回答。\n"
                + "1) 如果用户在问整个团队知识库，请按主题/类别做结构化总结，并指出对应文档标题。\n"
                + "2) 如果用户的问题更具体，请优先关联到最相关的几篇文档并给出答案；资料不足时请说明需要补充哪些文档内容。";
        ctx.setEnhancedMessage(enhancedMessage);
        return ctx;
    }

    private String buildTeamEnhancedMessage(String userQuestion, List<TeamVectorService.TeamDocChunkMatch> matches) {
        if (matches == null || matches.isEmpty()) {
            return "【用户问题】\n" + userQuestion
                    + "\n\n【检索结果】\n未检索到与问题相关的团队文档片段。请先给出一个简要的通用回答，并提示用户补充更具体的关键词或完善团队文档，以便后续能给出基于团队文档的回答。不要声称无法访问或无法读取团队知识库。";
        }

        String context = matches.stream()
                .map(TeamVectorService.TeamDocChunkMatch::getText)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .limit(5)
                .collect(Collectors.joining("\n\n---\n\n"));

        return "【团队文档参考资料】\n以下内容来自团队文档检索结果，请优先基于这些内容回答：\n\n"
                + context
                + "\n\n【用户问题】\n"
                + userQuestion
                + "\n\n请基于上述参考资料回答。如果参考资料不包含答案，请明确说明并给出合理建议。";
    }

    private List<CitationVO> buildCitations(List<TeamVectorService.TeamDocChunkMatch> matches) {
        if (matches == null || matches.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, CitationVO> dedup = new LinkedHashMap<>();
        for (TeamVectorService.TeamDocChunkMatch m : matches) {
            if (m == null || m.getDocId() == null) {
                continue;
            }
            if (dedup.containsKey(m.getDocId())) {
                continue;
            }
            CitationVO c = new CitationVO();
            c.setDocId(m.getDocId());
            c.setTitle(m.getTitle());
            dedup.put(m.getDocId(), c);
        }
        return new ArrayList<>(dedup.values());
    }

    private void maybeUpdateSessionTitleFromFirstUserMessage(TeamAISession session, String userContent) {
        if (session == null || session.getId() == null) {
            return;
        }
        if (StrUtil.isBlank(userContent)) {
            return;
        }

        String currentTitle = session.getTitle();
        if (StrUtil.isNotBlank(currentTitle) && !"新会话".equals(currentTitle)) {
            return;
        }

        Long count = messageMapper.countMessagesBySessionId(session.getId());
        if (count != null && count > 0) {
            return;
        }

        String title = userContent.trim();
        if (title.length() > 30) {
            title = title.substring(0, 30);
        }

        TeamAISession update = new TeamAISession();
        update.setId(session.getId());
        update.setTitle(title);
        sessionMapper.updateById(update);
        session.setTitle(title);
    }

    @lombok.Data
    private static class TeamKnowledgeBaseContext {
        private String enhancedMessage;
        private List<CitationVO> citations;
    }
}
