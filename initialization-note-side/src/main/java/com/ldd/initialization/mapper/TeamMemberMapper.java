package com.ldd.initialization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ldd.initialization.domain.TeamMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 团队成员Mapper接口
 */
@Mapper
public interface TeamMemberMapper extends BaseMapper<TeamMember> {

    /**
     * 查询团队成员列表（带用户信息）
     */
    @Select("SELECT m.*, u.username, u.avatar_url, u.phone as email " +
            "FROM t_team_member m " +
            "LEFT JOIN \"t_user\" u ON m.user_id = u.id " +
            "WHERE m.team_id = #{teamId} AND m.status = 1 " +
            "ORDER BY m.role ASC, m.join_time ASC")
    List<TeamMember> selectMembersWithUser(@Param("teamId") Long teamId);

    /**
     * 查询用户在团队中的角色
     */
    @Select("SELECT role FROM t_team_member " +
            "WHERE team_id = #{teamId} AND user_id = #{userId} AND status = 1")
    Integer selectUserRole(@Param("teamId") Long teamId, @Param("userId") Long userId);

    /**
     * 检查用户是否是团队成员
     */
    @Select("SELECT COUNT(1) > 0 FROM t_team_member " +
            "WHERE team_id = #{teamId} AND user_id = #{userId} AND status = 1")
    boolean existsByTeamIdAndUserId(@Param("teamId") Long teamId, @Param("userId") Long userId);

    /**
     * 统计团队有效成员数
     */
    @Select("SELECT COUNT(1) FROM t_team_member WHERE team_id = #{teamId} AND status = 1")
    int countActiveMembers(@Param("teamId") Long teamId);
}
