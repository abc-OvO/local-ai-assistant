package com.example.localai.service;

import com.example.localai.dto.DeleteDocumentResponse;
import com.example.localai.dto.DocumentDetailResponse;
import com.example.localai.dto.DocumentSummaryResponse;
import com.example.localai.dto.DocumentUploadResponse;
import com.example.localai.model.DocumentRecord;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    DocumentUploadResponse upload(MultipartFile file);

    List<DocumentSummaryResponse> listDocuments();

    DocumentDetailResponse getDocumentDetail(String documentId);

    DeleteDocumentResponse deleteDocument(String documentId);

    DocumentRecord getDocument(String documentId);

    void restorePersistedDocuments(List<DocumentRecord> documents);
}
