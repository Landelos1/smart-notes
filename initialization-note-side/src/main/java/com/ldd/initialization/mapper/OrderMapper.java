package com.ldd.initialization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ldd.initialization.domain.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 订单Mapper
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    @Select("""
        SELECT COUNT(*)
        FROM t_order
        WHERE status = 1
          AND pay_time >= date_trunc('day', now())
          AND pay_time < date_trunc('day', now()) + interval '1 day'
        """)
    Long countTodayPaidOrders();

    @Select("""
        WITH months AS (
          SELECT date_trunc('month', now()) - (interval '1 month' * gs) AS month_start
          FROM generate_series(0, #{months} - 1) AS gs
        )
        SELECT to_char(month_start, 'YYYY-MM') AS period,
               COALESCE(SUM(CASE WHEN mp.duration_months = 1 THEN 1 ELSE 0 END), 0) AS monthly_count,
               COALESCE(SUM(CASE WHEN mp.duration_months = 3 THEN 1 ELSE 0 END), 0) AS quarterly_count,
               COALESCE(SUM(CASE WHEN mp.duration_months >= 12 THEN 1 ELSE 0 END), 0) AS yearly_count,
               COALESCE(SUM(o.amount), 0) AS total_amount
        FROM months m
        LEFT JOIN t_order o
          ON o.pay_time >= m.month_start
         AND o.pay_time < m.month_start + interval '1 month'
         AND o.status = 1
        LEFT JOIN t_member_plan mp
          ON o.plan_id = mp.id
        GROUP BY month_start
        ORDER BY month_start
        """)
    List<Map<String, Object>> countPaidOrdersByMonth(@Param("months") Integer months);

    @Select("""
        SELECT COALESCE(SUM(amount), 0)
        FROM t_order
        WHERE status = 1
          AND pay_time >= date_trunc('month', now())
          AND pay_time < date_trunc('month', now()) + interval '1 month'
        """)
    BigDecimal sumPaidAmountThisMonth();

    @Select("""
        SELECT COUNT(*)
        FROM t_order
        WHERE status = 1
          AND pay_time >= date_trunc('week', now()) - interval '1 week'
          AND pay_time < date_trunc('week', now())
        """)
    Long countLastWeekPaidOrders();
}
