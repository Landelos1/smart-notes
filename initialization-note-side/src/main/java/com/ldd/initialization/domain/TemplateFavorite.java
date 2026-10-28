package com.ldd.initialization.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模板收藏实体类
 */
@Data
@TableName("t_template_favorite")
public class TemplateFavorite {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 模板ID
     */
    private Long templateId;
    
    /**
     * 收藏时间
     */
    private LocalDateTime createTime;
}
