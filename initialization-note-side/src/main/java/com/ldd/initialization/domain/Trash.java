package com.ldd.initialization.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 回收站实体类
 * 
 * @TableName t_trash
 */
@TableName(value = "t_trash")
@Data
public class Trash implements Serializable {
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 回收站类型：1=笔记，2=文件
     */
    @TableField(value = "trash_type")
    private Integer trashType;

    /**
     * 源对象ID（笔记ID或文件ID）
     */
    @TableField(value = "source_id")
    private Long sourceId;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 删除时间
     */
    @TableField(value = "delete_time")
    private LocalDateTime deleteTime;

    /**
     * 恢复截止时间（30天后自动删除）
     */
    @TableField(value = "restore_deadline")
    private LocalDateTime restoreDeadline;

    // ========== 笔记相关字段 ==========
    
    /**
     * 笔记标题
     */
    @TableField(value = "note_title")
    private String noteTitle;

    /**
     * 笔记Markdown内容
     */
    @TableField(value = "note_content_md")
    private String noteContentMd;

    /**
     * 笔记HTML内容
     */
    @TableField(value = "note_content_html")
    private String noteContentHtml;

    /**
     * 原所属笔记本ID
     */
    @TableField(value = "notebook_id")
    private Long notebookId;

    /**
     * 笔记是否置顶
     */
    @TableField(value = "note_is_pinned")
    private Boolean noteIsPinned;

    /**
     * 笔记状态
     */
    @TableField(value = "note_status")
    private Integer noteStatus;

    /**
     * 笔记浏览次数
     */
    @TableField(value = "note_view_count")
    private Integer noteViewCount;

    /**
     * 笔记字数统计
     */
    @TableField(value = "note_word_count")
    private Integer noteWordCount;

    /**
     * 笔记创建时间
     */
    @TableField(value = "note_create_time")
    private LocalDateTime noteCreateTime;

    /**
     * 笔记最后更新时间
     */
    @TableField(value = "note_update_time")
    private LocalDateTime noteUpdateTime;

    // ========== 文件相关字段 ==========
    
    /**
     * 原始文件名
     */
    @TableField(value = "file_original_filename")
    private String fileOriginalFilename;

    /**
     * 存储在服务器上的文件名
     */
    @TableField(value = "file_name")
    private String fileName;

    /**
     * 文件扩展名
     */
    @TableField(value = "file_extension")
    private String fileExtension;

    /**
     * 文件大小（以字节为单位）
     */
    @TableField(value = "file_size")
    private Long fileSize;

    /**
     * 文件在服务器上的物理路径
     */
    @TableField(value = "file_path")
    private String filePath;

    /**
     * 文件的可访问URL
     */
    @TableField(value = "file_url")
    private String fileUrl;

    /**
     * 文件的MD5哈希值
     */
    @TableField(value = "file_md5")
    private String fileMd5;

    /**
     * 文件最后更新时间
     */
    @TableField(value = "file_update_time")
    private LocalDateTime fileUpdateTime;

    /**
     * 文件类型
     */
    @TableField(value = "file_type")
    private String fileType;

    /**
     * 文件的MIME类型
     */
    @TableField(value = "mime_type")
    private String mimeType;

    /**
     * 文件的哈希值
     */
    @TableField(value = "file_hash")
    private String fileHash;

    /**
     * 来源类型（1=个人知识库，2=共享知识库）
     */
    @TableField(value = "source_type")
    private Integer sourceType;

    /**
     * 来源笔记ID
     */
    @TableField(value = "source_note_id")
    private Long sourceNoteId;

    /**
     * 来源笔记标题
     */
    @TableField(value = "source_note_title")
    private String sourceNoteTitle;

    /**
     * 文件创建时间
     */
    @TableField(value = "file_create_time")
    private LocalDateTime fileCreateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 回收站类型枚举
     */
    public static final int TYPE_NOTE = 1;
    public static final int TYPE_FILE = 2;
}
