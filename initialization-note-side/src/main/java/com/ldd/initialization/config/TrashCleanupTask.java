package com.ldd.initialization.config;

import com.ldd.initialization.service.TrashService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 回收站自动清理定时任务
 * 每天凌晨2点执行，清理超过30天的回收站数据
 */
@Component
@EnableScheduling
@Slf4j
public class TrashCleanupTask {

    @Autowired
    private TrashService trashService;

    /**
     * 每天凌晨2点执行清理任务
     * cron表达式: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredTrash() {
        log.info("开始执行回收站自动清理任务...");
        try {
            int cleanedCount = trashService.cleanExpiredTrash();
            log.info("回收站自动清理任务完成，清理了 {} 条过期记录", cleanedCount);
        } catch (Exception e) {
            log.error("回收站自动清理任务执行失败", e);
        }
    }
}
