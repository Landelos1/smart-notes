package com.ldd.initialization.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatSessionVO {
    private Long id;
    private String title;
    private String lastMessage;
    private LocalDateTime timestamp;
}
