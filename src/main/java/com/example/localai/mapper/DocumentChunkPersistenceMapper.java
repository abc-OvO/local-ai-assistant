package com.example.localai.mapper;

import com.example.localai.model.DocumentChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentChunkPersistenceMapper {

    void deleteByDocumentId(@Param("documentId") String documentId);

    void insertBatch(@Param("chunks") List<DocumentChunk> chunks);

    List<DocumentChunk> findAll();

    List<DocumentChunk> findByDocumentId(@Param("documentId") String documentId);

    int countByDocumentId(@Param("documentId") String documentId);
}
