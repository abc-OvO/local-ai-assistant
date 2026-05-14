package com.example.localai.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class KnowledgeAskResponse {

    private String documentId;

    private String fileName;

    private String model;

    private String reply;

    private List<RetrievedChunkInfo> retrievedChunks;

    private String sessionId;

    private Boolean memoryEnabled;

    private Integer historyTurns;

    public KnowledgeAskResponse(String documentId, String fileName, String model, String reply, List<RetrievedChunkInfo> retrievedChunks) {
        this.documentId = documentId;
        this.fileName = fileName;
        this.model = model;
        this.reply = reply;
        this.retrievedChunks = retrievedChunks;
    }

    public KnowledgeAskResponse(
            String documentId,
            String fileName,
            String model,
            String reply,
            List<RetrievedChunkInfo> retrievedChunks,
            String sessionId,
            Boolean memoryEnabled,
            Integer historyTurns
    ) {
        this.documentId = documentId;
        this.fileName = fileName;
        this.model = model;
        this.reply = reply;
        this.retrievedChunks = retrievedChunks;
        this.sessionId = sessionId;
        this.memoryEnabled = memoryEnabled;
        this.historyTurns = historyTurns;
    }
}
