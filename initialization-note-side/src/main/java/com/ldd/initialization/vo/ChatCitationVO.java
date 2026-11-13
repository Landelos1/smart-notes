package com.ldd.initialization.vo;

import lombok.Data;

@Data
public class ChatCitationVO {
    private Long fileId;
    private String title;
    /** 检索到的原文摘录，供前端展示来源证据。 */
    private String quote;
    /** PDF 页码；文本类文件没有页码时为空。 */
    private Integer page;
    /** 文件内的检索片段序号，从 1 开始。 */
    private Integer chunkIndex;
    /** 当前片段与问题的相似度。 */
    private Double score;
    private String sourceType;
}
