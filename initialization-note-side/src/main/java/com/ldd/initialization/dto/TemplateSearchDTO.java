package com.ldd.initialization.dto;

import lombok.Data;

/**
 * 模板搜索请求DTO
 */
@Data
public class TemplateSearchDTO {

    /**
     * 分类（all表示全部）
     */
    private String category;

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 页码（从1开始）
     */
    private Integer page = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 20;
}
