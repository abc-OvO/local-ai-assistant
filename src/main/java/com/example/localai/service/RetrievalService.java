package com.example.localai.service;

import com.example.localai.model.DocumentChunk;

import java.util.List;

public interface RetrievalService {

    void saveDocumentChunks(String documentId, List<DocumentChunk> chunks);

    List<DocumentChunk> retrieve(String documentId, String question, List<Double> queryEmbedding, int topK);

    List<DocumentChunk> retrieveGlobal(String question, List<Double> queryEmbedding, int topK);
}
