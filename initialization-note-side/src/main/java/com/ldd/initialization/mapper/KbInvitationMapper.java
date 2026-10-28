package com.ldd.initialization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ldd.initialization.domain.KbInvitation;
import com.ldd.initialization.vo.KbInvitationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识库邀请Mapper接口
 */
@Mapper
public interface KbInvitationMapper extends BaseMapper<KbInvitation> {

    /**
     * 查询用户的邀请列表（带分页）
     */
    @Select("SELECT inv.id, inv.knowledge_base_id as knowledgeBaseId, " +
            "       inv.inviter_id as inviterId, inv.role, inv.status, " +
            "       inv.invite_time as inviteTime, inv.expire_time as expireTime, " +
            "       inv.inviter_message as inviterMessage, " +
            "       kb.name as knowledgeBaseName, kb.cover_url as knowledgeBaseCover, " +
            "       kb.description as knowledgeBaseDescription, " +
            "       u.username as inviterName, u.avatar_url as inviterAvatar, " +
            "       CASE " +
            "           WHEN inv.role = 1 THEN '创建者' " +
            "           WHEN inv.role = 2 THEN '管理员' " +
            "           WHEN inv.role = 3 THEN '成员' " +
            "           ELSE '未知' " +
            "       END as roleLabel " +
            "FROM t_kb_invitation inv " +
            "LEFT JOIN t_shared_knowledge_base kb ON inv.knowledge_base_id = kb.id " +
            "LEFT JOIN \"t_user\" u ON inv.inviter_id = u.id " +
            "WHERE inv.invitee_id = #{userId} " +
            "  AND inv.status = #{status} " +
            "  AND inv.expire_time > CURRENT_TIMESTAMP " +
            "ORDER BY inv.invite_time DESC " +
            "LIMIT #{size} OFFSET #{offset}")
    List<KbInvitationVO> selectMyInvitations(@Param("userId") Long userId,
                                             @Param("status") Integer status,
                                             @Param("offset") Integer offset,
                                             @Param("size") Integer size);

    /**
     * 统计用户的待处理邀请数量
     */
    @Select("SELECT COUNT(1) FROM t_kb_invitation " +
            "WHERE invitee_id = #{userId} " +
            "  AND status = 0 " +
            "  AND expire_time > CURRENT_TIMESTAMP")
    Integer countPendingInvitations(@Param("userId") Long userId);

    /**
     * 检查是否存在待处理的邀请
     */
    @Select("SELECT COUNT(1) > 0 FROM t_kb_invitation " +
            "WHERE knowledge_base_id = #{knowledgeBaseId} " +
            "  AND invitee_id = #{inviteeId} " +
            "  AND status = 0 " +
            "  AND expire_time > CURRENT_TIMESTAMP")
    boolean existsPendingInvitation(@Param("knowledgeBaseId") Long knowledgeBaseId,
                                    @Param("inviteeId") Long inviteeId);

    /**
     * 统计指定状态的邀请总数
     */
    @Select("SELECT COUNT(1) FROM t_kb_invitation " +
            "WHERE invitee_id = #{userId} " +
            "  AND status = #{status} " +
            "  AND expire_time > CURRENT_TIMESTAMP")
    Integer countInvitationsByStatus(@Param("userId") Long userId, 
                                      @Param("status") Integer status);
}
