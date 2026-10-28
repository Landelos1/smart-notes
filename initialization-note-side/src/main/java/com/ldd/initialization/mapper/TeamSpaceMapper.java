package com.ldd.initialization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ldd.initialization.domain.TeamSpace;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 团队空间Mapper接口
 */
@Mapper
public interface TeamSpaceMapper extends BaseMapper<TeamSpace> {

    /**
     * 查询用户所属团队列表（带用户角色）
     */
    @Select("SELECT t.*, m.role as my_role " +
            "FROM t_team_space t " +
            "INNER JOIN t_team_member m ON t.id = m.team_id " +
            "WHERE m.user_id = #{userId} AND m.status = 1 AND t.status = 1 " +
            "ORDER BY t.last_active_time DESC NULLS LAST, t.create_time DESC")
    List<TeamSpace> selectTeamsByUserId(@Param("userId") Long userId);

    @Select("SELECT t.* " +
            "FROM t_team_space t " +
            "WHERE t.status = 1 AND t.is_public = TRUE " +
            "AND NOT EXISTS (" +
            "  SELECT 1 FROM t_team_member m " +
            "  WHERE m.team_id = t.id AND m.user_id = #{userId} AND m.status = 1" +
            ") " +
            "ORDER BY t.last_active_time DESC NULLS LAST, t.create_time DESC")
    List<TeamSpace> selectDiscoverableTeams(@Param("userId") Long userId);

    /**
     * 根据邀请码查询团队
     */
    @Select("SELECT * FROM t_team_space WHERE invite_code = #{inviteCode} AND status = 1")
    TeamSpace selectByInviteCode(@Param("inviteCode") String inviteCode);

    /**
     * 更新成员数量
     */
    @Update("UPDATE t_team_space SET member_count = member_count + #{delta} WHERE id = #{teamId}")
    int updateMemberCount(@Param("teamId") Long teamId, @Param("delta") int delta);

    /**
     * 更新文档数量
     */
    @Update("UPDATE t_team_space SET doc_count = doc_count + #{delta} WHERE id = #{teamId}")
    int updateDocCount(@Param("teamId") Long teamId, @Param("delta") int delta);

    /**
     * 更新邀请码使用次数
     */
    @Update("UPDATE t_team_space SET invite_used_count = invite_used_count + 1 WHERE id = #{teamId}")
    int incrementInviteUsedCount(@Param("teamId") Long teamId);

    /**
     * 更新最后活跃时间
     */
    @Update("UPDATE t_team_space SET last_active_time = NOW() WHERE id = #{teamId}")
    int updateLastActiveTime(@Param("teamId") Long teamId);
}
