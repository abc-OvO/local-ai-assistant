package com.example.localai.service.impl;

import com.example.localai.exception.BusinessException;
import com.example.localai.model.DocumentChunk;
import com.example.localai.service.RetrievalService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class RetrievalServiceImpl implements RetrievalService {

    private final ConcurrentMap<String, List<DocumentChunk>> documentChunkStore = new ConcurrentHashMap<>();

    @Override
    public void saveDocumentChunks(String documentId, List<DocumentChunk> chunks) {
        documentChunkStore.put(documentId, List.copyOf(chunks));
    }

    @Override
    public List<DocumentChunk> retrieve(String documentId, List<Double> queryEmbedding, int topK) {
        List<DocumentChunk> chunks = documentChunkStore.get(documentId);
        if (chunks == null || chunks.isEmpty()) {
            throw new BusinessException(404, "文档尚未生成可检索文本块，documentId：" + documentId);
        }

        return chunks.stream()
                .map(chunk -> copyWithScore(chunk, cosineSimilarity(queryEmbedding, chunk.getEmbedding())))
                .sorted(Comparator.comparing(DocumentChunk::getScore).reversed())
                .limit(topK)
                .toList();
    }

    private DocumentChunk copyWithScore(DocumentChunk source, double score) {
        return new DocumentChunk(
                source.getChunkId(),
                source.getDocumentId(),
                source.getChunkIndex(),
                source.getContent(),
                new ArrayList<>(source.getEmbedding()),
                score
        );
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
