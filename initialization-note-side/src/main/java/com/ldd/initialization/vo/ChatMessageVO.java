package com.ldd.initialization.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChatMessageVO {
    private Long id;
    private String role;
    private String content;
    private List<ChatCitationVO> citations;
    private LocalDateTime timestamp;
}
