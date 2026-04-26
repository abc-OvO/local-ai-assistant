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

    private String contentPreview;
}
