package com.ldd.initialization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ldd.initialization.domain.TeamAIMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 团队AI消息Mapper接口
 */
@Mapper
public interface TeamAIMessageMapper extends BaseMapper<TeamAIMessage> {

    /**
     * 查询会话消息列表
     */
    @Select("SELECT * FROM t_team_ai_message " +
            "WHERE session_id = #{sessionId} " +
            "ORDER BY create_time ASC")
    List<TeamAIMessage> selectMessagesBySessionId(@Param("sessionId") Long sessionId);

    @Select("SELECT COUNT(1) FROM t_team_ai_message WHERE session_id = #{sessionId}")
    Long countMessagesBySessionId(@Param("sessionId") Long sessionId);
}
