package com.ldd.initialization.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 会员配额信息VO
 */
@Data
public class MembershipQuotaVO implements Serializable {
    
    /**
     * AI调用总配额
     */
    private Integer aiCallQuota;
    
    /**
     * 已使用AI调用次数
     */
    private Integer aiCallUsed;
    
    /**
     * 剩余AI调用次数
     */
    private Integer aiCallRemaining;
    
    /**
     * 下次重置时间
     */
    private String nextResetTime;
    
    /**
     * 知识库总配额
     */
    private Integer kbQuota;
    
    /**
     * 已使用知识库数量
     */
    private Integer kbUsed;
    
    /**
     * 剩余可创建知识库数量
     */
    private Integer kbRemaining;
    
    /**
     * 是否无限制知识库
     */
    private Boolean isUnlimitedKb;
    
    /**
     * 会员过期时间
     */
    private String memberExpireTime;
    
    /**
     * 会员是否有效
     */
    private Boolean isActive;
    
    private static final long serialVersionUID = 1L;
}
