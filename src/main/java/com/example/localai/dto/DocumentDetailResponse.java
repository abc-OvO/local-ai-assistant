package com.example.localai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDetailResponse {

    private String documentId;

    private String fileName;

    private String fileType;

    private String contentPreview;

    private Integer chunkCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<ChunkBrief> chunks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChunkBrief {

        private String chunkId;

        private Integer chunkIndex;

        private String contentPreview;

        private Integer embeddingDimension;
    }
}
