package com.ldd.initialization.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ldd.initialization.config.handler.JsonbTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName(value = "t_chat_message")
@Data
public class ChatMessage implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "session_id")
    private Long sessionId;

    private Integer role;

    private String content;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private String citations;

    @TableField(value = "total_tokens")
    private Integer totalTokens;

    @TableField(value = "timestamp")
    private LocalDateTime timestamp;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
