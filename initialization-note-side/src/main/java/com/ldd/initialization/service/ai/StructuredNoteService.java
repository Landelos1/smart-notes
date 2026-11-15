package com.ldd.initialization.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ldd.initialization.service.ai.adapter.ConsultantService;
import com.ldd.initialization.vo.StructuredNoteVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 负责将 PDF 或图片资料提取为可保存、可继续编辑的 Markdown 笔记。
 */
@Service
@Slf4j
public class StructuredNoteService {
    private final ConsultantService consultantService;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    @Value("${ai.vision.base-url:${langchain4j.open-ai.embedding-model.base-url:${langchain4j.open-ai.chat-model.base-url:}}}")
    private String visionBaseUrl;
    @Value("${ai.vision.api-key:${langchain4j.open-ai.embedding-model.api-key:${langchain4j.open-ai.chat-model.api-key:}}}")
    private String visionApiKey;
    @Value("${ai.vision.model:qwen3-vl-plus}")
    private String visionModel;

    public StructuredNoteService(ConsultantService consultantService, ObjectMapper objectMapper) {
        this.consultantService = consultantService;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder().build();
    }

    public StructuredNoteVO structure(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("资料文件不能为空");
        }
        String name = file.getOriginalFilename() == null ? "资料" : file.getOriginalFilename();
        String extension = extension(name);
        if ("pdf".equals(extension)) {
            return structurePdf(name, file.getBytes());
        }
        if (List.of("png", "jpg", "jpeg", "webp", "bmp").contains(extension)) {
            return structureImage(name, file.getBytes(), file.getContentType());
        }
        throw new IllegalArgumentException("结构化整理目前支持 PDF、PNG、JPG、JPEG、WEBP 和 BMP");
    }

    private StructuredNoteVO structurePdf(String fileName, byte[] bytes) throws IOException {
        StringBuilder pages = new StringBuilder();
        int pageCount;
        try (PDDocument document = PDDocument.load(bytes)) {
            pageCount = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document);
                if (StringUtils.hasText(text)) {
                    pages.append("\n[第 ").append(page).append(" 页]\n").append(text.trim());
                }
            }
        }
        if (!StringUtils.hasText(pages)) {
            throw new IllegalArgumentException("PDF 未提取到文本，扫描版 PDF 请导出为图片后使用图片整理");
        }
        StructuredNoteVO note = generateFromText(fileName, pages.toString(), "PDF文本解析");
        note.setPageCount(pageCount);
        return note;
    }

    private StructuredNoteVO structureImage(String fileName, byte[] bytes, String contentType) {
        if (!StringUtils.hasText(visionBaseUrl) || !StringUtils.hasText(visionApiKey)
                || visionApiKey.contains("YOUR_") || visionApiKey.contains("CHANGE_ME")) {
            throw new IllegalStateException("图片整理需要配置 ai.vision.base-url、ai.vision.api-key 和 ai.vision.model");
        }
        String mime = StringUtils.hasText(contentType) ? contentType : "image/" + extension(fileName);
        String dataUrl = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        Map<String, Object> body = Map.of(
                "model", visionModel,
                "temperature", 0.2,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "text", "text", structurePrompt()),
                                Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))
                        )
                ))
        );
        String response = webClient.post()
                .uri(normalizeCompletionsUrl(visionBaseUrl))
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBearerAuth(visionApiKey))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        try {
            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            if (!StringUtils.hasText(content)) {
                throw new IllegalStateException("视觉模型没有返回可整理的内容");
            }
            StructuredNoteVO note = parseModelJson(content, fileName, "视觉模型解析");
            note.setPageCount(1);
            return note;
        } catch (IOException e) {
            throw new IllegalStateException("视觉模型返回格式无法解析", e);
        }
    }

    private StructuredNoteVO generateFromText(String fileName, String text, String mode) {
        String source = text.length() > 30000 ? text.substring(0, 30000) : text;
        String response = consultantService.simpleChat("structure-" + UUID.randomUUID(),
                structurePrompt() + "\n\n资料文件名：" + fileName + "\n资料正文：\n" + source);
        return parseModelJson(response, fileName, mode);
    }

    private StructuredNoteVO parseModelJson(String response, String fileName, String mode) {
        try {
            String json = response == null ? "" : response.trim();
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }
            JsonNode node = objectMapper.readTree(json);
            StructuredNoteVO note = new StructuredNoteVO();
            note.setSourceFileName(fileName);
            note.setTitle(text(node, "title", fileName));
            note.setSummary(text(node, "summary", ""));
            note.setKeyPoints(strings(node, "keyPoints"));
            note.setTags(strings(node, "tags"));
            note.setActionItems(strings(node, "actionItems"));
            note.setExtractionMode(mode);
            note.setMarkdown(buildMarkdown(note));
            return note;
        } catch (Exception e) {
            log.warn("结构化模型返回非标准 JSON，将正文作为摘要: {}", e.getMessage());
            StructuredNoteVO fallback = new StructuredNoteVO();
            fallback.setSourceFileName(fileName);
            fallback.setTitle(fileName);
            fallback.setSummary(response == null ? "" : response.trim());
            fallback.setExtractionMode(mode);
            fallback.setMarkdown("# " + fileName + "\n\n" + fallback.getSummary());
            return fallback;
        }
    }

    private String structurePrompt() {
        return "你是资料整理助手。请从输入的 PDF 或图片资料中提取信息，并且只返回 JSON，不要 Markdown 代码围栏。字段必须为：" +
                "title(标题字符串)、summary(150字以内摘要字符串)、keyPoints(3到8条关键要点数组)、" +
                "tags(3到6个标签数组)、actionItems(待办事项数组，没有则为空数组)。内容要忠实于原文，不要编造。";
    }

    private String buildMarkdown(StructuredNoteVO note) {
        StringBuilder markdown = new StringBuilder("# ").append(note.getTitle()).append("\n\n")
                .append("## 摘要\n\n").append(note.getSummary()).append("\n\n## 关键要点\n");
        note.getKeyPoints().forEach(item -> markdown.append("- ").append(item).append("\n"));
        markdown.append("\n## 标签\n\n").append(String.join("、", note.getTags())).append("\n\n## 待办事项\n");
        if (note.getActionItems().isEmpty()) markdown.append("- 暂无\n");
        note.getActionItems().forEach(item -> markdown.append("- [ ] ").append(item).append("\n"));
        return markdown.toString();
    }

    private List<String> strings(JsonNode node, String field) {
        List<String> values = new ArrayList<>();
        JsonNode array = node.path(field);
        if (array.isArray()) array.forEach(item -> { if (item.isTextual() && StringUtils.hasText(item.asText())) values.add(item.asText()); });
        return values;
    }

    private String text(JsonNode node, String field, String fallback) {
        return node.path(field).isTextual() && StringUtils.hasText(node.path(field).asText()) ? node.path(field).asText() : fallback;
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }

    private String normalizeCompletionsUrl(String baseUrl) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return base.endsWith("/chat/completions") ? base : base + "/chat/completions";
    }
}



