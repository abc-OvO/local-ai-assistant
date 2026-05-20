package com.example.localai.service;

import com.example.localai.mapper.DocumentChunkPersistenceMapper;
import com.example.localai.mapper.DocumentPersistenceMapper;
import com.example.localai.model.DocumentChunk;
import com.example.localai.model.DocumentRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class VectorIndexBootstrap implements ApplicationRunner {

    private final DocumentPersistenceMapper documentPersistenceMapper;

    private final DocumentChunkPersistenceMapper documentChunkPersistenceMapper;

    private final DocumentService documentService;

    private final RetrievalService retrievalService;

    private final EmbeddingJsonCodec embeddingJsonCodec;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<DocumentRecord> documents = documentPersistenceMapper.findAll();
            List<DocumentChunk> chunks = documentChunkPersistenceMapper.findAll();
            if (chunks.isEmpty()) {
                documentService.restorePersistedDocuments(documents);
                System.out.println("[VectorIndexBootstrap] no persisted chunks found, loadedDocuments="
                        + documents.size()
                        + ", loadedChunks=0");
                return;
            }

            for (DocumentChunk chunk : chunks) {
                chunk.setEmbedding(embeddingJsonCodec.fromJson(chunk.getEmbeddingJson()));
            }

            Map<String, List<DocumentChunk>> chunksByDocumentId = chunks.stream()
                    .collect(Collectors.groupingBy(DocumentChunk::getDocumentId));
            chunksByDocumentId.forEach(retrievalService::saveDocumentChunks);
            documentService.restorePersistedDocuments(documents);

            System.out.println("[VectorIndexBootstrap] loadedDocuments=" + documents.size()
                    + ", loadedChunks=" + chunks.size());
        } catch (DataAccessException ex) {
            throw new IllegalStateException("数据库不可用或表结构未初始化，请确认 MySQL 已启动并执行 sql/init.sql", ex);
        }
    }
}
