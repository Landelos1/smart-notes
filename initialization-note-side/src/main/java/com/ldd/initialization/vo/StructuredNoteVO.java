package com.ldd.initialization.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 从 PDF 或图片资料中整理出的结构化笔记。
 */
@Data
public class StructuredNoteVO {
    private String sourceFileName;
    private String title;
    private String summary;
    private List<String> keyPoints = new ArrayList<>();
    private List<String> tags = new ArrayList<>();
    private List<String> actionItems = new ArrayList<>();
    private String markdown;
    private Integer pageCount;
    private String extractionMode;
}
