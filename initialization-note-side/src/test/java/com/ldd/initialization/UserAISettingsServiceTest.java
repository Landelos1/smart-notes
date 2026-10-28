package com.ldd.initialization;

import com.ldd.initialization.config.exception.BizException;
import com.ldd.initialization.domain.UserAISettings;
import com.ldd.initialization.dto.AISettingsDTO;
import com.ldd.initialization.service.UserAISettingsService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * AI设置服务测试
 */
@SpringBootTest
public class UserAISettingsServiceTest {

  @Autowired
  private UserAISettingsService userAISettingsService;

  @Test
  @DisplayName("测试创建和更新AI设置")
  void testCreateAndUpdateSettings() {
    Long userId = 1L;
    AISettingsDTO dto = new AISettingsDTO();
    dto.setEnablePersonalKb(true);
    dto.setPersonalKbMaxDocs(5);
    dto.setChatStyle(2);
    dto.setReplyLanguage(1);
    dto.setAssistantName("助理A");
    dto.setEnableSharedKb(true);
    dto.setSharedKbMaxDocs(5);
    userAISettingsService.saveOrUpdate(userId, dto);
    var s = userAISettingsService.getOrInit(userId);
    Assertions.assertEquals("助理A", s.getAssistantName());
    Assertions.assertEquals(5, s.getPersonalKbMaxDocs());
  }

  @Test
  @DisplayName("测试参数范围校验 - 个人知识库文档数下限")
  void testValidationRange() {
    Long userId = 1L;
    AISettingsDTO dto = new AISettingsDTO();
    dto.setEnablePersonalKb(true);
    dto.setPersonalKbMaxDocs(0); // 无效值
    dto.setChatStyle(2);
    dto.setReplyLanguage(1);
    dto.setEnableSharedKb(true);
    dto.setSharedKbMaxDocs(5);
    Assertions.assertThrows(BizException.class, () -> userAISettingsService.saveOrUpdate(userId, dto));
  }

  @Test
  @DisplayName("测试参数范围校验 - 个人知识库文档数上限")
  void testValidationRangeUpperBound() {
    Long userId = 1L;
    AISettingsDTO dto = new AISettingsDTO();
    dto.setEnablePersonalKb(true);
    dto.setPersonalKbMaxDocs(51); // 超过上限
    dto.setChatStyle(2);
    dto.setReplyLanguage(1);
    dto.setEnableSharedKb(true);
    dto.setSharedKbMaxDocs(5);
    Assertions.assertThrows(BizException.class, () -> userAISettingsService.saveOrUpdate(userId, dto));
  }

  @Test
  @DisplayName("测试参数校验 - 回答风格无效")
  void testInvalidChatStyle() {
    Long userId = 1L;
    AISettingsDTO dto = new AISettingsDTO();
    dto.setEnablePersonalKb(true);
    dto.setPersonalKbMaxDocs(5);
    dto.setChatStyle(4); // 无效值
    dto.setReplyLanguage(1);
    dto.setEnableSharedKb(true);
    dto.setSharedKbMaxDocs(5);
    Assertions.assertThrows(BizException.class, () -> userAISettingsService.saveOrUpdate(userId, dto));
  }

  @Test
  @DisplayName("测试参数校验 - 回复语言无效")
  void testInvalidReplyLanguage() {
    Long userId = 1L;
    AISettingsDTO dto = new AISettingsDTO();
    dto.setEnablePersonalKb(true);
    dto.setPersonalKbMaxDocs(5);
    dto.setChatStyle(2);
    dto.setReplyLanguage(0); // 无效值
    dto.setEnableSharedKb(true);
    dto.setSharedKbMaxDocs(5);
    Assertions.assertThrows(BizException.class, () -> userAISettingsService.saveOrUpdate(userId, dto));
  }

  @Test
  @DisplayName("测试获取或初始化设置")
  void testGetOrInit() {
    Long userId = 999L; // 使用一个新的userId
    UserAISettings settings = userAISettingsService.getOrInit(userId);
    Assertions.assertNotNull(settings);
    Assertions.assertEquals(userId, settings.getUserId());
    // 默认值验证
    Assertions.assertTrue(settings.getEnablePersonalKb());
    Assertions.assertEquals(5, settings.getPersonalKbMaxDocs());
    Assertions.assertEquals(2, settings.getChatStyle());
    Assertions.assertEquals(1, settings.getReplyLanguage());
    Assertions.assertEquals("小智助手", settings.getAssistantName());
  }

  @Test
  @DisplayName("测试更新所有字段")
  void testUpdateAllFields() {
    Long userId = 2L;
    AISettingsDTO dto = new AISettingsDTO();
    dto.setEnablePersonalKb(false);
    dto.setPersonalKbMaxDocs(10);
    dto.setChatStyle(3);
    dto.setReplyLanguage(2);
    dto.setAssistantName("测试助手");
    dto.setChatApiKey("test-api-key");
    dto.setChatApiEndpoint("https://test.api.com/chat");
    dto.setEnableSharedKb(false);
    dto.setSharedKbMaxDocs(20);
    dto.setCustomSystemPrompt("自定义提示词");
    dto.setKbApiKey("test-kb-key");
    dto.setKbApiEndpoint("https://test.api.com/kb");

    userAISettingsService.saveOrUpdate(userId, dto);
    UserAISettings settings = userAISettingsService.getOrInit(userId);

    Assertions.assertFalse(settings.getEnablePersonalKb());
    Assertions.assertEquals(10, settings.getPersonalKbMaxDocs());
    Assertions.assertEquals(3, settings.getChatStyle());
    Assertions.assertEquals(2, settings.getReplyLanguage());
    Assertions.assertEquals("测试助手", settings.getAssistantName());
    Assertions.assertEquals("test-api-key", settings.getChatApiKey());
    Assertions.assertFalse(settings.getEnableSharedKb());
    Assertions.assertEquals(20, settings.getSharedKbMaxDocs());
    Assertions.assertEquals("自定义提示词", settings.getCustomSystemPrompt());
  }
}

