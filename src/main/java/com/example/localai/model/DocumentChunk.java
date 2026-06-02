package com.example.localai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    private String chunkId;

    private String documentId;

    private String fileName;

    private Integer chunkIndex;

    private String content;

    private List<Double> embedding;

    /**
     * 持久化时使用的 JSON 字符串，运行时检索仍使用 embedding。
     */
    private String embeddingJson;

    /**
     * 检索时临时写入的最终分数，便于兼容旧前端和旧接口字段。
     */
    private Double score;

    private Double vectorScore;

    private Double keywordScore;

    private Double finalScore;

    public DocumentChunk(
            String chunkId,
            String documentId,
            String fileName,
            Integer chunkIndex,
            String content,
            List<Double> embedding,
            Double score
    ) {
        this.chunkId = chunkId;
        this.documentId = documentId;
        this.fileName = fileName;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.embedding = embedding;
        this.score = score;
        this.vectorScore = score;
        this.keywordScore = 0.0;
        this.finalScore = score;
    }

    public DocumentChunk(
            String chunkId,
            String documentId,
            String fileName,
            Integer chunkIndex,
            String content,
            List<Double> embedding,
            Double vectorScore,
            Double keywordScore,
            Double finalScore
    ) {
        this.chunkId = chunkId;
        this.documentId = documentId;
        this.fileName = fileName;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.embedding = embedding;
        this.vectorScore = vectorScore;
        this.keywordScore = keywordScore;
        this.finalScore = finalScore;
        this.score = finalScore;
    }
}
