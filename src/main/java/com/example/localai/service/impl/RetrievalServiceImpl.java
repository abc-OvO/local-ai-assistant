package com.example.localai.service.impl;

import com.example.localai.exception.BusinessException;
import com.example.localai.model.DocumentChunk;
import com.example.localai.service.RetrievalService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RetrievalServiceImpl implements RetrievalService {

    private static final double FILE_NAME_KEYWORD_BOOST = 0.25;
    private static final double CONTENT_KEYWORD_BOOST = 0.10;
    private static final double MAX_KEYWORD_BOOST = 0.60;
    private static final Pattern ENGLISH_TOKEN_PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z0-9+#.-]*");

    private final ConcurrentMap<String, List<DocumentChunk>> documentChunkStore = new ConcurrentHashMap<>();

    @Override
    public void saveDocumentChunks(String documentId, List<DocumentChunk> chunks) {
        documentChunkStore.put(documentId, List.copyOf(chunks));
    }

    @Override
    public void deleteDocumentChunks(String documentId) {
        documentChunkStore.remove(documentId);
    }

    @Override
    public List<DocumentChunk> retrieve(String documentId, String question, List<Double> queryEmbedding, int topK) {
        List<DocumentChunk> chunks = documentChunkStore.get(documentId);
        if (chunks == null || chunks.isEmpty()) {
            throw new BusinessException(404, "文档尚未生成可检索文本块，documentId：" + documentId);
        }

        return scoreAndLimit(chunks, question, queryEmbedding, topK);
    }

    @Override
    public List<DocumentChunk> retrieveGlobal(String question, List<Double> queryEmbedding, int topK) {
        List<DocumentChunk> allChunks = documentChunkStore.values()
                .stream()
                .flatMap(List::stream)
                .toList();
        if (allChunks.isEmpty()) {
            throw new BusinessException(404, "当前没有任何可检索文档，请先上传文档");
        }

        return scoreAndLimit(allChunks, question, queryEmbedding, topK);
    }

    private List<DocumentChunk> scoreAndLimit(List<DocumentChunk> chunks, String question, List<Double> queryEmbedding, int topK) {
        List<String> keywords = extractKeywords(question);
        return chunks.stream()
                .map(chunk -> {
                    double vectorScore = cosineSimilarity(queryEmbedding, chunk.getEmbedding());
                    double keywordBoost = calculateKeywordBoost(chunk, keywords);
                    return copyWithScore(chunk, vectorScore + keywordBoost);
                })
                .sorted(Comparator.comparing(DocumentChunk::getScore).reversed())
                .limit(topK)
                .toList();
    }

    private DocumentChunk copyWithScore(DocumentChunk source, double score) {
        return new DocumentChunk(
                source.getChunkId(),
                source.getDocumentId(),
                source.getFileName(),
                source.getChunkIndex(),
                source.getContent(),
                new ArrayList<>(source.getEmbedding()),
                score
        );
    }

    private List<String> extractKeywords(String question) {
        if (!StringUtils.hasText(question)) {
            return List.of();
        }

        String normalizedQuestion = question.toLowerCase(Locale.ROOT);
        List<String> keywords = new ArrayList<>();

        Matcher matcher = ENGLISH_TOKEN_PATTERN.matcher(normalizedQuestion);
        while (matcher.find()) {
            String token = matcher.group().trim();
            if (token.length() >= 2 && !keywords.contains(token)) {
                keywords.add(token);
            }
        }

        String chineseQuestion = normalizedQuestion
                .replace("什么", " ")
                .replace("这个", " ")
                .replace("项目", " ")
                .replace("一下", " ")
                .replace("哪些", " ")
                .replace("使用了", " ")
                .replace("使用", " ")
                .replace("是", " ")
                .replace("吗", " ")
                .replace("？", " ")
                .replace("?", " ")
                .trim();

        for (String token : chineseQuestion.split("\\s+")) {
            String cleaned = token.trim();
            if (cleaned.length() >= 2 && !keywords.contains(cleaned)) {
                keywords.add(cleaned);
            }
        }

        return keywords;
    }

    private double calculateKeywordBoost(DocumentChunk chunk, List<String> keywords) {
        if (keywords.isEmpty()) {
            return 0.0;
        }

        String lowerContent = chunk.getContent() == null ? "" : chunk.getContent().toLowerCase(Locale.ROOT);
        String lowerFileName = chunk.getFileName() == null ? "" : chunk.getFileName().toLowerCase(Locale.ROOT);

        double boost = 0.0;
        for (String keyword : keywords) {
            if (lowerContent.contains(keyword)) {
                boost += CONTENT_KEYWORD_BOOST;
            }
            if (lowerFileName.contains(keyword)) {
                boost += FILE_NAME_KEYWORD_BOOST;
            }
        }
        return Math.min(boost, MAX_KEYWORD_BOOST);
    }

    private double cosineSimilarity(List<Double> left, List<Double> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }

        int dimension = Math.min(left.size(), right.size());
        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;

        for (int i = 0; i < dimension; i++) {
            double leftValue = left.get(i);
            double rightValue = right.get(i);
            dot += leftValue * rightValue;
            leftNorm += leftValue * leftValue;
            rightNorm += rightValue * rightValue;
        }

        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
