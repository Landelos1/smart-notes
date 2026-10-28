package com.ldd.initialization.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

@TableName(value = "t_user_ai_settings")
@Data
public class UserAISettings implements Serializable {
  @TableId(type = IdType.AUTO)
  private Long id;

  @TableField("user_id")
  private Long userId;

  @TableField("enable_personal_kb")
  private Boolean enablePersonalKb;

  @TableField("personal_kb_max_docs")
  private Integer personalKbMaxDocs;

  @TableField("chat_style")
  private Integer chatStyle;

  @TableField("reply_language")
  private Integer replyLanguage;

  @TableField("assistant_name")
  private String assistantName;

  @TableField("chat_api_key")
  private String chatApiKey;

  @TableField("chat_api_endpoint")
  private String chatApiEndpoint;

  @TableField("enable_shared_kb")
  private Boolean enableSharedKb;

  @TableField("shared_kb_max_docs")
  private Integer sharedKbMaxDocs;

  @TableField("custom_system_prompt")
  private String customSystemPrompt;

  @TableField("kb_api_key")
  private String kbApiKey;

  @TableField("kb_api_endpoint")
  private String kbApiEndpoint;

  @TableField("use_custom_key")
  private Boolean useCustomKey;

  @TableField("chat_model")
  private String chatModel;

  @TableField("kb_use_custom_key")
  private Boolean kbUseCustomKey;

  @TableField("kb_chat_model")
  private String kbChatModel;

  @TableField("create_time")
  private Date createTime;

  @TableField("update_time")
  private Date updateTime;

  @TableField(exist = false)
  private static final long serialVersionUID = 1L;
}

