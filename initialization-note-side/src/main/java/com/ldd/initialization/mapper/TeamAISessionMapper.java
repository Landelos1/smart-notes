package com.ldd.initialization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ldd.initialization.domain.TeamAISession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 团队AI会话Mapper接口
 */
@Mapper
public interface TeamAISessionMapper extends BaseMapper<TeamAISession> {

    /**
     * 查询团队AI会话列表
     */
    @Select("SELECT * FROM t_team_ai_session " +
            "WHERE team_id = #{teamId} AND status = 1 " +
            "ORDER BY is_pinned DESC, update_time DESC")
    List<TeamAISession> selectSessionsByTeamId(@Param("teamId") Long teamId);
}
