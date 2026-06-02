package com.example.localai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetrievedChunkInfo {

    private String chunkId;

    private String documentId;

    private String fileName;

    private Integer chunkIndex;

    private Double score;

    private Double vectorScore;

    private Double keywordScore;

    private Double finalScore;

    private String contentPreview;

    public RetrievedChunkInfo(
            String chunkId,
            String documentId,
            String fileName,
            Integer chunkIndex,
            Double score,
            String contentPreview
    ) {
        this.chunkId = chunkId;
        this.documentId = documentId;
        this.fileName = fileName;
        this.chunkIndex = chunkIndex;
        this.score = score;
        this.vectorScore = score;
        this.keywordScore = 0.0;
        this.finalScore = score;
        this.contentPreview = contentPreview;
    }
}
