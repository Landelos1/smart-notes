package com.ldd.initialization.vo;

import lombok.Data;

/**
 * 模板分享信息VO
 */
@Data
public class TemplateShareVO {
    
    /**
     * 分享链接
     */
    private String shareUrl;
    
    /**
     * 二维码图片URL
     */
    private String qrCodeUrl;
    
    /**
     * 分享标题
     */
    private String shareTitle;
    
    /**
     * 分享描述
     */
    private String shareDescription;
}
