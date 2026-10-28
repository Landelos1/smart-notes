package com.ldd.initialization.service;

import com.ldd.initialization.vo.DashboardOverviewVO;

import java.util.Map;

public interface DashboardService {
    DashboardOverviewVO getOverview();

    Map<String, Object> getUserGrowth();

    Map<String, Object> getNoteSquareStats();

    Map<String, Object> getDynamics();

    Map<String, Object> getOrderStats();
}
