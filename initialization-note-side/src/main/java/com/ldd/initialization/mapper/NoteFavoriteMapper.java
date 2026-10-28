package com.ldd.initialization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ldd.initialization.domain.NoteFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * Note favorite mapper.
 */
@Mapper
public interface NoteFavoriteMapper extends BaseMapper<NoteFavorite> {

    @Select("SELECT * FROM t_note_favorite WHERE user_id = #{userId} AND note_id = #{noteId} LIMIT 1")
    NoteFavorite selectByUserIdAndNoteId(@Param("userId") Long userId,
                                         @Param("noteId") Long noteId);

    @Select("""
        WITH days AS (
          SELECT date_trunc('day', now()) - (interval '1 day' * gs) AS day_start
          FROM generate_series(0, #{days} - 1) AS gs
        )
        SELECT to_char(day_start, 'YYYY-MM-DD') AS period,
               COALESCE(COUNT(nf.id), 0) AS count
        FROM days d
        LEFT JOIN t_note_favorite nf
          ON nf.create_time >= d.day_start
         AND nf.create_time < d.day_start + interval '1 day'
        GROUP BY day_start
        ORDER BY day_start
        """)
    List<Map<String, Object>> countDailyFavorites(@Param("days") Integer days);
}
