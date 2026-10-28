package com.ldd.initialization.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.ldd.initialization.domain.UserAISettings;
import com.ldd.initialization.dto.AISettingsDTO;
import com.ldd.initialization.result.Result;
import com.ldd.initialization.service.UserAISettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

  @Autowired
  private UserAISettingsService userAISettingsService;

  private final WebClient webClient = WebClient.builder().build();

  /**
   * 获取当前用户的AI设置
   * @return 用户AI设置信息
   */
  @SaCheckLogin
  @GetMapping("/ai")
  public Result<UserAISettings> getAISettings() {
    Long userId = Long.valueOf(StpUtil.getLoginId().toString());
    UserAISettings settings = userAISettingsService.getOrInit(userId);
    return Result.success(settings);
  }

  /**
   * 更新当前用户的AI设置
   * @param request AI设置请求参数
   * @return 操作结果
   */
  @SaCheckLogin
  @PutMapping("/ai")
  public Result<Void> updateAISettings(@RequestBody AISettingsDTO request) {
    Long userId = Long.valueOf(StpUtil.getLoginId().toString());
    userAISettingsService.saveOrUpdate(userId, request);
    return Result.success(null);
  }

  /**
   * 验证AI API连接并获取可用模型列表
   * @param request 包含apiKey和apiEndpoint的请求参数
   * @return 包含模型数量、模型列表和连接信息的响应
   */
  @SaCheckLogin
  @PostMapping("/ai/validate")
  public Result<Map<String, Object>> validateAIConnection(@RequestBody Map<String, String> request) {
    String apiKey = request.get("apiKey");
    String apiEndpoint = request.get("apiEndpoint");
    if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(apiEndpoint)) {
      return Result.error("请填写 API 密钥和 API 端点");
    }
    try {
      List<String> modelIds = fetchModelIds(apiKey, apiEndpoint);
      Map<String, Object> result = new HashMap<>();
      result.put("count", modelIds.size());
      result.put("models", modelIds);
      result.put("message", modelIds.isEmpty() ? "连接成功，但未检测到模型" : "连接成功，检测到 " + modelIds.size() + " 个模型");
      return Result.success(result);
    } catch (Exception e) {
      log.warn("[AI设置] 校验 API 连接失败: {}", e.getMessage());
      return Result.error("连接失败: " + e.getMessage());
    }
  }

  /**
   * 用用户自己的 chatApiKey + chatApiEndpoint 查询可用模型列表
   */
  /**
   * 获取当前用户的可用模型列表
   * 使用用户已配置的API密钥和端点查询模型列表
   * @return 模型ID列表
   */
  @SaCheckLogin
  @GetMapping("/ai/models")
  public Result<List<String>> getAvailableModels() {
    Long userId = Long.valueOf(StpUtil.getLoginId().toString());
    UserAISettings settings = userAISettingsService.getOrInit(userId);
    if (!StringUtils.hasText(settings.getChatApiKey()) || !StringUtils.hasText(settings.getChatApiEndpoint())) {
      return Result.error("请先填写 API 密钥和 API 端点");
    }
    try {
      List<String> modelIds = fetchModelIds(settings.getChatApiKey(), settings.getChatApiEndpoint());
      log.info("[AI设置] 用户 {} 获取到 {} 个模型", userId, modelIds.size());
      return Result.success(modelIds);
    } catch (Exception e) {
      log.warn("[AI设置] 用户 {} 查询模型列表失败: {}", userId, e.getMessage());
      return Result.error("查询模型列表失败: " + e.getMessage());
    }
  }

  /**
   * 从AI API获取模型ID列表
   * @param apiKey API密钥
   * @param apiEndpoint API端点
   * @return 排序后的模型ID列表
   * @throws IllegalStateException 当API返回格式异常时抛出
   */
  private List<String> fetchModelIds(String apiKey, String apiEndpoint) {
    String modelsUrl = buildModelsUrl(apiEndpoint);
    log.info("[AI设置] 查询模型列表: {}", modelsUrl);
    Map<?, ?> response = webClient.get()
        .uri(modelsUrl)
        .header("Authorization", "Bearer " + apiKey)
        .header("Content-Type", "application/json")
        .retrieve()
        .bodyToMono(Map.class)
        .block();
    if (response == null || !(response.get("data") instanceof List)) {
      throw new IllegalStateException("模型列表接口返回格式异常");
    }
    List<?> data = (List<?>) response.get("data");
    return data.stream()
        .filter(item -> item instanceof Map)
        .map(item -> {
          Object id = ((Map<?, ?>) item).get("id");
          return id != null ? id.toString() : null;
        })
        .filter(id -> id != null && !id.isEmpty())
        .sorted()
        .collect(Collectors.toList());
  }

  /**
   * 根据API端点构建模型查询URL
   * 支持多种端点格式的解析，包括标准OpenAI格式和自定义格式
   * @param endpoint API端点
   * @return 模型查询URL
   */
  private String buildModelsUrl(String endpoint) {
    try {
      java.net.URI u = new java.net.URI(endpoint);
      String path = u.getPath();
      int vIdx = path.indexOf("/v");
      if (vIdx >= 0) {
        int slashAfterV = path.indexOf("/", vIdx + 1);
        String vPart = slashAfterV > 0 ? path.substring(0, slashAfterV) : path;
        return u.getScheme() + "://" + u.getAuthority() + vPart + "/models";
      }
      return u.getScheme() + "://" + u.getAuthority() + "/v1/models";
    } catch (Exception e) {
      if (endpoint.contains("/v1/")) {
        return endpoint.split("/v1/")[0] + "/v1/models";
      }
      return endpoint.replaceAll("/chat/completions$", "/models");
    }
  }
}

