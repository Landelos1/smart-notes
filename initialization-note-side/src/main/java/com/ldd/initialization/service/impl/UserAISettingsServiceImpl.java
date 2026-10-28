package com.ldd.initialization.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ldd.initialization.config.exception.BizException;
import com.ldd.initialization.domain.UserAISettings;
import com.ldd.initialization.dto.AISettingsDTO;
import com.ldd.initialization.mapper.UserAISettingsMapper;
import com.ldd.initialization.service.UserAISettingsService;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class UserAISettingsServiceImpl extends ServiceImpl<UserAISettingsMapper, UserAISettings>
    implements UserAISettingsService {

  @Override
  public UserAISettings getOrInit(Long userId) {
    log.debug("[AI设置] 获取用户AI设置, 用户ID: {}", userId);
    UserAISettings existing = getLatestByUserId(userId);
    if (existing != null) {
      log.debug("[AI设置] 返回已有设置, 用户ID: {}", userId);
      return existing;
    }
    log.info("[AI设置] 初始化默认设置, 用户ID: {}", userId);
    UserAISettings init = buildDefault(userId);
    save(init);
    return init;
  }

  @Override
  public void saveOrUpdate(Long userId, AISettingsDTO dto) {
    log.info("[AI设置] 更新用户AI设置, 用户ID: {}", userId);
    validate(dto);
    UserAISettings entity = getLatestByUserId(userId);
    Date now = new Date();
    if (entity == null) {
      log.debug("[AI设置] 创建新设置, 用户ID: {}", userId);
      entity = new UserAISettings();
      entity.setUserId(userId);
      entity.setCreateTime(now);
    }
    entity.setEnablePersonalKb(Boolean.TRUE.equals(dto.getEnablePersonalKb()));
    entity.setPersonalKbMaxDocs(dto.getPersonalKbMaxDocs());
    entity.setChatStyle(dto.getChatStyle());
    entity.setReplyLanguage(dto.getReplyLanguage());
    entity.setAssistantName(StringUtils.hasText(dto.getAssistantName()) ? dto.getAssistantName() : null);
    entity.setChatApiKey(StringUtils.hasText(dto.getChatApiKey()) ? dto.getChatApiKey() : null);
    entity.setChatApiEndpoint(StringUtils.hasText(dto.getChatApiEndpoint()) ? dto.getChatApiEndpoint() : null);
    entity.setEnableSharedKb(Boolean.TRUE.equals(dto.getEnableSharedKb()));
    entity.setSharedKbMaxDocs(dto.getSharedKbMaxDocs());
    entity.setCustomSystemPrompt(StringUtils.hasText(dto.getCustomSystemPrompt()) ? dto.getCustomSystemPrompt() : null);
    entity.setKbApiKey(StringUtils.hasText(dto.getKbApiKey()) ? dto.getKbApiKey() : null);
    entity.setKbApiEndpoint(StringUtils.hasText(dto.getKbApiEndpoint()) ? dto.getKbApiEndpoint() : null);
    entity.setUseCustomKey(Boolean.TRUE.equals(dto.getUseCustomKey()));
    entity.setChatModel(StringUtils.hasText(dto.getChatModel()) ? dto.getChatModel() : null);
    entity.setKbUseCustomKey(Boolean.TRUE.equals(dto.getKbUseCustomKey()));
    entity.setKbChatModel(StringUtils.hasText(dto.getKbChatModel()) ? dto.getKbChatModel() : null);
    entity.setUpdateTime(now);
    if (entity.getId() == null) {
      save(entity);
    } else {
      updateById(entity);
    }
    log.info("[AI设置] AI设置更新成功, 用户ID: {}", userId);
  }

  private UserAISettings getLatestByUserId(Long userId) {
    List<UserAISettings> list = list(new QueryWrapper<UserAISettings>()
        .eq("user_id", userId)
        .orderByDesc("update_time")
        .orderByDesc("id"));
    if (list == null || list.isEmpty()) {
      return null;
    }
    if (list.size() > 1) {
      log.warn("[AI设置] 用户 {} 存在 {} 条 AI 设置记录，自动使用最新一条，建议后续清理重复数据", userId, list.size());
    }
    return list.get(0);
  }

  private UserAISettings buildDefault(Long userId) {
    Date now = new Date();
    UserAISettings s = new UserAISettings();
    s.setUserId(userId);
    s.setEnablePersonalKb(true);
    s.setPersonalKbMaxDocs(5);
    s.setChatStyle(2);
    s.setReplyLanguage(1);
    s.setAssistantName("小智助手");
    s.setEnableSharedKb(true);
    s.setSharedKbMaxDocs(5);
    s.setUseCustomKey(false);
    s.setKbUseCustomKey(false);
    s.setCreateTime(now);
    s.setUpdateTime(now);
    return s;
  }

  private void validate(AISettingsDTO dto) {
    if (dto.getPersonalKbMaxDocs() == null || dto.getPersonalKbMaxDocs() < 1 || dto.getPersonalKbMaxDocs() > 50) {
      log.warn("[AI设置] 参数校验失败: 个人知识库参考文档数无效");
      throw new BizException("个人知识库参考文档数需在1-50之间");
    }
    if (dto.getSharedKbMaxDocs() == null || dto.getSharedKbMaxDocs() < 1 || dto.getSharedKbMaxDocs() > 50) {
      log.warn("[AI设置] 参数校验失败: 共享知识库参考文档数无效");
      throw new BizException("共享知识库参考文档数需在1-50之间");
    }
    if (dto.getChatStyle() == null || (dto.getChatStyle() < 1 || dto.getChatStyle() > 3)) {
      log.warn("[AI设置] 参数校验失败: 回答风格无效");
      throw new BizException("回答风格非法");
    }
    if (dto.getReplyLanguage() == null || (dto.getReplyLanguage() < 1 || dto.getReplyLanguage() > 3)) {
      log.warn("[AI设置] 参数校验失败: 默认回答语言无效");
      throw new BizException("默认回答语言非法");
    }
    if (StringUtils.hasText(dto.getChatApiEndpoint()) && dto.getChatApiEndpoint().length() > 255) {
      log.warn("[AI设置] 参数校验失败: 对话API端点过长");
      throw new BizException("对话API端点过长");
    }
    if (StringUtils.hasText(dto.getKbApiEndpoint()) && dto.getKbApiEndpoint().length() > 255) {
      log.warn("[AI设置] 参数校验失败: 知识库API端点过长");
      throw new BizException("知识库API端点过长");
    }
  }
}

