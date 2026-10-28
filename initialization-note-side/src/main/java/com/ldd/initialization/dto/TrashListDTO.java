package com.ldd.initialization.dto;

import lombok.Data;

/**
 * 回收站列表查询DTO
 */
@Data
public class TrashListDTO {
    
    /**
     * 页码
     */
    private Integer page = 1;

    /**
     * 每页大小
     */
    private Integer size = 10;

    /**
     * 回收站类型筛选：1=笔记，2=文件，null=全部
     */
    private Integer trashType;

    /**
     * 搜索关键词（按标题/文件名搜索）
     */
    private String keyword;

    /**
     * 排序字段
     */
    private String sortBy = "delete_time";

    /**
     * 排序方向 asc/desc
     */
    private String sortOrder = "desc";
}
