package com.ldd.initialization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ldd.initialization.domain.NoteSquare;
import com.ldd.initialization.vo.NoteSquareDetailVO;
import com.ldd.initialization.vo.NoteSquareVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * Note square mapper.
 */
@Mapper
public interface NoteSquareMapper extends BaseMapper<NoteSquare> {

    @Select("SELECT * FROM t_note_square WHERE note_id = #{noteId} LIMIT 1")
    NoteSquare selectByNoteId(@Param("noteId") Long noteId);

    @Select("<script>" +
            "SELECT ns.id AS square_id, n.id AS id, n.title, n.content_md AS content, " +
            "ns.summary, ns.cover_url AS coverUrl, ns.status AS square_status, " +
            "ns.publish_time AS publish_time, ns.update_time AS update_time, " +
            "ns.view_count AS view_count, ns.like_count AS like_count, ns.favorite_count AS favorite_count, ns.comment_count AS comment_count, " +
            "n.create_time AS create_time, " +
            "u.id AS author_id, u.username AS author_name, u.avatar_url AS author_avatar, " +
            "CASE WHEN nl.user_id IS NULL THEN false ELSE true END AS is_liked, " +
            "CASE WHEN nf.user_id IS NULL THEN false ELSE true END AS is_favorited " +
            "FROM t_note_square ns " +
            "JOIN t_note n ON ns.note_id = n.id " +
            "LEFT JOIN t_user u ON n.user_id = u.id " +
            "LEFT JOIN t_note_like nl ON nl.note_id = n.id AND nl.user_id = #{userId} " +
            "LEFT JOIN t_note_favorite nf ON nf.note_id = n.id AND nf.user_id = #{userId} " +
            "WHERE ns.status = 1 " +
            "<if test='keywords != null and keywords.size() > 0'> " +
            "  AND ( " +
            "    <foreach collection='keywords' item='kw' separator=' OR '> " +
            "      (n.title LIKE CONCAT('%', #{kw}, '%') " +
            "      OR n.content_md LIKE CONCAT('%', #{kw}, '%') " +
            "      OR ns.summary LIKE CONCAT('%', #{kw}, '%')) " +
            "    </foreach> " +
            "  ) " +
            "</if> " +
            "<if test='(keywords == null or keywords.size() == 0) and keyword != null and keyword != \"\"'> " +
            "  AND (n.title LIKE CONCAT('%', #{keyword}, '%') " +
            "  OR n.content_md LIKE CONCAT('%', #{keyword}, '%') " +
            "  OR ns.summary LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if> " +
            "ORDER BY ns.is_pinned DESC, " +
            "<choose> " +
            "  <when test='sortBy == \"recommend\"'>ns.recommend_score</when> " +
            "  <when test='sortBy == \"latest\"'>ns.publish_time</when> " +
            "  <when test='sortBy == \"update_time\"'>ns.update_time</when> " +
            "  <otherwise>ns.publish_time</otherwise> " +
            "</choose> " +
            "<choose> " +
            "  <when test='sortOrder == \"asc\"'>ASC</when> " +
            "  <otherwise>DESC</otherwise> " +
            "</choose> " +
            "</script>")
    IPage<NoteSquareVO> selectSquareList(Page<NoteSquareVO> page,
                                         @Param("keyword") String keyword,
                                         @Param("keywords") List<String> keywords,
                                         @Param("sortBy") String sortBy,
                                         @Param("sortOrder") String sortOrder,
                                         @Param("userId") Long userId);

    @Select("<script>" +
            "SELECT ns.id AS square_id, n.id AS id, n.title, n.content_md AS content, " +
            "ns.summary, ns.cover_url AS coverUrl, ns.status AS square_status, " +
            "ns.publish_time AS publish_time, ns.update_time AS update_time, " +
            "ns.view_count AS view_count, ns.like_count AS like_count, ns.favorite_count AS favorite_count, ns.comment_count AS comment_count, " +
            "n.create_time AS create_time, " +
            "u.id AS author_id, u.username AS author_name, u.avatar_url AS author_avatar, " +
            "CASE WHEN nl.user_id IS NULL THEN false ELSE true END AS is_liked, " +
            "true AS is_favorited " +
            "FROM t_note_favorite nf " +
            "JOIN t_note_square ns ON nf.note_id = ns.note_id " +
            "JOIN t_note n ON n.id = ns.note_id " +
            "LEFT JOIN t_user u ON n.user_id = u.id " +
            "LEFT JOIN t_note_like nl ON nl.note_id = n.id AND nl.user_id = #{userId} " +
            "WHERE nf.user_id = #{userId} AND ns.status = 1 " +
            "<if test='keywords != null and keywords.size() > 0'> " +
            "  AND ( " +
            "    <foreach collection='keywords' item='kw' separator=' OR '> " +
            "      (n.title LIKE CONCAT('%', #{kw}, '%') " +
            "      OR n.content_md LIKE CONCAT('%', #{kw}, '%') " +
            "      OR ns.summary LIKE CONCAT('%', #{kw}, '%')) " +
            "    </foreach> " +
            "  ) " +
            "</if> " +
            "<if test='(keywords == null or keywords.size() == 0) and keyword != null and keyword != \"\"'> " +
            "  AND (n.title LIKE CONCAT('%', #{keyword}, '%') " +
            "  OR n.content_md LIKE CONCAT('%', #{keyword}, '%') " +
            "  OR ns.summary LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if> " +
            "ORDER BY nf.create_time DESC " +
            "</script>")
    IPage<NoteSquareVO> selectFavoriteList(Page<NoteSquareVO> page,
                                           @Param("keyword") String keyword,
                                           @Param("keywords") List<String> keywords,
                                           @Param("userId") Long userId);

