package com.ldd.initialization.service.impl;

import com.ldd.initialization.mapper.ChatMessageMapper;
import com.ldd.initialization.service.TokenStatsService;
import com.ldd.initialization.vo.TokenStatsVO;
import com.ldd.initialization.vo.TokenTrendItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TokenStatsServiceImpl implements TokenStatsService {

    private final ChatMessageMapper chatMessageMapper;

    @Override
    public TokenStatsVO getTokenStats(Long userId) {
        TokenStatsVO stats = new TokenStatsVO();
        Long todayTokens = chatMessageMapper.sumTodayTokensByUserId(userId);
        Long monthTokens = chatMessageMapper.sumMonthTokensByUserId(userId);
        Long totalTokens = chatMessageMapper.sumTotalTokensByUserId(userId);
        List<TokenTrendItem> trend = Optional.ofNullable(chatMessageMapper.listMonthlyTokensByUserId(userId))
            .orElse(List.of());

        stats.setTodayTokens(todayTokens != null ? todayTokens : 0L);
        stats.setMonthTokens(monthTokens != null ? monthTokens : 0L);
        stats.setTotalTokens(totalTokens != null ? totalTokens : 0L);
        stats.setMonthlyTrend(trend);
        return stats;
    }

    @Override
    public TokenStatsVO getGlobalTokenStats() {
        TokenStatsVO stats = new TokenStatsVO();
        Long todayTokens = chatMessageMapper.sumTodayTokensGlobal();
        Long monthTokens = chatMessageMapper.sumMonthTokensGlobal();
        Long totalTokens = chatMessageMapper.sumTotalTokensGlobal();
        List<TokenTrendItem> trend = Optional.ofNullable(chatMessageMapper.listMonthlyTokensGlobal())
            .orElse(List.of());

        stats.setTodayTokens(todayTokens != null ? todayTokens : 0L);
        stats.setMonthTokens(monthTokens != null ? monthTokens : 0L);
        stats.setTotalTokens(totalTokens != null ? totalTokens : 0L);
        stats.setMonthlyTrend(trend);
        return stats;
    }
}
