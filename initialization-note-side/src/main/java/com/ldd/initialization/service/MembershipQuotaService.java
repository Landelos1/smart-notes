package com.ldd.initialization.service;

import com.ldd.initialization.domain.UserMembership;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 会员配额管理服务接口
 * 负责AI调用配额和知识库配额的管理
 */
@Service
public interface MembershipQuotaService {

    /**
     * 检查并重置AI调用配额（如果需要）
     * 判断是否跨月，如果跨月则重置配额
     * 
     * @param membership 会员信息
     * @return 是否执行了重置
     */
    boolean checkAndResetAIQuota(UserMembership membership);

    /**
     * 检查AI调用配额是否充足
     * 
     * @param userId 用户ID
     * @return true-配额充足，false-配额不足
     */
    boolean checkAIQuotaAvailable(Long userId);

    /**
     * 增加AI调用使用次数
     * 
     * @param userId 用户ID
     * @return 更新后的使用次数
     */
    int incrementAICallUsed(Long userId);

    /**
     * 检查知识库创建配额是否充足
     * 
     * @param userId 用户ID
     * @return true-配额充足，false-配额不足
     */
    boolean checkKbQuotaAvailable(Long userId);

    /**
     * 增加知识库使用数量
     * 
     * @param userId 用户ID
     * @return 更新后的使用数量
     */
    int incrementKbUsed(Long userId);

    /**
     * 减少知识库使用数量
     * 
     * @param userId 用户ID
     * @return 更新后的使用数量
     */
    int decrementKbUsed(Long userId);

    /**
     * 获取用户会员配额信息
     * 
     * @param userId 用户ID
     * @return 会员配额信息
     */
    UserMembership getMembershipQuota(Long userId);

    /**
     * 批量重置所有用户的AI调用配额（定时任务调用）
     * 
     * @return 重置的用户数量
     */
    int resetAllAIQuota();

    /**
     * 计算下次配额重置时间
     * 
     * @return 下月1号的日期
     */
    default LocalDate getNextResetDate() {
        LocalDate now = LocalDate.now();
        return now.plusMonths(1).withDayOfMonth(1);
    }
}
