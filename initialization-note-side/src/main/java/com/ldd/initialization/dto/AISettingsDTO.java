package com.ldd.initialization.dto;

import lombok.Data;

@Data
public class AISettingsDTO {
  private Boolean enablePersonalKb;
  private Integer personalKbMaxDocs;
  private Integer chatStyle;
  private Integer replyLanguage;
  private String assistantName;
  private String chatApiKey;
  private String chatApiEndpoint;
  private Boolean enableSharedKb;
  private Integer sharedKbMaxDocs;
  private String customSystemPrompt;
  private String kbApiKey;
  private String kbApiEndpoint;
  private Boolean useCustomKey;
  private String chatModel;
  private Boolean kbUseCustomKey;
  private String kbChatModel;
}

