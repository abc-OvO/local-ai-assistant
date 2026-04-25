package com.example.localai.service;

import com.example.localai.model.DocumentChunk;

import java.util.List;

public interface RetrievalService {

    void saveDocumentChunks(String documentId, List<DocumentChunk> chunks);

    List<DocumentChunk> retrieve(String documentId, List<Double> queryEmbedding, int topK);
}
