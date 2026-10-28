package com.ldd.initialization.service;

public interface RequestStatsService {
    Long getTodayRequestCount();

    Long getLastWeekRequestCount();
}
