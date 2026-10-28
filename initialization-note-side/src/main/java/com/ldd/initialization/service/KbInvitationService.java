package com.ldd.initialization.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ldd.initialization.domain.KbInvitation;
import com.ldd.initialization.dto.InvitationRespondDTO;

import java.util.Map;

/**
 * 知识库邀请Service接口
 */
public interface KbInvitationService extends IService<KbInvitation> {

    /**
     * 获取我的邀请列表
     * @param userId 用户ID
     * @param status 邀请状态
     * @param page 页码
     * @param size 每页大小
     * @return 邀请列表和总数
     */
    Map<String, Object> getMyInvitations(Long userId, Integer status, Integer page, Integer size);

    /**
     * 获取待处理邀请数量
     * @param userId 用户ID
     * @return 待处理邀请数量
     */
    Integer getPendingInvitationCount(Long userId);

    /**
     * 响应邀请
     * @param invitationId 邀请ID
     * @param respondDTO 响应DTO
     * @param userId 当前用户ID
     */
    void respondInvitation(Long invitationId, InvitationRespondDTO respondDTO, Long userId);

    /**
     * 检查是否存在待处理的邀请
     * @param knowledgeBaseId 知识库ID
     * @param inviteeId 被邀请人ID
     * @return 是否存在待处理邀请
     */
    boolean hasPendingInvitation(Long knowledgeBaseId, Long inviteeId);
}
