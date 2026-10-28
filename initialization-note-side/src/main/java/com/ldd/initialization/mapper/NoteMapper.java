package com.ldd.initialization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ldd.initialization.domain.Note;
import com.ldd.initialization.vo.NoteDailyCountVO;
import com.ldd.initialization.vo.NoteBriefVO;
import com.ldd.initialization.vo.NoteDetailVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 笔记Mapper接口
 */
@Mapper
public interface NoteMapper extends BaseMapper<Note> {

    /**
     * 分页查询笔记列表（简要信息）
     * @param page 分页对象
     * @param notebookId 笔记本ID
     * @param userId 用户ID
     * @param keyword 搜索关键词
     * @return 笔记列表
     */
    @Select("<script>" +
            "SELECT n.*, nb.name as notebook_name, u.username as creator_name " +
            "FROM t_note n " +
            "LEFT JOIN t_notebook nb ON n.notebook_id = nb.id " +
            "LEFT JOIN t_user u ON n.user_id = u.id " +
            "WHERE n.user_id = #{userId} " +
            "<if test='notebookId != null'> AND n.notebook_id = #{notebookId} </if> " +
            "<if test='keyword != null and keyword != \"\"'> AND (n.title LIKE CONCAT('%', #{keyword}, '%') OR n.content_md LIKE CONCAT('%', #{keyword}, '%')) </if> " +
            "ORDER BY n.is_pinned DESC, n.update_time DESC" +
            "</script>")
    IPage<NoteBriefVO> selectNotesBriefByPage(Page<NoteBriefVO> page, @Param("notebookId") Long notebookId, @Param("userId") Long userId, @Param("keyword") String keyword);

    /**
     * 管理员分页查询笔记列表（不按 user_id 过滤）
     */
    @Select("<script>" +
            "SELECT n.*, nb.name as notebook_name, u.username as creator_name " +
            "FROM t_note n " +
            "LEFT JOIN t_notebook nb ON n.notebook_id = nb.id " +
            "LEFT JOIN t_user u ON n.user_id = u.id " +
            "WHERE 1=1 " +
            "<if test='notebookId != null'> AND n.notebook_id = #{notebookId} </if> " +
            "<if test='keyword != null and keyword != \"\"'> AND (n.title LIKE CONCAT('%', #{keyword}, '%') OR n.content_md LIKE CONCAT('%', #{keyword}, '%')) </if> " +
            "ORDER BY n.is_pinned DESC, n.update_time DESC" +
            "</script>")
    IPage<NoteBriefVO> selectNotesBriefByPageForAdmin(Page<NoteBriefVO> page, @Param("notebookId") Long notebookId, @Param("keyword") String keyword);

    /**
     * 查询笔记详情
     * @param noteId 笔记ID
     * @param userId 用户ID
     * @return 笔记详情
     */
    @Select("SELECT n.*, nb.name as notebook_name " +
            "FROM t_note n " +
            "LEFT JOIN t_notebook nb ON n.notebook_id = nb.id " +
            "WHERE n.id = #{noteId} AND n.user_id = #{userId}")
    NoteDetailVO selectNoteDetailById(@Param("noteId") Long noteId, @Param("userId") Long userId);

    /**
     * 管理员查询笔记详情（不按 user_id 过滤）
     */
    @Select("SELECT n.*, nb.name as notebook_name " +
            "FROM t_note n " +
            "LEFT JOIN t_notebook nb ON n.notebook_id = nb.id " +
            "WHERE n.id = #{noteId}")
    NoteDetailVO selectNoteDetailByIdForAdmin(@Param("noteId") Long noteId);

    /**
     * 增加笔记查看次数
     * @param noteId 笔记ID
     */
    @Update("UPDATE t_note SET view_count = view_count + 1 WHERE id = #{noteId}")
    void incrementViewCount(@Param("noteId") Long noteId);

    /**
     * 统计用户笔记总数
     * @param userId 用户ID
     * @return 笔记总数
     */
    @Select("SELECT COUNT(*) FROM t_note WHERE user_id = #{userId}")
    Long countNotesByUserId(@Param("userId") Long userId);

    /**
     * 查询用户最高产的一天（按创建日期统计）
     * @param userId 用户ID
     * @return 日期与数量
     */
    @Select("SELECT TO_CHAR(create_time, 'YYYY-MM-DD') AS day, COUNT(*) AS count " +
            "FROM t_note WHERE user_id = #{userId} " +
            "GROUP BY TO_CHAR(create_time, 'YYYY-MM-DD') " +
            "ORDER BY count DESC, day DESC LIMIT 1")
    NoteDailyCountVO selectMostProductiveDay(@Param("userId") Long userId);

    /**
     * 统计今日新增笔记数量
     */
    @Select("""
        SELECT COUNT(*)
        FROM t_note
        WHERE create_time >= date_trunc('day', now())
          AND create_time < date_trunc('day', now()) + interval '1 day'
        """)
    Long countTodayNotes();

    @Select("""
        SELECT u.username AS username,
               n.title AS target,
               n.create_time AS time
        FROM t_note n
        LEFT JOIN t_user u ON n.user_id = u.id
        ORDER BY n.create_time DESC
        LIMIT #{limit}
        """)
    List<Map<String, Object>> selectRecentNotes(@Param("limit") Integer limit);

    @Select("""
        SELECT COUNT(*)
        FROM t_note
        WHERE create_time >= date_trunc('week', now()) - interval '1 week'
          AND create_time < date_trunc('week', now())
        """)
    Long countLastWeekNotes();
} 
