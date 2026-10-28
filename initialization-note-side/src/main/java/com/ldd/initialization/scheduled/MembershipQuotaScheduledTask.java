package com.ldd.initialization.scheduled;

import com.ldd.initialization.service.MembershipQuotaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 会员配额定时任务
 * 负责定期重置用户的AI调用配额
 */
@Slf4j
@Component
public class MembershipQuotaScheduledTask {

    @Resource
    private MembershipQuotaService membershipQuotaService;

    /**
     * 每月1号凌晨00:00:00执行配额重置
     * Cron表达式：0 0 0 1 * ?
     * 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    public void resetMonthlyAIQuota() {
        log.info("========== 开始执行每月AI调用配额重置任务 ==========");
        
        try {
            int resetCount = membershipQuotaService.resetAllAIQuota();
            log.info("每月AI调用配额重置完成，影响用户数: {}", resetCount);
        } catch (Exception e) {
            log.error("每月AI调用配额重置失败", e);
        }
        
        log.info("========== 每月AI调用配额重置任务结束 ==========");
    }

    /**
     * 每天凌晨3点检查并修复配额数据一致性（可选）
     * Cron表达式：0 0 3 * * ?
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void checkQuotaConsistency() {
        log.info("========== 开始执行配额数据一致性检查 ==========");
        
        // 这里可以添加数据一致性检查逻辑
        // 例如：检查kb_used是否与实际知识库数量一致
        
        log.info("========== 配额数据一致性检查结束 ==========");
    }
}
