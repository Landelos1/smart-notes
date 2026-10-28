package com.ldd.initialization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发表笔记评论DTO
 */
@Data
public class NoteCommentCreateDTO {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容长度不能超过500")
    private String content;

    private Long parentId;

    private Long replyToUserId;
}
