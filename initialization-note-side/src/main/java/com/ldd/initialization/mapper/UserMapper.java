package com.ldd.initialization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ldd.initialization.domain.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface UserMapper extends BaseMapper<User> {

    @Select("""
        SELECT COUNT(*)
        FROM t_user
        WHERE last_login_time >= date_trunc('day', now())
          AND last_login_time < date_trunc('day', now()) + interval '1 day'
        """)
    Long countActiveUsersToday();

    @Select("""
        WITH months AS (
          SELECT date_trunc('month', now()) - (interval '1 month' * gs) AS month_start
          FROM generate_series(0, #{months} - 1) AS gs
        )
        SELECT to_char(month_start, 'YYYY-MM') AS period,
               COALESCE(COUNT(u.id), 0) AS count
        FROM months m
        LEFT JOIN t_user u
          ON u.create_time >= m.month_start
         AND u.create_time < m.month_start + interval '1 month'
        GROUP BY month_start
        ORDER BY month_start
        """)
    List<Map<String, Object>> countNewUsersByMonth(@Param("months") Integer months);

    @Select("""
        SELECT COUNT(*)
        FROM t_user
        WHERE create_time >= date_trunc('month', now())
          AND create_time < date_trunc('month', now()) + interval '1 month'
        """)
    Long countNewUsersThisMonth();

    @Select("""
        SELECT COUNT(*)
        FROM t_user
        WHERE create_time >= date_trunc('week', now())
          AND create_time < date_trunc('week', now()) + interval '1 week'
        """)
    Long countNewUsersThisWeek();

    @Select("""
        SELECT COUNT(*)
        FROM t_user
        WHERE last_login_time >= date_trunc('week', now()) - interval '1 week'
          AND last_login_time < date_trunc('week', now())
        """)
    Long countActiveUsersLastWeek();
}
