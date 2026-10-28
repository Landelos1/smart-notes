package com.ldd.initialization.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ldd.initialization.domain.UserMembership;
import com.ldd.initialization.mapper.SharedKnowledgeBaseMapper;
import com.ldd.initialization.mapper.UserMembershipMapper;
import com.ldd.initialization.service.MembershipQuotaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

/**
 * 会员配额管理服务实现类
 */
@Slf4j
@Service
public class MembershipQuotaServiceImpl implements MembershipQuotaService {

    @Resource
    private UserMembershipMapper userMembershipMapper;

    @Resource
    private SharedKnowledgeBaseMapper sharedKnowledgeBaseMapper;

    /**
     * 检查并重置AI调用配额（如果需要）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean checkAndResetAIQuota(UserMembership membership) {
        if (membership == null || membership.getLastAiResetTime() == null) {
            return false;
        }

        Date lastResetTime = membership.getLastAiResetTime();
        Date now = new Date();

        // 判断是否跨月
        Calendar lastCal = Calendar.getInstance();
        lastCal.setTime(lastResetTime);
        
        Calendar nowCal = Calendar.getInstance();
        nowCal.setTime(now);

        int lastMonth = lastCal.get(Calendar.MONTH);
        int lastYear = lastCal.get(Calendar.YEAR);
        int nowMonth = nowCal.get(Calendar.MONTH);
        int nowYear = nowCal.get(Calendar.YEAR);

        // 如果跨月了，重置配额
        if (nowYear > lastYear || (nowYear == lastYear && nowMonth > lastMonth)) {
            // 重置配额
            UpdateWrapper<UserMembership> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("user_id", membership.getUserId())
                    .set("ai_call_used", 0)
                    .set("last_ai_reset_time", now);
            
            int updated = userMembershipMapper.update(null, updateWrapper);
            if (updated > 0) {
                log.info("用户 {} 的AI调用配额已重置", membership.getUserId());
                // 清除缓存
                evictQuotaCache(membership.getUserId());
                return true;
            }
        }
        
        return false;
    }

    /**
     * 检查AI调用配额是否充足
     */
    @Override
    public boolean checkAIQuotaAvailable(Long userId) {
        UserMembership membership = getMembershipQuota(userId);
        if (membership == null) {
            // 如果不存在会员记录，创建默认记录
            membership = createDefaultMembership(userId);
        }

        // 先检查是否需要重置配额
        checkAndResetAIQuota(membership);

        // 重新查询最新数据
        membership = getMembershipQuota(userId);
        if (membership == null) {
            return false;
        }

        Integer quota = membership.getAiCallQuota();
        Integer used = membership.getAiCallUsed();

        if (quota == null || used == null) {
            return false;
        }

        return used < quota;
    }

    /**
     * 增加AI调用使用次数（使用行级锁保证并发安全）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "membershipQuota", key = "#userId")
    public int incrementAICallUsed(Long userId) {
        // 使用乐观锁更新
        UpdateWrapper<UserMembership> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("user_id", userId)
                .setSql("ai_call_used = ai_call_used + 1");
        
        int updated = userMembershipMapper.update(null, updateWrapper);
        
        if (updated > 0) {
            UserMembership membership = getMembershipQuota(userId);
            return membership != null ? membership.getAiCallUsed() : 0;
        }
        
        return 0;
    }

    /**
     * 检查知识库创建配额是否充足
     */
    @Override
    public boolean checkKbQuotaAvailable(Long userId) {
        UserMembership membership = getMembershipQuota(userId);
        if (membership == null) {
            membership = createDefaultMembership(userId);
        }

        if (membership == null) {
            return false;
        }

        Integer quota = membership.getKbQuota();
        Integer used = (int) Math.min(Integer.MAX_VALUE, sharedKnowledgeBaseMapper.countCreatedByUserId(userId));

        // -1 表示无限制
        if (quota == null || quota == -1) {
            return true;
        }

        return used < quota;
    }

    /**
     * 增加知识库使用数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "membershipQuota", key = "#userId")
    public int incrementKbUsed(Long userId) {
        UpdateWrapper<UserMembership> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("user_id", userId)
                .setSql("kb_used = kb_used + 1");
        
        int updated = userMembershipMapper.update(null, updateWrapper);
        
        if (updated > 0) {
            UserMembership membership = getMembershipQuota(userId);
            return membership != null ? membership.getKbUsed() : 0;
        }
        
        return 0;
    }

    /**
     * 减少知识库使用数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "membershipQuota", key = "#userId")
    public int decrementKbUsed(Long userId) {
        UpdateWrapper<UserMembership> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("user_id", userId)
                .ne("kb_quota", -1)  // 只有非无限制的才需要减少
                .setSql("kb_used = CASE WHEN kb_used > 0 THEN kb_used - 1 ELSE 0 END");
        
        int updated = userMembershipMapper.update(null, updateWrapper);
        
        if (updated > 0) {
            UserMembership membership = getMembershipQuota(userId);
            return membership != null ? membership.getKbUsed() : 0;
        }
        
        return 0;
    }

    /**
     * 获取用户会员配额信息（使用缓存）
     */
    @Override
    @Cacheable(value = "membershipQuota", key = "#userId", unless = "#result == null")
    public UserMembership getMembershipQuota(Long userId) {
        QueryWrapper<UserMembership> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        return userMembershipMapper.selectOne(queryWrapper);
    }

    /**
     * 批量重置所有用户的AI调用配额
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int resetAllAIQuota() {
        // 获取当前月份第一天
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        Date resetTime = Date.from(firstDayOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant());

        UpdateWrapper<UserMembership> updateWrapper = new UpdateWrapper<>();
        // 只重置上次重置时间在本月之前的记录
        updateWrapper.lt("last_ai_reset_time", resetTime)
                .set("ai_call_used", 0)
                .set("last_ai_reset_time", new Date());

        int updated = userMembershipMapper.update(null, updateWrapper);
        log.info("批量重置AI调用配额完成，影响用户数: {}", updated);
        
        return updated;
    }

    /**
     * 创建默认会员记录
     */
    private UserMembership createDefaultMembership(Long userId) {
        UserMembership membership = new UserMembership();
        membership.setUserId(userId);
        membership.setAiCallQuota(50);  // 普通会员默认50次
        membership.setAiCallUsed(0);
        membership.setLastAiResetTime(new Date());
        membership.setKbQuota(3);  // 普通会员默认3个知识库
        membership.setKbUsed(0);
        membership.setIsActive(true);
        membership.setCreateTime(new Date());
        membership.setUpdateTime(new Date());
        
        try {
            userMembershipMapper.insert(membership);
            log.info("为用户 {} 创建默认会员记录", userId);
            return membership;
        } catch (Exception e) {
            log.error("创建默认会员记录失败", e);
            return null;
        }
    }

    /**
     * 清除配额缓存
     */
    @CacheEvict(value = "membershipQuota", key = "#userId")
    private void evictQuotaCache(Long userId) {
        log.debug("清除用户 {} 的配额缓存", userId);
    }
}
