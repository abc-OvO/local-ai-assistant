package com.example.localai.service.impl;

import com.example.localai.config.RagProperties;
import com.example.localai.model.DocumentChunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RetrievalServiceImplTest {

    @Test
    void hybridRetrievalExposesVectorKeywordAndFinalScores() {
        RetrievalServiceImpl service = new RetrievalServiceImpl(new RagProperties());
        service.saveDocumentChunks("doc-1", List.of(
                chunk("chunk-1", "doc-1", "spring-guide.txt", 0, "Spring Boot MySQL RAG", List.of(1.0, 0.0)),
                chunk("chunk-2", "doc-1", "notes.txt", 1, "unrelated content", List.of(0.7, 0.3))
        ));

        List<DocumentChunk> chunks = service.retrieveGlobal("spring boot", List.of(1.0, 0.0), 2);

        DocumentChunk first = chunks.get(0);
        assertThat(first.getChunkId()).isEqualTo("chunk-1");
        assertThat(first.getVectorScore()).isGreaterThan(0.0);
        assertThat(first.getKeywordScore()).isGreaterThan(0.0);
        assertThat(first.getFinalScore()).isGreaterThan(first.getVectorScore());
        assertThat(first.getScore()).isEqualTo(first.getFinalScore());
    }

    @Test
    void mmrDoesNotReturnDuplicateChunkIds() {
        RagProperties properties = new RagProperties();
        properties.getMmr().setCandidateSize(5);
        RetrievalServiceImpl service = new RetrievalServiceImpl(properties);
        service.saveDocumentChunks("doc-1", List.of(
                chunk("same", "doc-1", "a.txt", 0, "Spring Boot", List.of(1.0, 0.0)),
                chunk("same", "doc-1", "a.txt", 1, "Spring Boot duplicate", List.of(1.0, 0.0)),
                chunk("other", "doc-1", "b.txt", 2, "MySQL RAG", List.of(0.0, 1.0))
        ));

        List<DocumentChunk> chunks = service.retrieveGlobal("spring boot mysql", List.of(1.0, 0.0), 3);

        assertThat(chunks).extracting(DocumentChunk::getChunkId)
                .doesNotHaveDuplicates()
                .containsExactlyInAnyOrder("same", "other");
    }

    @Test
    void disabledHybridAndMmrKeepsVectorOnlyRetrievalPath() {
        RagProperties properties = new RagProperties();
        properties.getHybrid().setEnabled(false);
        properties.getMmr().setEnabled(false);
        RetrievalServiceImpl service = new RetrievalServiceImpl(properties);
        service.saveDocumentChunks("doc-1", List.of(
                chunk("keyword", "doc-1", "spring.txt", 0, "Spring Boot", List.of(0.0, 1.0)),
                chunk("vector", "doc-1", "notes.txt", 1, "plain text", List.of(1.0, 0.0))
        ));

        List<DocumentChunk> chunks = service.retrieveGlobal("spring boot", List.of(1.0, 0.0), 2);

        assertThat(chunks.get(0).getChunkId()).isEqualTo("vector");
        assertThat(chunks.get(0).getKeywordScore()).isZero();
        assertThat(chunks.get(0).getFinalScore()).isEqualTo(chunks.get(0).getVectorScore());
    }

    private DocumentChunk chunk(
            String chunkId,
            String documentId,
            String fileName,
            int chunkIndex,
            String content,
            List<Double> embedding
    ) {
        return new DocumentChunk(chunkId, documentId, fileName, chunkIndex, content, embedding, null);
    }
}
