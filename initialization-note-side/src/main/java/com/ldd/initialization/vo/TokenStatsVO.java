package com.ldd.initialization.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class TokenStatsVO implements Serializable {
    private Long todayTokens;
    private Long monthTokens;
    private Long totalTokens;
    private List<TokenTrendItem> monthlyTrend;
}
