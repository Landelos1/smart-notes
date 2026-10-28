package com.ldd.initialization.service.impl;

import com.ldd.initialization.mapper.FileInfoMapper;
import com.ldd.initialization.mapper.NoteCommentMapper;
import com.ldd.initialization.mapper.NoteFavoriteMapper;
import com.ldd.initialization.mapper.NoteLikeMapper;
import com.ldd.initialization.mapper.NoteMapper;
import com.ldd.initialization.mapper.NoteSquareMapper;
import com.ldd.initialization.mapper.OrderMapper;
import com.ldd.initialization.mapper.SharedKnowledgeBaseMapper;
import com.ldd.initialization.mapper.UserMapper;
import com.ldd.initialization.service.DashboardService;
import com.ldd.initialization.service.RequestStatsService;
import com.ldd.initialization.vo.DashboardOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final int USER_GROWTH_MONTHS = 9;
    private static final int NOTE_SQUARE_DAYS = 7;
    private static final int ORDER_STATS_MONTHS = 6;
    private static final int DYNAMIC_LIMIT = 8;
    private static final int DYNAMIC_QUERY_LIMIT = 20;

    private final UserMapper userMapper;
    private final NoteMapper noteMapper;
    private final OrderMapper orderMapper;
    private final RequestStatsService requestStatsService;
    private final NoteSquareMapper noteSquareMapper;
    private final NoteLikeMapper noteLikeMapper;
    private final NoteFavoriteMapper noteFavoriteMapper;
    private final NoteCommentMapper noteCommentMapper;
    private final FileInfoMapper fileInfoMapper;
    private final SharedKnowledgeBaseMapper sharedKnowledgeBaseMapper;

    @Override
    public DashboardOverviewVO getOverview() {
        DashboardOverviewVO overview = new DashboardOverviewVO();
        Long activeUsers = userMapper.countActiveUsersToday();
        Long newNotes = noteMapper.countTodayNotes();
        Long paidOrders = orderMapper.countTodayPaidOrders();
        Long requestCount = requestStatsService.getTodayRequestCount();

        Long lastWeekActiveUsers = userMapper.countActiveUsersLastWeek();
        Long lastWeekNotes = noteMapper.countLastWeekNotes();
        Long lastWeekOrders = orderMapper.countLastWeekPaidOrders();
        Long lastWeekRequests = requestStatsService.getLastWeekRequestCount();

        overview.setActiveUsers(activeUsers != null ? activeUsers : 0L);
        overview.setNewNotes(newNotes != null ? newNotes : 0L);
        overview.setPaidOrders(paidOrders != null ? paidOrders : 0L);
        overview.setRequestCount(requestCount != null ? requestCount : 0L);

        overview.setActiveUsersChange(calcChange(activeUsers, lastWeekActiveUsers));
        overview.setNewNotesChange(calcChange(newNotes, lastWeekNotes));
        overview.setPaidOrdersChange(calcChange(paidOrders, lastWeekOrders));
        overview.setRequestCountChange(calcChange(requestCount, lastWeekRequests));
        return overview;
    }

    private String calcChange(Long current, Long lastWeek) {
        long cur = current != null ? current : 0L;
        long last = lastWeek != null ? lastWeek : 0L;
        if (last == 0) {
            return cur > 0 ? "+100%" : "0%";
        }
        long diff = cur - last;
        int percent = (int) Math.round((diff * 100.0) / last);
        return percent >= 0 ? "+" + percent + "%" : percent + "%";
    }

    @Override
    public Map<String, Object> getUserGrowth() {
        List<Map<String, Object>> trend = userMapper.countNewUsersByMonth(USER_GROWTH_MONTHS);
        List<String> periods = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (Map<String, Object> item : trend) {
            periods.add(String.valueOf(item.get("period")));
            counts.add(toLong(item.get("count")));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("periods", periods);
        result.put("counts", counts);
        result.put("monthNew", safeLong(userMapper.countNewUsersThisMonth()));
        result.put("weekNew", safeLong(userMapper.countNewUsersThisWeek()));
        return result;
    }

    @Override
    public Map<String, Object> getNoteSquareStats() {
        List<Map<String, Object>> published = noteSquareMapper.countDailyPublished(NOTE_SQUARE_DAYS);
        List<Map<String, Object>> likes = noteLikeMapper.countDailyLikes(NOTE_SQUARE_DAYS);
        List<Map<String, Object>> favorites = noteFavoriteMapper.countDailyFavorites(NOTE_SQUARE_DAYS);
        List<Map<String, Object>> comments = noteCommentMapper.countDailyComments(NOTE_SQUARE_DAYS);

        Map<String, Long> likeMap = toCountMap(likes);
        Map<String, Long> favoriteMap = toCountMap(favorites);
        Map<String, Long> commentMap = toCountMap(comments);

        List<String> periods = new ArrayList<>();
        List<Long> publishCounts = new ArrayList<>();
        List<Long> interactionCounts = new ArrayList<>();
        for (Map<String, Object> item : published) {
            String period = String.valueOf(item.get("period"));
            long publishCount = toLong(item.get("count"));
            long interactions = likeMap.getOrDefault(period, 0L)
                    + favoriteMap.getOrDefault(period, 0L)
                    + commentMap.getOrDefault(period, 0L);
            periods.add(period);
            publishCounts.add(publishCount);
            interactionCounts.add(interactions);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("periods", periods);
        result.put("publishCounts", publishCounts);
        result.put("interactionCounts", interactionCounts);
        return result;
    }

    @Override
    public Map<String, Object> getDynamics() {
        List<Map<String, Object>> combined = new ArrayList<>();
        appendDynamics(combined, noteMapper.selectRecentNotes(DYNAMIC_QUERY_LIMIT), "创建了笔记");
        appendDynamics(combined, fileInfoMapper.selectRecentUploads(DYNAMIC_QUERY_LIMIT), "上传了文档");
        appendDynamics(combined, sharedKnowledgeBaseMapper.selectRecentKnowledgeBases(DYNAMIC_QUERY_LIMIT), "创建了共享知识库");
        appendDynamics(combined, noteSquareMapper.selectRecentPublished(DYNAMIC_QUERY_LIMIT), "发布了笔记");
        appendDynamics(combined, noteCommentMapper.selectRecentComments(DYNAMIC_QUERY_LIMIT), "评论了笔记");

        combined.sort((a, b) -> toTime(b.get("time")).compareTo(toTime(a.get("time"))));

        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        long newCount = combined.stream()
                .map(item -> toTime(item.get("time")))
                .filter(time -> !time.isBefore(todayStart))
                .count();

        List<Map<String, Object>> items = new ArrayList<>();
        int limit = Math.min(DYNAMIC_LIMIT, combined.size());
        for (int i = 0; i < limit; i++) {
            Map<String, Object> item = combined.get(i);
            Map<String, Object> out = new HashMap<>();
            out.put("username", item.getOrDefault("username", ""));
            out.put("type", item.getOrDefault("type", ""));
            out.put("target", item.getOrDefault("target", ""));
            items.add(out);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("newCount", newCount);
        result.put("list", items);
        return result;
    }

    @Override
    public Map<String, Object> getOrderStats() {
        List<Map<String, Object>> stats = orderMapper.countPaidOrdersByMonth(ORDER_STATS_MONTHS);
        List<String> periods = new ArrayList<>();
        List<Long> monthlyCounts = new ArrayList<>();
        List<Long> quarterlyCounts = new ArrayList<>();
        List<Long> yearlyCounts = new ArrayList<>();
        List<BigDecimal> amounts = new ArrayList<>();

        for (Map<String, Object> item : stats) {
            periods.add(String.valueOf(item.get("period")));
            monthlyCounts.add(toLong(item.get("monthly_count")));
            quarterlyCounts.add(toLong(item.get("quarterly_count")));
            yearlyCounts.add(toLong(item.get("yearly_count")));
            amounts.add(toBigDecimal(item.get("total_amount")));
        }

        BigDecimal monthAmount = orderMapper.sumPaidAmountThisMonth();

        Map<String, Object> result = new HashMap<>();
        result.put("periods", periods);
        result.put("monthlyCounts", monthlyCounts);
        result.put("quarterlyCounts", quarterlyCounts);
        result.put("yearlyCounts", yearlyCounts);
        result.put("amounts", amounts);
        result.put("monthAmount", monthAmount != null ? monthAmount : BigDecimal.ZERO);
        return result;
    }

    private void appendDynamics(List<Map<String, Object>> target, List<Map<String, Object>> source, String type) {
        if (source == null || source.isEmpty()) {
            return;
        }
        for (Map<String, Object> item : source) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("username", item.get("username"));
            entry.put("type", type);
            entry.put("target", item.get("target"));
            entry.put("time", item.get("time"));
            target.add(entry);
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private Map<String, Long> toCountMap(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Long> result = new HashMap<>();
        for (Map<String, Object> item : items) {
            result.put(String.valueOf(item.get("period")), toLong(item.get("count")));
        }
        return result;
    }

    private long safeLong(Long value) {
        return value != null ? value : 0L;
    }

    private long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private LocalDateTime toTime(Object value) {
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }
        if (value instanceof java.util.Date) {
            return LocalDateTime.ofInstant(((java.util.Date) value).toInstant(), ZoneId.systemDefault());
        }
        return LocalDateTime.MIN;
    }
}
