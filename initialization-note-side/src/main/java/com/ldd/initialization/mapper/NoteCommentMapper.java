package com.ldd.initialization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ldd.initialization.domain.NoteComment;
import com.ldd.initialization.vo.NoteCommentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * Note comment mapper.
 */
@Mapper
public interface NoteCommentMapper extends BaseMapper<NoteComment> {

    @Select("SELECT c.id, c.note_id AS note_id, c.user_id AS user_id, c.parent_id AS parent_id, " +
            "c.reply_to_user_id AS reply_to_user_id, c.content, c.likes, " +
            "c.create_time AS create_time, c.update_time AS update_time, " +
            "u.username AS author_name, u.avatar_url AS author_avatar, " +
            "ru.username AS reply_to_user_name, ru.avatar_url AS reply_to_user_avatar " +
            "FROM t_note_comment c " +
            "LEFT JOIN t_user u ON c.user_id = u.id " +
            "LEFT JOIN t_user ru ON c.reply_to_user_id = ru.id " +
            "WHERE c.note_id = #{noteId} " +
            "ORDER BY c.create_time DESC")
    List<NoteCommentVO> selectByNoteId(@Param("noteId") Long noteId);

    @Select("<script>" +
            "SELECT c.id AS id, c.note_id AS targetId, n.title AS targetTitle, " +
            "c.content AS content, c.likes AS likes, " +
            "c.create_time AS createdAt, u.username AS author, u.avatar_url AS avatar " +
            "FROM t_note_comment c " +
            "LEFT JOIN t_note n ON c.note_id = n.id " +
            "LEFT JOIN t_user u ON c.user_id = u.id " +
            "WHERE 1=1 " +
            "<if test='targetId != null'> AND c.note_id = #{targetId} </if> " +
            "<if test='keyword != null and keyword != \"\"'> AND c.content LIKE CONCAT('%', #{keyword}, '%') </if> " +
            "ORDER BY c.create_time DESC" +
            "</script>")
    List<com.ldd.initialization.vo.TemplateCommentVO> selectAdminComments(@Param("targetId") Long targetId,
                                                                         @Param("keyword") String keyword);

    @Select("<script>" +
            "SELECT COUNT(*) FROM t_note_comment c WHERE 1=1 " +
            "<if test='targetId != null'> AND c.note_id = #{targetId} </if> " +
            "<if test='keyword != null and keyword != \"\"'> AND c.content LIKE CONCAT('%', #{keyword}, '%') </if> " +
            "</script>")
    Long countAdminComments(@Param("targetId") Long targetId,
                            @Param("keyword") String keyword);

    @Select("""
        WITH days AS (
          SELECT date_trunc('day', now()) - (interval '1 day' * gs) AS day_start
          FROM generate_series(0, #{days} - 1) AS gs
        )
        SELECT to_char(day_start, 'YYYY-MM-DD') AS period,
               COALESCE(COUNT(c.id), 0) AS count
        FROM days d
        LEFT JOIN t_note_comment c
          ON c.create_time >= d.day_start
         AND c.create_time < d.day_start + interval '1 day'
        GROUP BY day_start
        ORDER BY day_start
        """)
    List<Map<String, Object>> countDailyComments(@Param("days") Integer days);

    @Select("""
        SELECT u.username AS username,
               n.title AS target,
               c.create_time AS time
        FROM t_note_comment c
        JOIN t_note n ON c.note_id = n.id
        LEFT JOIN t_user u ON c.user_id = u.id
        ORDER BY c.create_time DESC
        LIMIT #{limit}
        """)
    List<Map<String, Object>> selectRecentComments(@Param("limit") Integer limit);
}
