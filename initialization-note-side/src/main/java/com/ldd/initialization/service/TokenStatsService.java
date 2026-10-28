package com.ldd.initialization.service;

import com.ldd.initialization.vo.TokenStatsVO;

public interface TokenStatsService {
    TokenStatsVO getTokenStats(Long userId);

    TokenStatsVO getGlobalTokenStats();
}