    @Select("<script>" +
            "SELECT ns.id AS square_id, n.id AS id, n.title, n.content_md AS content, " +
            "ns.summary, ns.cover_url AS coverUrl, ns.status AS square_status, " +
            "ns.publish_time AS publish_time, ns.update_time AS update_time, " +
            "ns.view_count AS view_count, ns.like_count AS like_count, ns.favorite_count AS favorite_count, ns.comment_count AS comment_count, " +
            "n.create_time AS create_time, " +
            "u.id AS author_id, u.username AS author_name, u.avatar_url AS author_avatar, " +
            "true AS is_liked, " +
            "CASE WHEN nf.user_id IS NULL THEN false ELSE true END AS is_favorited " +
            "FROM t_note_like nl " +
            "JOIN t_note_square ns ON nl.note_id = ns.note_id " +
            "JOIN t_note n ON n.id = ns.note_id " +
            "LEFT JOIN t_user u ON n.user_id = u.id " +
            "LEFT JOIN t_note_favorite nf ON nf.note_id = n.id AND nf.user_id = #{userId} " +
            "WHERE nl.user_id = #{userId} AND ns.status = 1 " +
            "<if test='keywords != null and keywords.size() > 0'> " +
            "  AND ( " +
            "    <foreach collection='keywords' item='kw' separator=' OR '> " +
            "      (n.title LIKE CONCAT('%', #{kw}, '%') " +
            "      OR n.content_md LIKE CONCAT('%', #{kw}, '%') " +
            "      OR ns.summary LIKE CONCAT('%', #{kw}, '%')) " +
            "    </foreach> " +
            "  ) " +
            "</if> " +
            "<if test='(keywords == null or keywords.size() == 0) and keyword != null and keyword != \"\"'> " +
            "  AND (n.title LIKE CONCAT('%', #{keyword}, '%') " +
            "  OR n.content_md LIKE CONCAT('%', #{keyword}, '%') " +
            "  OR ns.summary LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if> " +
            "ORDER BY nl.create_time DESC " +
            "</script>")
    IPage<NoteSquareVO> selectLikeList(Page<NoteSquareVO> page,
                                       @Param("keyword") String keyword,
                                       @Param("keywords") List<String> keywords,
                                       @Param("userId") Long userId);

    @Select("<script>" +
            "SELECT n.id AS id, n.title, n.content_md AS content, " +
            "n.create_time AS create_time, n.update_time AS update_time, " +
            "ns.id AS square_id, ns.summary, ns.cover_url AS coverUrl, " +
            "COALESCE(ns.status, 0) AS square_status, ns.publish_time AS publish_time, " +
            "COALESCE(ns.view_count, 0) AS view_count, COALESCE(ns.like_count, 0) AS like_count, " +
            "COALESCE(ns.favorite_count, 0) AS favorite_count, COALESCE(ns.comment_count, 0) AS comment_count, " +
            "u.id AS author_id, u.username AS author_name, u.avatar_url AS author_avatar, " +
            "CASE WHEN nl.user_id IS NULL THEN false ELSE true END AS is_liked, " +
            "CASE WHEN nf.user_id IS NULL THEN false ELSE true END AS is_favorited " +
            "FROM t_note n " +
            "LEFT JOIN t_note_square ns ON n.id = ns.note_id " +
            "LEFT JOIN t_user u ON n.user_id = u.id " +
            "LEFT JOIN t_note_like nl ON nl.note_id = n.id AND nl.user_id = #{userId} " +
            "LEFT JOIN t_note_favorite nf ON nf.note_id = n.id AND nf.user_id = #{userId} " +
            "WHERE n.user_id = #{userId} " +
            "<if test='keywords != null and keywords.size() > 0'> " +
            "  AND ( " +
            "    <foreach collection='keywords' item='kw' separator=' OR '> " +
            "      (n.title LIKE CONCAT('%', #{kw}, '%') " +
            "      OR n.content_md LIKE CONCAT('%', #{kw}, '%') " +
            "      OR ns.summary LIKE CONCAT('%', #{kw}, '%')) " +
            "    </foreach> " +
            "  ) " +
            "</if> " +
            "<if test='(keywords == null or keywords.size() == 0) and keyword != null and keyword != \"\"'> " +
            "  AND (n.title LIKE CONCAT('%', #{keyword}, '%') " +
            "  OR n.content_md LIKE CONCAT('%', #{keyword}, '%') " +
            "  OR ns.summary LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if> " +
            "ORDER BY n.update_time DESC " +
            "</script>")
    IPage<NoteSquareVO> selectMyNotes(Page<NoteSquareVO> page,
                                      @Param("keyword") String keyword,
                                      @Param("keywords") List<String> keywords,
                                      @Param("userId") Long userId);

