package com.ldd.initialization.service.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ldd.initialization.config.ai.PgVectorConnectionPool;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TeamVectorService {

    private static final int KNOWLEDGE_BASE_TYPE_TEAM = 3;

    @Autowired
    private PgVectorConnectionPool connectionPool;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private ObjectMapper objectMapper;

    @Data
    public static class TeamDocChunkMatch {
        private String embeddingId;
        private double similarity;
        private Long docId;
        private String title;
        private String text;
        private Map<String, Object> metadata;
    }

    public void addDocumentsToTeam(Long teamId, Long docId, String title, List<Document> documents) {
        if (teamId == null || docId == null) {
            throw new IllegalArgumentException("teamId/docId不能为空");
        }
        if (documents == null || documents.isEmpty()) {
            return;
        }

        try (Connection connection = connectionPool.getConnection()) {
            String sql = "INSERT INTO documents (text, metadata, embedding, file_id, knowledge_base_id, knowledge_base_type) VALUES (?, ?::jsonb, CAST(? AS vector), ?, ?, ?)";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                int chunkIndex = 0;
                for (Document doc : documents) {
                    String text = doc.text();
                    if (text == null || text.trim().isEmpty()) {
                        continue;
                    }

                    Embedding embedding = embeddingModel.embed(text).content();

                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("knowledgeBaseId", teamId);
                    metadata.put("knowledgeBaseType", KNOWLEDGE_BASE_TYPE_TEAM);
                    metadata.put("teamId", teamId);
                    metadata.put("docId", docId);
                    metadata.put("title", title);
                    metadata.put("chunkIndex", chunkIndex++);
                    metadata.put("contentLength", text.length());
                    metadata.put("indexTime", System.currentTimeMillis());

                    List<Float> vectorList = embedding.vectorAsList();
                    String vectorString = "[" + vectorList.stream()
                            .map(String::valueOf)
                            .reduce((a, b) -> a + "," + b)
                            .orElse("") + "]";

                    stmt.setString(1, text);
                    stmt.setString(2, objectMapper.writeValueAsString(metadata));
                    stmt.setString(3, vectorString);
                    stmt.setNull(4, java.sql.Types.BIGINT);
                    stmt.setLong(5, teamId);
                    stmt.setInt(6, KNOWLEDGE_BASE_TYPE_TEAM);
                    stmt.addBatch();
                }

                stmt.executeBatch();
            }
        } catch (SQLException e) {
            log.error("为团队 {} 文档 {} 写入向量失败: {}", teamId, docId, e.getMessage(), e);
            throw new RuntimeException("写入团队知识库向量失败", e);
        } catch (Exception e) {
            log.error("为团队 {} 文档 {} 写入向量失败: {}", teamId, docId, e.getMessage(), e);
            throw new RuntimeException("写入团队知识库向量失败", e);
        }
    }

    public int deleteVectorsByTeamDocId(Long teamId, Long docId) {
        if (teamId == null || docId == null) {
            return 0;
        }

        try (Connection connection = connectionPool.getConnection()) {
            String sql = "DELETE FROM documents WHERE knowledge_base_id = ? AND knowledge_base_type = ? AND metadata->>'docId' = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, teamId);
                stmt.setInt(2, KNOWLEDGE_BASE_TYPE_TEAM);
                stmt.setString(3, String.valueOf(docId));
                return stmt.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("删除团队 {} 文档 {} 向量失败: {}", teamId, docId, e.getMessage(), e);
            throw new RuntimeException("删除团队知识库向量失败", e);
        }
    }

    public List<TeamDocChunkMatch> searchInTeam(Long teamId, String query, int maxResults, double minScore) {
        if (teamId == null) {
            return List.of();
        }
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        Embedding queryEmbedding = embeddingModel.embed(query).content();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();

        return searchInTeam(teamId, request);
    }

    public List<TeamDocChunkMatch> searchInTeam(Long teamId, EmbeddingSearchRequest request) {
        List<TeamDocChunkMatch> results = new ArrayList<>();
        if (teamId == null) {
            return results;
        }

        try (Connection connection = connectionPool.getConnection()) {
            String sql = """
                    SELECT embedding_id, text, metadata, embedding <=> CAST(? AS vector) as distance
                    FROM documents
                    WHERE knowledge_base_id = ? AND knowledge_base_type = ?
                    ORDER BY embedding <=> CAST(? AS vector)
                    LIMIT ?
                    """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                List<Float> vectorList = request.queryEmbedding().vectorAsList();
                String vectorString = "[" + vectorList.stream()
                        .map(String::valueOf)
                        .reduce((a, b) -> a + "," + b)
                        .orElse("") + "]";

                stmt.setString(1, vectorString);
                stmt.setLong(2, teamId);
                stmt.setInt(3, KNOWLEDGE_BASE_TYPE_TEAM);
                stmt.setString(4, vectorString);
                stmt.setInt(5, request.maxResults());

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        double distance = rs.getDouble("distance");
                        double similarity = 1.0 - distance;
                        if (similarity < request.minScore()) {
                            continue;
                        }

                        TeamDocChunkMatch match = new TeamDocChunkMatch();
                        match.setEmbeddingId(rs.getString("embedding_id"));
                        match.setSimilarity(similarity);
                        match.setText(rs.getString("text"));

                        String metadataJson = rs.getString("metadata");
                        if (metadataJson != null && !metadataJson.isBlank()) {
                            Map<String, Object> map = objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {
                            });
                            match.setMetadata(map);

                            Object docIdObj = map.get("docId");
                            if (docIdObj instanceof Number) {
                                match.setDocId(((Number) docIdObj).longValue());
                            } else if (docIdObj != null) {
                                try {
                                    match.setDocId(Long.parseLong(String.valueOf(docIdObj)));
                                } catch (NumberFormatException ignored) {
                                }
                            }

                            Object titleObj = map.get("title");
                            if (titleObj != null) {
                                match.setTitle(String.valueOf(titleObj));
                            }
                        }

                        results.add(match);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("团队 {} 检索失败: {}", teamId, e.getMessage(), e);
            throw new RuntimeException("团队知识库检索失败", e);
        } catch (Exception e) {
            log.error("团队 {} 检索失败: {}", teamId, e.getMessage(), e);
            throw new RuntimeException("团队知识库检索失败", e);
        }

        return results;
    }
}
