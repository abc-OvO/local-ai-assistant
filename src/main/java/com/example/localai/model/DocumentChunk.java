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
     * 检索时临时写入的相似度分数，便于返回和调试。
     */
    private Double score;

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
    }
}
