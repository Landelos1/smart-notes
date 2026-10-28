package com.ldd.initialization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ldd.initialization.domain.ChatMessage;
import com.ldd.initialization.vo.TokenTrendItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Select("""
        SELECT COALESCE(SUM(m.total_tokens), 0)
        FROM t_chat_message m
        JOIN t_chat_session s ON m.session_id = s.id
        WHERE s.user_id = #{userId}
          AND m.timestamp >= date_trunc('day', now())
          AND m.timestamp < date_trunc('day', now()) + interval '1 day'
        """)
    Long sumTodayTokensByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT COALESCE(SUM(m.total_tokens), 0)
        FROM t_chat_message m
        JOIN t_chat_session s ON m.session_id = s.id
        WHERE s.user_id = #{userId}
          AND m.timestamp >= date_trunc('month', now())
          AND m.timestamp < date_trunc('month', now()) + interval '1 month'
        """)
    Long sumMonthTokensByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT COALESCE(SUM(m.total_tokens), 0)
        FROM t_chat_message m
        JOIN t_chat_session s ON m.session_id = s.id
        WHERE s.user_id = #{userId}
        """)
    Long sumTotalTokensByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT TO_CHAR(date_trunc('month', m.timestamp), 'YYYY-MM') AS period,
               COALESCE(SUM(m.total_tokens), 0) AS totalTokens
        FROM t_chat_message m
        JOIN t_chat_session s ON m.session_id = s.id
        WHERE s.user_id = #{userId}
          AND m.timestamp >= date_trunc('month', now()) - interval '11 months'
        GROUP BY period
        ORDER BY period
        """)
    List<TokenTrendItem> listMonthlyTokensByUserId(@Param("userId") Long userId);

    @Select("""
        SELECT COUNT(*)
        FROM t_chat_message m
        WHERE m.role = #{role}
          AND m.timestamp >= date_trunc('day', now())
          AND m.timestamp < date_trunc('day', now()) + interval '1 day'
        """)
    Long countTodayRequests(@Param("role") Integer role);

    @Select("""
        SELECT COUNT(*)
        FROM t_chat_message m
        WHERE m.role = #{role}
          AND m.timestamp >= date_trunc('week', now()) - interval '1 week'
          AND m.timestamp < date_trunc('week', now())
        """)
    Long countLastWeekRequests(@Param("role") Integer role);

    @Select("""
        SELECT COALESCE(SUM(total_tokens), 0)
        FROM t_chat_message
        WHERE timestamp >= date_trunc('day', now())
          AND timestamp < date_trunc('day', now()) + interval '1 day'
        """)
    Long sumTodayTokensGlobal();

    @Select("""
        SELECT COALESCE(SUM(total_tokens), 0)
        FROM t_chat_message
        WHERE timestamp >= date_trunc('month', now())
          AND timestamp < date_trunc('month', now()) + interval '1 month'
        """)
    Long sumMonthTokensGlobal();

    @Select("""
        SELECT COALESCE(SUM(total_tokens), 0)
        FROM t_chat_message
        """)
    Long sumTotalTokensGlobal();

    @Select("""
        WITH months AS (
          SELECT date_trunc('month', now()) - (interval '1 month' * gs) AS month_start
          FROM generate_series(0, 11) AS gs
        )
        SELECT to_char(month_start, 'YYYY-MM') AS period,
               COALESCE(SUM(m.total_tokens), 0) AS totalTokens
        FROM months mo
        LEFT JOIN t_chat_message m
          ON m.timestamp >= mo.month_start
         AND m.timestamp < mo.month_start + interval '1 month'
        GROUP BY month_start
        ORDER BY month_start
        """)
    List<TokenTrendItem> listMonthlyTokensGlobal();
}
