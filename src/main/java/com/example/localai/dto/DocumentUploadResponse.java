package com.example.localai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadResponse {

    private String documentId;

    private String fileName;

    private Integer contentLength;

    private LocalDateTime uploadTime;
}
