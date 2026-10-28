package com.ldd.initialization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ldd.initialization.domain.TeamDocEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 团队文档事件Mapper接口
 */
@Mapper
public interface TeamDocEventMapper extends BaseMapper<TeamDocEvent> {

    /**
     * 查询文档版本历史（event_type=1）
     */
    @Select("SELECT e.*, u.username, u.avatar_url " +
            "FROM t_team_doc_event e " +
            "LEFT JOIN \"t_user\" u ON e.user_id = u.id " +
            "WHERE e.doc_id = #{docId} AND e.event_type = 1 " +
            "ORDER BY e.create_time DESC")
    List<TeamDocEvent> selectVersionHistory(@Param("docId") Long docId);

    /**
     * 查询文档评论列表（event_type=2）
     */
    @Select("SELECT e.*, u.username, u.avatar_url " +
            "FROM t_team_doc_event e " +
            "LEFT JOIN \"t_user\" u ON e.user_id = u.id " +
            "WHERE e.doc_id = #{docId} AND e.event_type = 2 " +
            "ORDER BY e.create_time ASC")
    List<TeamDocEvent> selectComments(@Param("docId") Long docId);

    /**
     * 查询文档动态（event_type=3）
     */
    @Select("SELECT e.*, u.username, u.avatar_url " +
            "FROM t_team_doc_event e " +
            "LEFT JOIN \"t_user\" u ON e.user_id = u.id " +
            "WHERE e.doc_id = #{docId} AND e.event_type = 3 " +
            "ORDER BY e.create_time DESC " +
            "LIMIT 50")
    List<TeamDocEvent> selectActivities(@Param("docId") Long docId);

    /**
     * 获取最新版本号
     */
    @Select("SELECT COALESCE(MAX((metadata->>'version')::int), 0) FROM t_team_doc_event " +
            "WHERE doc_id = #{docId} AND event_type = 1")
    Integer getLatestVersionNumber(@Param("docId") Long docId);
}
