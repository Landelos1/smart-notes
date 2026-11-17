package com.ldd.initialization.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ldd.initialization.config.exception.BizException;
import com.ldd.initialization.domain.KbInvitation;
import com.ldd.initialization.domain.KnowledgeBaseMember;
import com.ldd.initialization.domain.SharedKnowledgeBase;
import com.ldd.initialization.dto.InvitationRespondDTO;
import com.ldd.initialization.enums.InvitationStatusEnum;
import com.ldd.initialization.mapper.KbInvitationMapper;
import com.ldd.initialization.mapper.KnowledgeBaseMemberMapper;
import com.ldd.initialization.mapper.SharedKnowledgeBaseMapper;
import com.ldd.initialization.result.BizResponseCode;
import com.ldd.initialization.service.KbInvitationService;
import com.ldd.initialization.vo.KbInvitationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库邀请Service实现类
 */
@Service
@Slf4j
public class KbInvitationServiceImpl extends ServiceImpl<KbInvitationMapper, KbInvitation>
        implements KbInvitationService {

    @Autowired
    private KbInvitationMapper invitationMapper;

    @Autowired
    private KnowledgeBaseMemberMapper memberMapper;

    @Autowired
    private SharedKnowledgeBaseMapper knowledgeBaseMapper;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Map<String, Object> getMyInvitations(Long userId, Integer status, Integer page, Integer size) {
        invitationMapper.expireInvitations(userId);
        // 参数校验
        if (status == null) {
            status = InvitationStatusEnum.PENDING.getValue();
        }
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 10;
        }

        int offset = (page - 1) * size;

        // 查询邀请列表
        List<KbInvitationVO> invitations = invitationMapper.selectMyInvitations(userId, status, offset, size);

        // 计算剩余天数
        LocalDateTime now = LocalDateTime.now();
        for (KbInvitationVO invitation : invitations) {
            if (invitation.getExpireTime() != null) {
                try {
                    LocalDateTime expireTime = LocalDateTime.parse(invitation.getExpireTime(), DATE_TIME_FORMATTER);
                    long daysLeft = Duration.between(now, expireTime).toDays();
                    invitation.setDaysLeft((int) Math.max(0, daysLeft));
                } catch (Exception e) {
                    log.warn("解析过期时间失败: {}", invitation.getExpireTime(), e);
                    invitation.setDaysLeft(0);
                }
            }
        }

        // 统计总数
        Integer total = invitationMapper.countInvitationsByStatus(userId, status);

        Map<String, Object> result = new HashMap<>();
        result.put("data", invitations);
        result.put("total", total != null ? total : 0);
        return result;
    }

    @Override
    public Integer getPendingInvitationCount(Long userId) {
        invitationMapper.expireInvitations(userId);
        Integer count = invitationMapper.countPendingInvitations(userId);
        return count != null ? count : 0;
    }

    @Override
    @Transactional
    public void respondInvitation(Long invitationId, InvitationRespondDTO respondDTO, Long userId) {
        // 查询邀请记录
        KbInvitation invitation = this.getById(invitationId);
        if (invitation == null) {
            throw new BizException(BizResponseCode.ERROR_1, "邀请不存在");
        }

        // 验证是否为被邀请人
        if (!invitation.getInviteeId().equals(userId)) {
            throw new BizException(BizResponseCode.ERR_11004, "无权限操作此邀请");
        }

        // 检查邀请状态
        if (!invitation.getStatus().equals(InvitationStatusEnum.PENDING.getValue())) {
            throw new BizException(BizResponseCode.ERROR_1, "邀请已处理");
        }

        // 检查是否过期
        if (invitation.getExpireTime() != null && invitation.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException(BizResponseCode.ERROR_1, "邀请已过期");
        }

        if (respondDTO.getAccept()) {
            // 接受邀请
            acceptInvitation(invitation, userId);
        } else {
            // 拒绝邀请
            rejectInvitation(invitation);
        }

        log.info("用户 {} {} 邀请 ID: {}", userId, respondDTO.getAccept() ? "接受" : "拒绝", invitationId);
    }

    @Override
    public boolean hasPendingInvitation(Long knowledgeBaseId, Long inviteeId) {
        return invitationMapper.existsPendingInvitation(knowledgeBaseId, inviteeId);
    }

    /**
     * 接受邀请
     */
    private void acceptInvitation(KbInvitation invitation, Long userId) {
        // 检查用户是否已是成员
        boolean isMember = memberMapper.existsByKnowledgeBaseIdAndUserId(
                invitation.getKnowledgeBaseId(), userId);
        
        if (isMember) {
            throw new BizException(BizResponseCode.ERROR_1, "您已是该知识库成员");
        }

        // 检查知识库是否存在
        SharedKnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(invitation.getKnowledgeBaseId());
        if (knowledgeBase == null || knowledgeBase.getStatus() != 1) {
            throw new BizException(BizResponseCode.ERROR_1, "知识库不存在或已被删除");
        }

        // 更新邀请状态
        invitation.setStatus(InvitationStatusEnum.ACCEPTED.getValue());
        invitation.setResponseTime(LocalDateTime.now());
        this.updateById(invitation);

        // 添加成员记录
        KnowledgeBaseMember member = new KnowledgeBaseMember();
        member.setKnowledgeBaseId(invitation.getKnowledgeBaseId());
        member.setUserId(userId);
        member.setRole(invitation.getRole());
        member.setJoinTime(LocalDateTime.now());
        memberMapper.insert(member);

        // 更新知识库成员数量
        knowledgeBase.setMemberCount(knowledgeBase.getMemberCount() + 1);
        knowledgeBaseMapper.updateById(knowledgeBase);
    }

    /**
     * 拒绝邀请
     */
    private void rejectInvitation(KbInvitation invitation) {
        invitation.setStatus(InvitationStatusEnum.REJECTED.getValue());
        invitation.setResponseTime(LocalDateTime.now());
        this.updateById(invitation);
    }
}
