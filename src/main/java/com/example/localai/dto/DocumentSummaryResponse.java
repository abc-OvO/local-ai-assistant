package com.example.localai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSummaryResponse {

    private String documentId;

    private String fileName;

    private String fileType;

    private Integer contentLength;

    private LocalDateTime uploadTime;
}
