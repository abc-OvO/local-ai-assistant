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

    private Integer chunkIndex;

    private String content;

    private List<Double> embedding;

    /**
     * 检索时临时写入的相似度分数，便于返回和调试。
     */
    private Double score;
}
