package com.ldd.initialization.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ldd.initialization.service.ai.adapter.ConsultantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuredNoteServiceTest {
    private StructuredNoteService service;

    @BeforeEach
    void setUp() {
        service = new StructuredNoteService(Mockito.mock(ConsultantService.class), new ObjectMapper());
    }

    @Test
    void imageDataUrlUsesActualImageMimeType() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);

        String dataUrl = service.imageDataUrl(output.toByteArray());

        assertTrue(dataUrl.startsWith("data:image/png;base64,"));
        assertTrue(ImageIO.read(new java.io.ByteArrayInputStream(
                Base64.getDecoder().decode(dataUrl.substring(dataUrl.indexOf(',') + 1)))) != null);
    }

    @Test
    void bitmapIsConvertedToPngForVisionModel() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "bmp", output);

        assertTrue(service.imageDataUrl(output.toByteArray()).startsWith("data:image/png;base64,"));
    }

    @Test
    void structuredNoteContainsExtractedFields() {
        var note = service.parseModelJson("""
                {"title":"Redis 缓存","summary":"缓存常见策略","keyPoints":["LRU"],"tags":["Redis"],"actionItems":[],"readable":true}
                """, "cache.png", "视觉模型解析");

        assertEquals("Redis 缓存", note.getTitle());
        assertEquals("视觉模型解析", note.getExtractionMode());
        assertTrue(note.getMarkdown().contains("LRU"));
    }

    @Test
    void emptyOrUnreadableRecognitionDoesNotCreateAnEmptyNote() {
        assertThrows(IllegalStateException.class, () -> service.parseModelJson("{}", "blank.png", "视觉模型解析"));
        assertThrows(IllegalStateException.class, () -> service.parseModelJson(
                "{\"readable\":false,\"summary\":\"\",\"keyPoints\":[]}", "blur.png", "视觉模型解析"));
    }

    @Test
    void nonJsonModelResponseIsPreservedWithWarning() {
        var note = service.parseModelJson("可编辑的原始识别结果", "scan.png", "视觉模型解析");

        assertEquals("可编辑的原始识别结果", note.getSummary());
        assertTrue(note.getWarning().contains("保留原始内容"));
        assertFalse(note.getMarkdown().isBlank());
    }
}
