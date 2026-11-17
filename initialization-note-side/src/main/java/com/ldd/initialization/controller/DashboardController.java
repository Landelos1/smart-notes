package com.ldd.initialization.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.ldd.initialization.result.Result;
import com.ldd.initialization.service.DashboardService;
import com.ldd.initialization.vo.DashboardOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 获取数据看板总览信息
     *
     * @return 看板总览数据
     */
    @GetMapping("/overview")
    public Result<DashboardOverviewVO> getOverview() {
        return Result.success(dashboardService.getOverview());
    }

    /**
     * 获取用户增长趋势数据
     *
     * @return 用户增长统计数据
     */
    @GetMapping("/user-growth")
    public Result<Map<String, Object>> getUserGrowth() {
        return Result.success(dashboardService.getUserGrowth());
    }

    /**
     * 获取笔记广场统计数据
     *
     * @return 笔记广场统计信息
     */
    @GetMapping("/note-square-stats")
    public Result<Map<String, Object>> getNoteSquareStats() {
        return Result.success(dashboardService.getNoteSquareStats());
    }

    /**
     * 获取看板动态信息
     *
     * @return 动态列表及新增数量
     */
    @GetMapping("/dynamics")
    public Result<Map<String, Object>> getDynamics() {
        return Result.success(dashboardService.getDynamics());
    }

    /**
     * 获取订单统计数据
     *
     * @return 订单统计信息
     */
    @GetMapping("/order-stats")
    public Result<Map<String, Object>> getOrderStats() {
        return Result.success(dashboardService.getOrderStats());
    }
}
