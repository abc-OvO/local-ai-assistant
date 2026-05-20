package com.example.localai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRecord {

    private String documentId;

    private String fileName;

    private String fileType;

    private Integer contentLength;

    private LocalDateTime uploadTime;

    private String savedPath;

    private String content;

    private LocalDateTime updatedAt;

    public DocumentRecord(
            String documentId,
            String fileName,
            String fileType,
            Integer contentLength,
            LocalDateTime uploadTime,
            String savedPath,
            String content
    ) {
        this.documentId = documentId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.contentLength = contentLength;
        this.uploadTime = uploadTime;
        this.savedPath = savedPath;
        this.content = content;
        this.updatedAt = uploadTime;
    }
}