    @Select("SELECT n.id AS id, n.title, n.content_md AS content, " +
            "n.create_time AS create_time, n.update_time AS update_time, " +
            "ns.id AS square_id, ns.summary, ns.cover_url AS coverUrl, " +
            "COALESCE(ns.status, 0) AS square_status, ns.publish_time AS publish_time, " +
            "COALESCE(ns.view_count, 0) AS view_count, COALESCE(ns.like_count, 0) AS like_count, " +
            "COALESCE(ns.favorite_count, 0) AS favorite_count, COALESCE(ns.comment_count, 0) AS comment_count, " +
            "u.id AS author_id, u.username AS author_name, u.avatar_url AS author_avatar, " +
            "CASE WHEN nl.user_id IS NULL THEN false ELSE true END AS is_liked, " +
            "CASE WHEN nf.user_id IS NULL THEN false ELSE true END AS is_favorited " +
            "FROM t_note n " +
            "LEFT JOIN t_note_square ns ON n.id = ns.note_id " +
            "LEFT JOIN t_user u ON n.user_id = u.id " +
            "LEFT JOIN t_note_like nl ON nl.note_id = n.id AND nl.user_id = #{userId} " +
            "LEFT JOIN t_note_favorite nf ON nf.note_id = n.id AND nf.user_id = #{userId} " +
            "WHERE n.id = #{noteId}")
    NoteSquareDetailVO selectDetail(@Param("noteId") Long noteId,
                                    @Param("userId") Long userId);

    @Update("UPDATE t_note_square SET view_count = view_count + 1, update_time = CURRENT_TIMESTAMP WHERE note_id = #{noteId}")
    void incrementViewCount(@Param("noteId") Long noteId);

    @Update("UPDATE t_note_square SET like_count = like_count + 1, update_time = CURRENT_TIMESTAMP WHERE note_id = #{noteId}")
    void incrementLikeCount(@Param("noteId") Long noteId);

    @Update("UPDATE t_note_square SET like_count = CASE WHEN like_count > 0 THEN like_count - 1 ELSE 0 END, " +
            "update_time = CURRENT_TIMESTAMP WHERE note_id = #{noteId}")
    void decrementLikeCount(@Param("noteId") Long noteId);

    @Update("UPDATE t_note_square SET favorite_count = favorite_count + 1, update_time = CURRENT_TIMESTAMP WHERE note_id = #{noteId}")
    void incrementFavoriteCount(@Param("noteId") Long noteId);

    @Update("UPDATE t_note_square SET favorite_count = CASE WHEN favorite_count > 0 THEN favorite_count - 1 ELSE 0 END, " +
            "update_time = CURRENT_TIMESTAMP WHERE note_id = #{noteId}")
    void decrementFavoriteCount(@Param("noteId") Long noteId);

    @Update("UPDATE t_note_square SET comment_count = comment_count + 1, update_time = CURRENT_TIMESTAMP WHERE note_id = #{noteId}")
    void incrementCommentCount(@Param("noteId") Long noteId);

    @Update("UPDATE t_note_square SET comment_count = CASE WHEN comment_count > 0 THEN comment_count - 1 ELSE 0 END, " +
            "update_time = CURRENT_TIMESTAMP WHERE note_id = #{noteId}")
    void decrementCommentCount(@Param("noteId") Long noteId);

    @Select("""
        WITH days AS (
          SELECT date_trunc('day', now()) - (interval '1 day' * gs) AS day_start
          FROM generate_series(0, #{days} - 1) AS gs
        )
        SELECT to_char(day_start, 'YYYY-MM-DD') AS period,
               COALESCE(COUNT(ns.id), 0) AS count
        FROM days d
        LEFT JOIN t_note_square ns
          ON ns.publish_time >= d.day_start
         AND ns.publish_time < d.day_start + interval '1 day'
         AND ns.status = 1
        GROUP BY day_start
        ORDER BY day_start
        """)
    List<Map<String, Object>> countDailyPublished(@Param("days") Integer days);

    @Select("""
        SELECT u.username AS username,
               n.title AS target,
               ns.publish_time AS time
        FROM t_note_square ns
        JOIN t_note n ON ns.note_id = n.id
        LEFT JOIN t_user u ON n.user_id = u.id
        WHERE ns.status = 1
        ORDER BY ns.publish_time DESC
        LIMIT #{limit}
        """)
    List<Map<String, Object>> selectRecentPublished(@Param("limit") Integer limit);
}
