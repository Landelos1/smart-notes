package com.ldd.initialization.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 回收站项目VO
 */
@Data
public class TrashItemVO {
    
    /**
     * 回收站记录ID
     */
    private Long id;

    /**
     * 回收站类型：1=笔记，2=文件
     */
    private Integer trashType;

    /**
     * 类型名称：笔记/文件
     */
    private String typeName;

    /**
     * 源对象ID
     */
    private Long sourceId;

    /**
     * 显示标题（笔记标题或文件名）
     */
    private String title;

    /**
     * 删除时间
     */
    private LocalDateTime deleteTime;

    /**
     * 恢复截止时间
     */
    private LocalDateTime restoreDeadline;

    /**
     * 剩余天数
     */
    private Integer daysLeft;

    /**
     * 原创建时间
     */
    private LocalDateTime originalCreateTime;

    // ========== 笔记特有字段 ==========
    
    /**
     * 笔记本ID
     */
    private Long notebookId;

    /**
     * 笔记本名称
     */
    private String notebookName;

    /**
     * 笔记摘要
     */
    private String summary;

    // ========== 文件特有字段 ==========
    
    /**
     * 文件扩展名
     */
    private String fileExtension;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件大小格式化
     */
    private String fileSizeFormatted;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 用户ID
     */
    private Long userId;
}
