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
}
