package com.example.localai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class DocumentSummaryResponse {

    private String documentId;

    private String fileName;

    private String fileType;

    private Integer contentLength;

    private LocalDateTime uploadTime;

    private Integer chunkCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public DocumentSummaryResponse(
            String documentId,
            String fileName,
            String fileType,
            Integer contentLength,
            LocalDateTime uploadTime
    ) {
        this(documentId, fileName, fileType, contentLength, uploadTime, null, uploadTime, uploadTime);
    }

    public DocumentSummaryResponse(
            String documentId,
            String fileName,
            String fileType,
            Integer contentLength,
            LocalDateTime uploadTime,
            Integer chunkCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.documentId = documentId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.contentLength = contentLength;
        this.uploadTime = uploadTime;
        this.chunkCount = chunkCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
