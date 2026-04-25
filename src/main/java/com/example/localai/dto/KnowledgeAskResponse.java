package com.example.localai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeAskResponse {

    private String documentId;

    private String fileName;

    private String model;

    private String reply;

    private List<RetrievedChunkInfo> retrievedChunks;
}
