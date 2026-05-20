package com.example.localai.mapper;

import com.example.localai.model.DocumentRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DocumentPersistenceMapper {

    void upsert(DocumentRecord document);

    List<DocumentRecord> findAll();

    void deleteByDocumentId(String documentId);
}
