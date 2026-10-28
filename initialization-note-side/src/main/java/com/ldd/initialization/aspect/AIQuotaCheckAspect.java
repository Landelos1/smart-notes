package com.ldd.initialization.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.ldd.initialization.domain.UserMembership;
import com.ldd.initialization.exception.QuotaExceededException;
import com.ldd.initialization.service.MembershipQuotaService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * AI调用配额检查切面
 * 拦截AI相关接口，进行配额校验
 */
@Slf4j
@Aspect
@Component
public class AIQuotaCheckAspect {

    @Resource
    private MembershipQuotaService membershipQuotaService;

    /**
     * 拦截AI聊天流式接口
     */
    @Around("execution(* com.ldd.initialization.controller.ChatController.chatStream(..))")
    public Object checkAIQuotaForChatStream(ProceedingJoinPoint joinPoint) throws Throwable {
        return checkAndProceed(joinPoint, "AI聊天");
    }

    /**
     * 拦截共享知识库AI对话接口
     */
    @Around("execution(* com.ldd.initialization.controller.SharedKnowledgeBaseController.chatWithKnowledgeBase(..))")
    public Object checkAIQuotaForKbChat(ProceedingJoinPoint joinPoint) throws Throwable {
        return checkAndProceed(joinPoint, "知识库AI对话");
    }

    /**
     * 拦截笔记AI助手接口
     */
    @Around("execution(* com.ldd.initialization.controller.NoteController.processNoteAI(..))")
    public Object checkAIQuotaForNoteAI(ProceedingJoinPoint joinPoint) throws Throwable {
        return checkAndProceed(joinPoint, "笔记AI助手");
    }

    /**
     * 统一的配额检查和执行逻辑
     */
    private Object checkAndProceed(ProceedingJoinPoint joinPoint, String actionName) throws Throwable {
        // 获取当前用户ID
        Long userId = StpUtil.getLoginIdAsLong();
        
        log.info("用户 {} 调用 {}", userId, actionName);

        // 检查AI调用配额
        if (!membershipQuotaService.checkAIQuotaAvailable(userId)) {
            // 配额不足，获取详细信息用于错误提示
            UserMembership membership = membershipQuotaService.getMembershipQuota(userId);
            
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("quotaUsed", membership.getAiCallUsed());
            errorData.put("quotaTotal", membership.getAiCallQuota());
            
            LocalDate nextResetDate = membershipQuotaService.getNextResetDate();
            errorData.put("nextResetTime", nextResetDate.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            log.warn("用户 {} AI调用配额不足: {}/{}", userId, membership.getAiCallUsed(), membership.getAiCallQuota());
            
            throw new QuotaExceededException(
                "AI调用次数已用尽，请升级会员或等待下月重置",
                40301,
                errorData
            );
        }

        // 配额充足，执行原方法
        Object result = joinPoint.proceed();

        // 执行成功后，递增配额使用次数
        try {
            int newUsed = membershipQuotaService.incrementAICallUsed(userId);
            log.info("用户 {} {} 成功，当前已使用: {}", userId, actionName, newUsed);
        } catch (Exception e) {
            log.error("递增AI调用使用次数失败", e);
            // 不影响主流程，记录日志即可
        }

        return result;
    }
}
