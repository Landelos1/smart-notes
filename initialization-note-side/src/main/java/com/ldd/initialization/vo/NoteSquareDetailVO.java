package com.ldd.initialization.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Note square detail view object.
 */
@Data
public class NoteSquareDetailVO {

    private Long id;

    private Long squareId;

    private String title;

    private String content;

    private String summary;

    private String coverUrl;

    private Long authorId;

    private String authorName;

    private String authorAvatar;

    private Integer likeCount;

    private Integer favoriteCount;

    private Integer commentCount;

    private Integer viewCount;

    private Boolean isLiked;

    private Boolean isFavorited;

    private Integer squareStatus;

    private LocalDateTime publishTime;

    private LocalDateTime updateTime;

    private LocalDateTime createTime;

    private List<String> tags;
}
