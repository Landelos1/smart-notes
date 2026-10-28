package com.ldd.initialization.service;

import com.ldd.initialization.domain.UserAISettings;
import com.ldd.initialization.dto.AISettingsDTO;

public interface UserAISettingsService {
  UserAISettings getOrInit(Long userId);
  void saveOrUpdate(Long userId, AISettingsDTO dto);
}

